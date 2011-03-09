package sk.fiit.peweproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.xml.DOMConfigurator;

import rabbit.handler.AdaptiveHandler;
import rabbit.handler.Handler;
import rabbit.http.HttpHeader;
import rabbit.httpio.ConnectionSetupResolver;
import rabbit.httpio.request.ClientResourceHandler;
import rabbit.httpio.request.ContentChunksModifier;
import rabbit.httpio.request.ContentFetcher;
import rabbit.httpio.request.ContentSeparator;
import rabbit.httpio.request.ContentSource;
import rabbit.httpio.request.DirectContentSource;
import rabbit.httpio.request.PrefetchedContentSource;
import rabbit.io.BufferHandle;
import org.khelekore.rnio.impl.DefaultTaskIdentifier;

import rabbit.proxy.Connection;
import rabbit.proxy.HttpProxy;
import rabbit.proxy.TrafficLoggerHandler;
import rabbit.util.SProperties;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.messages.HttpMessageFactoryImpl;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpRequestImpl;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.HttpResponseImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequestImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpResponseImpl;
import sk.fiit.peweproxy.plugins.PluginHandler;
import sk.fiit.peweproxy.plugins.PluginsIntegrationManager;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.plugins.PluginHandler.PluginInstance;
import sk.fiit.peweproxy.plugins.events.EventsHandler;
import sk.fiit.peweproxy.plugins.processing.RequestChunksProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.ResponseChunksProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin.RequestProcessingActions;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin.ResponseProcessingActions;
import sk.fiit.peweproxy.services.ModulesManager;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.RequestChunkServiceHandleImpl;
import sk.fiit.peweproxy.services.ResponseChunkServiceHandleImpl;
import sk.fiit.peweproxy.services.ServicesHandleBase;
import sk.fiit.peweproxy.utils.StackTraceUtils;
import sk.fiit.peweproxy.utils.Statistics;
import sk.fiit.peweproxy.utils.Statistics.ProcessType;

public class AdaptiveEngine  {
	private static final Logger log = Logger.getLogger(AdaptiveEngine.class);
	
	private static final String ORDERING_REQUEST_TEXT = "[request]";
	private static final String ORDERING_RESPONSE_TEXT = "[response]";
	
	private final Map<Connection, ConnectionHandle> requestHandles;
	private final HttpProxy proxy;
	private final PluginHandler pluginHandler;
	private final EventsHandler eventsHandler;
	private final ModulesManager modulesManager;
	private final PluginsIntegrationManager integrationManager;
	private final List<ProcessingPluginInstance<RequestProcessingPlugin>> requestPlugins;
	private final List<ProcessingPluginInstance<RequestChunksProcessingPlugin>> requestChunksPlugins;
	private final List<ProcessingPluginInstance<ResponseProcessingPlugin>> responsePlugins;
	private final List<ProcessingPluginInstance<ResponseChunksProcessingPlugin>> responseChunksPlugins;
	private File pluginsOrderFile = null; 
	private boolean proxyDying = false;
	private final Statistics stats; 
	
	class ProcessingPluginInstance<PluginType extends ProxyPlugin> {
		private final PluginInstance plgInstance;
		private final PluginType plugin;
		
		@SuppressWarnings("unchecked")
		public ProcessingPluginInstance(PluginInstance plgInstance) {
			this.plgInstance = plgInstance;
			this.plugin = (PluginType)plgInstance.getInstance();
		}
		
		@Override
		public String toString() {
			return plgInstance.toString();
		}
	}
	
	class ConnectionHandle {
		private final Connection con;
		private ModifiableHttpRequestImpl request = null;
		private ModifiableHttpResponseImpl response = null;
		private HttpMessageFactoryImpl messageFactory;
		//private boolean requestChunking = false;
		private Handler handler = null;
		/**
		 * <code>true</code> means that LT processing will be started after caching data, <code>false</code> means that RT processing will start it
		 */
		private boolean rqLateProcessing = false;
		/**
		 * <code>true</code> means that LT processing will be started after caching data, <code>false</code> means that RT processing will start it
		 */
		private boolean rpLateProcessing = false;
		RequestProcessingActions rqResult = null;
		boolean transferRpBody = true;
		private final long requestTime;
		private long responseTime;

		public ConnectionHandle(Connection con) {
			this.con = con;
			requestTime = System.currentTimeMillis();
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
		}
	}
	
	class RequestContentListener extends ContentChunksModifier {
		final ConnectionHandle conHandle;
		final boolean chunking; 
		
		private RequestContentListener(ConnectionHandle conHandle, boolean chunking) {
			this.conHandle = conHandle;
			this.chunking = chunking;
		}
		
		@Override
		public void finishedRead(final AsyncChunkDataModifiedListener listener) {
			if (chunking && !requestChunksPlugins.isEmpty()
				&& (!conHandle.rqLateProcessing || conHandle.rqResult == RequestProcessingActions.PROCEED)) {
				proxy.getNioHandler().runThreadTask(new Runnable() {
					@Override
					public void run() {
						byte[] retVal = doRequestChunkProcessing(conHandle, null);
						finishReadTask(retVal, listener);
					}
				}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestChunkProcessingEnd", requestChunkProcessingTaskInfo(conHandle, 0)));
			} else
				finishReadTask(null, listener);
		}
		
		void finishReadTask(byte[] data, AsyncChunkDataModifiedListener listener) {
			conHandle.request.setChunkProcessed();
			requestContentCached(conHandle);
			listener.dataModified(data);
		}

		@Override
		protected void modifyData(final byte[] data, final AsyncChunkDataModifiedListener listener) {
			conHandle.request.originalMessage().addData(data);
			if (!conHandle.rqLateProcessing || conHandle.rqResult == RequestProcessingActions.PROCEED) {
				// we're caching chunks before RT processing or we are allowed to modify chunks cached for LT processing
				if (chunking && !requestChunksPlugins.isEmpty()) {
					proxy.getNioHandler().runThreadTask(new Runnable() {
						@Override
						public void run() {
							byte[] retVal = doRequestChunkProcessing(conHandle, data);
							listener.dataModified(retVal);
						}
					}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestChunkProcessing", requestChunkProcessingTaskInfo(conHandle, data.length)));
					return; // don't add data second time and don't call listener now
				}
				// add data to actual request unmodified
				conHandle.request.addData(data);
			}
			listener.dataModified(data);
		}
	}
	
	class ResponseContentListener extends ContentChunksModifier {
		final ConnectionHandle conHandle;
		final AdaptiveHandler handler;
		
		private ResponseContentListener(ConnectionHandle conHandle, AdaptiveHandler handler) {
			this.conHandle = conHandle;
			this.handler = handler;
		}
		
		@Override
		public void finishedRead(final AsyncChunkDataModifiedListener listener) {
			if (conHandle.con.getChunking() && !responseChunksPlugins.isEmpty() &&
					(!conHandle.rpLateProcessing || conHandle.transferRpBody)) {
				proxy.getNioHandler().runThreadTask(new Runnable() {
					@Override
					public void run() {
						byte[] retVal = doResponseChunkProcessing(conHandle, null);
						finishReadTask(retVal, listener);
					}
				}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseChunkProcessingEnd", responseChunkProcessingTaskInfo(conHandle, 0)));
			} else
				finishReadTask(null, listener);
		}
		
		void finishReadTask(byte[] data, AsyncChunkDataModifiedListener listener) {
			conHandle.response.setChunkProcessed();
			responseContentCached(conHandle,handler);
			if (listener != null)
				// we are finished caching for LT processing, send remaining chunk
				listener.dataModified(data);
		}

		@Override
		protected void modifyData(final byte[] data, final AsyncChunkDataModifiedListener listener) {
			conHandle.response.originalMessage().addData(data);
			if (!conHandle.rpLateProcessing || conHandle.transferRpBody) {
				if (conHandle.con.getChunking() && !responseChunksPlugins.isEmpty()) {
					proxy.getNioHandler().runThreadTask(new Runnable() {
						@Override
						public void run() {
							byte[] retVal = doResponseChunkProcessing(conHandle, data);
							listener.dataModified(retVal);
						}
					}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseChunkProcessing", responseChunkProcessingTaskInfo(conHandle, data.length)));
					return; // don't call listener now
				}
				// add data to actual response unmodified
				conHandle.response.addData(data);
			}
			listener.dataModified(data);
		}
	}
	
	public AdaptiveEngine(HttpProxy proxy) {
		requestHandles = new HashMap<Connection, ConnectionHandle>();
		this.proxy = proxy;
		eventsHandler = new EventsHandler(this);
		stats = new Statistics();
		pluginHandler = new PluginHandler(stats);
		modulesManager = new ModulesManager(this);
		integrationManager = new PluginsIntegrationManager();
		requestPlugins = new LinkedList<ProcessingPluginInstance<RequestProcessingPlugin>>();
		requestChunksPlugins = new LinkedList<ProcessingPluginInstance<RequestChunksProcessingPlugin>>();
		responsePlugins = new LinkedList<ProcessingPluginInstance<ResponseProcessingPlugin>>();
		responseChunksPlugins = new LinkedList<ProcessingPluginInstance<ResponseChunksProcessingPlugin>>();
	}
	
	public ModifiableHttpRequestImpl getRequestForConnection(Connection con) {
		if (con == null) {
			log.warn("Trying to get request for null connection\n"+StackTraceUtils.getStackTraceText());
			return null;
		}
		if (!requestHandles.containsKey(con)) {
			log.warn("No handle for "+con+", can't return request\n"+StackTraceUtils.getStackTraceText());
			return null;
		}
		return requestHandles.get(con).request;
	}
	
	public ModifiableHttpResponseImpl getResponseForConnection(Connection con) {
		if (con == null) {
			log.warn("Trying to get response for null connection\n"+StackTraceUtils.getStackTraceText());
			return null;
		}
		if (!requestHandles.containsKey(con)) {
			log.warn("No handle for "+con+", can't return response\n"+StackTraceUtils.getStackTraceText());
			return null;
		}
		return requestHandles.get(con).response;
	}
	
	public void newRequestAttempt(Connection con) {
		ConnectionHandle newHandle = new ConnectionHandle(con);
		requestHandles.put(con, newHandle);
		if (log.isTraceEnabled())
			log.trace("RQ: "+newHandle+" | Registering new handle for "+con);
	}
	Map<Connection, String> closedConns = new HashMap<Connection, String>();
	
	public void connectionClosed(Connection con) {
		//stackTraceWatcher.addStackTrace(con);
		String stackTrace = StackTraceUtils.getStackTraceText();
		if (closedConns.containsKey(con)) {
			System.out.println(closedConns.get(con));
			System.out.println(stackTrace);
		}
		closedConns.put(con, stackTrace);
		ConnectionHandle conHandle = requestHandles.remove(con);
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | Removing handle for "+con);
	}
	
	public void newRequest(Connection con, HttpHeader clientHeader, HttpHeader proxyHeader) {
		ConnectionHandle conHandle = requestHandles.get(con);
		InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		HttpRequestImpl origRequest = new HttpRequestImpl(modulesManager, new HeaderWrapper(clientHeader),clientSocketAdr);
		origRequest.setReadOnly();
		conHandle.request = new ModifiableHttpRequestImpl(modulesManager, new HeaderWrapper(proxyHeader), origRequest);
		conHandle.messageFactory = new HttpMessageFactoryImpl(this,con,conHandle.request);
		con.setProxyRHeader(conHandle.request.getHeader().getBackedHeader());
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | New request received ("
					+origRequest.getHeader().getFullLine()+") - "+conHandle.request);
	}
	
	public void cacheRequestIfNeeded(final Connection con, final ContentSeparator separator, final boolean isChunked) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		final BufferHandle bufHandle = con.getRequestBufferHandle();
		final TrafficLoggerHandler tlh = con.getTrafficLoggerHandler();
		ClientResourceHandler resourceHandler = null;
		if (separator != null) {
			// request has some content
			if (log.isTraceEnabled())
				log.trace("RQ: "+conHandle+" | Request separator used: "+separator.toString());
			boolean prefetch = false;
			Set<Class<? extends ProxyService>> desiredServices = new HashSet<Class<? extends ProxyService>>();
			for (ProcessingPluginInstance<RequestProcessingPlugin> rqPlgInstance : requestPlugins) {
				if (!integrationManager.isPluginEnabled(conHandle.request, rqPlgInstance.plgInstance.getName(), RequestProcessingPlugin.class)) {
					if (log.isTraceEnabled())
						log.trace("RQ: "+conHandle+" | Plugin "+rqPlgInstance+" skipped during services need discovery");
					continue;
				}
				try {
					final Set<Class<? extends ProxyService>> pluginsDesiredSvcs	= new HashSet<Class<? extends ProxyService>>();
					final RequestProcessingPlugin plugin = rqPlgInstance.plugin;
					stats.executeProcess(new Runnable() {
						@Override
						public void run() {
							plugin.desiredRequestServices(pluginsDesiredSvcs,conHandle.request.getHeader(),isChunked);
						}
					}, plugin, ProcessType.REQUEST_DESIRED_SERVICES, conHandle.request.originalMessage());
					if (ServicesHandleBase.contentNeeded(pluginsDesiredSvcs)) {
						if (log.isDebugEnabled())
							log.debug("RQ: "+conHandle+" | Plugin "+rqPlgInstance+" wants content modifying service for request");
						prefetch = true;
						break;
					} else
						desiredServices.addAll(pluginsDesiredSvcs);
				} catch (Throwable t) {
					log.info("RQ: Throwable raised while obtaining set of desired services from "+rqPlgInstance,t);
				}
			}
			boolean rqSendingChunked = isChunked;
			if (!prefetch)
				prefetch = conHandle.request.getServicesHandle().needContent(desiredServices,isChunked);
			if (prefetch)
				rqSendingChunked = true;
			if (!rqSendingChunked) {
				rqSendingChunked = true; // TODO ak je isChunked = false, treba zistit u chunk pluginov ci bude chciet niekto menit data (= dlzku)
			}
			RequestContentListener rqContentModifier = new RequestContentListener(conHandle,rqSendingChunked);
			if (prefetch) {
				new ContentFetcher(con,bufHandle,tlh,separator,rqContentModifier);
				// cache request body data first, then run full message real-time processing (see requestContentCached())
				return;
			} else {
				if (log.isDebugEnabled())
					log.debug("RQ: "+conHandle+" | No plugin wants content modifying service for request");
				conHandle.rqLateProcessing = true;
				ContentSource directSource = new DirectContentSource(con,bufHandle,tlh,separator,rqContentModifier);
				resourceHandler = new ClientResourceHandler(con,directSource,rqSendingChunked);
				// will run header-only real-time processing, then proceeds in handling request what will
				// transfer request body data while caching it for late processing and processing incoming chunks
				// if real-time processing ended with PROCEED result
			}
		} else {
			if (log.isTraceEnabled())
				log.trace("RQ: "+conHandle+" | Request has no body");
		}
		final ClientResourceHandler handler = resourceHandler;
		// no body or no need to modify it, run header-only real-time processing (request may have body)
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				ClientResourceHandler handlerToUse = handler;
				conHandle.rqResult = doRequestProcessing(conHandle, false); //header-only real-time processing
				boolean toSendResponse = (conHandle.rqResult == RequestProcessingActions.NEW_RESPONSE || conHandle.rqResult == RequestProcessingActions.FINAL_RESPONSE );
				if (conHandle.rqResult == RequestProcessingActions.NEW_REQUEST) {
					// substitutive request was constructed
					HttpHeader headerToBeSent = conHandle.request.getHeader().getBackedHeader();
					con.setProxyRHeader(headerToBeSent);
					byte[] requestContent = conHandle.request.getData();
					if (requestContent != null) {
						// substitutive request with body was constructed
						boolean isChunked = ConnectionSetupResolver.isChunked(headerToBeSent); // plugins can decide whether to chunk request body
						PrefetchedContentSource contentSource = new PrefetchedContentSource(requestContent);
						ClientResourceHandler newHandler = new ClientResourceHandler(conHandle.con,contentSource,isChunked);
						handlerToUse = newHandler; // transfer constructed request body instead
						
					} else
						// no body to transfer
						handlerToUse = null;
				} else if (toSendResponse) {
					// set that we need to send response after all data was read from connection with client
					handlerToUse = null;
				}
				boolean responseWillBeSent = false;
				if (handler != null && handlerToUse != handler) {
					// we are transferring other body than one being read by 'handler' so we need to cache original body for late processing
					RequestContentListener rqContentModifier = new RequestContentListener(conHandle, false);
					new ContentFetcher(con,bufHandle,tlh,separator,rqContentModifier);
					responseWillBeSent = true; // don't send response now, but after request body is read
				}
				if (toSendResponse) {
					if (!responseWillBeSent) {
						boolean processResponse = conHandle.rqResult == RequestProcessingActions.NEW_RESPONSE;
						sendResponse(conHandle, processResponse, false);
					}
				} else
					proceedWithRequest(conHandle, handlerToUse);
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestProcessing", requestProcessingTaskInfo(conHandle)));
	}
	
	private void requestContentCached(final ConnectionHandle conHandle) {
		if (log.isTraceEnabled()) {
			if (conHandle.rqLateProcessing)
				log.trace("RQ: "+conHandle+" | "+conHandle.request.originalMessage().getData().length+" bytes of request cached for late processing");
			else
				log.trace("RQ: "+conHandle+" | "+conHandle.request.getData().length+" bytes of request cached for real-time processing");
		}
		if (conHandle.rqLateProcessing) {
			// full message late processing
			doRequestLateProcessing(conHandle);
			if (conHandle.rqResult == RequestProcessingActions.NEW_RESPONSE || conHandle.rqResult == RequestProcessingActions.FINAL_RESPONSE ) {
				// now after we've read request body from the connection it's safe to send
				// response body constructed by plugins in header-only real-time processing
				sendResponse(conHandle, (conHandle.rqResult == RequestProcessingActions.NEW_RESPONSE), false);
			}
		} else {
			// full message real-time processing
			proxy.getNioHandler().runThreadTask(new Runnable() {
				@Override
				public void run() {
					conHandle.rqResult = doRequestProcessing(conHandle, false);
					if (conHandle.rqResult != RequestProcessingActions.NEW_RESPONSE && conHandle.rqResult != RequestProcessingActions.FINAL_RESPONSE) {
						// substitutive request may have been constructed
						HttpHeader headerToBeSent = conHandle.request.getHeader().getBackedHeader();
						conHandle.con.setProxyRHeader(headerToBeSent);
						ClientResourceHandler resourceHandler = null;
						byte[] requestContent = conHandle.request.getData();
						if (requestContent != null) {
							boolean isChunked = ConnectionSetupResolver.isChunked(headerToBeSent); // plugins can decide whether to chunk request body
							PrefetchedContentSource contentSource = new PrefetchedContentSource(requestContent);
							resourceHandler = new ClientResourceHandler(conHandle.con,contentSource,isChunked);
						}
						proceedWithRequest(conHandle, resourceHandler);
					} else {
						boolean processResponse = (conHandle.rqResult == RequestProcessingActions.NEW_RESPONSE) ? true : false;
						sendResponse(conHandle, processResponse, false);
					}
				}
			}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestProcessing", requestProcessingTaskInfo(conHandle)));
		}
	}
	
	private void doRequestLateProcessing(final ConnectionHandle conHandle) {
		pluginHandler.submitTaskToThreadPool(new Runnable() {
			@Override
			public void run() {
				doRequestProcessing(conHandle, true);
			}
		});
	}
	
	private byte[] doRequestChunkProcessing(final ConnectionHandle conHandle, byte[] data) {
		conHandle.request.setAllowedThread();
		Set<String> blacklistedPlugins = integrationManager.getBlackList(conHandle.request,RequestChunksProcessingPlugin.class);
		final RequestChunkServiceHandleImpl svcHandle = new RequestChunkServiceHandleImpl(conHandle.request, modulesManager, data);
		final boolean finalization = data == null;
		for (ProcessingPluginInstance<RequestChunksProcessingPlugin> rqPlgInstance : requestChunksPlugins) {
			if (blacklistedPlugins.contains(rqPlgInstance.plgInstance.getName())) {
				if (log.isTraceEnabled())
					log.trace("RQ: "+conHandle+" | Plugin "+rqPlgInstance+" skipped during chunk processing");
				continue;
			}
			try {
				final RequestChunksProcessingPlugin plugin = rqPlgInstance.plugin;
				stats.executeProcess(new Runnable() {
					@Override
					public void run() {
						if (finalization)
							plugin.finalizeProcessing(conHandle.request, svcHandle);
						else
							plugin.processRequestChunk(conHandle.request, svcHandle);
					}
				}, plugin, ProcessType.REQUEST_CHUNK_PROCESSING, conHandle.request);
			} catch (Throwable t) {
				log.info("RQ: "+conHandle+" | Throwable raised while processing request chunk by "+rqPlgInstance,t);
			}
			if (!finalization && svcHandle.getActualData() == null)
				break;
		}
		svcHandle.finalize();
		byte[] retVal = svcHandle.getData();
		conHandle.request.addData(retVal);
		return retVal;
	}
	
	private RequestProcessingActions doRequestProcessing(final ConnectionHandle conHandle, boolean lateProcessing) {
		conHandle.request.setAllowedThread();
		if (log.isTraceEnabled()) {
			if (lateProcessing)
				log.trace("RQ: "+conHandle+" | Running late request processing");
			else
				log.trace("RQ: "+conHandle+" | Running real-time request processing");
		}
		boolean transferOriginalBody = true;
		boolean again = false;
		boolean sendResponse = false;
		boolean processResponse = true;
		Set<ProcessingPluginInstance<RequestProcessingPlugin>> pluginsChangedResponse
			= new HashSet<AdaptiveEngine.ProcessingPluginInstance<RequestProcessingPlugin>>();
		Set<String> blacklistedPlugins = integrationManager.getBlackList(conHandle.request,RequestProcessingPlugin.class);
		do {
			if (again) {
				// new message provided
				byte[] data = conHandle.request.getData();
				if (data != null && data.length > 0) {
					conHandle.request.setData(null);
					doRequestChunkProcessing(conHandle, data);
					doRequestChunkProcessing(conHandle, null);
				}
			}
			again = false;
			for (ProcessingPluginInstance<RequestProcessingPlugin> rqPlgInstance : requestPlugins) {
				if (pluginsChangedResponse.contains(rqPlgInstance)) {
					if (log.isTraceEnabled())
						log.trace("RQ: "+conHandle+" | Plugin "+rqPlgInstance+" skipped since it already procesed the request and returned new one");
					continue;
				} if (blacklistedPlugins.contains(rqPlgInstance.plgInstance.getName())) {
					if (log.isTraceEnabled())
						log.trace("RQ: "+conHandle+" | Plugin "+rqPlgInstance+" skipped during processing");
					continue;
				}
				log.trace("RQ: "+conHandle+" | Processing request with "+rqPlgInstance);
				try {
					final RequestProcessingPlugin plugin = rqPlgInstance.plugin;
					if (lateProcessing) {
						stats.executeProcess(new Runnable() {
							@Override
							public void run() {
								plugin.processTransferedRequest(conHandle.request);
							}
						}, plugin, ProcessType.REQUEST_LATE_PROCESSING, conHandle.request.originalMessage());
					} else {
						RequestProcessingActions action = stats.executeProcess(new Callable<RequestProcessingActions>() {
							@Override
							public RequestProcessingActions call() throws Exception {
								return plugin.processRequest(conHandle.request);
							}
						}, plugin, ProcessType.REQUEST_PROCESSING, conHandle.request.originalMessage());
						if (action == RequestProcessingActions.NEW_REQUEST || action == RequestProcessingActions.FINAL_REQUEST) {
							HttpRequest newRequest = stats.executeProcess(new Callable<HttpRequest>() {
								@Override
								public HttpRequest call() throws Exception {
									return plugin.getNewRequest(conHandle.request, conHandle.messageFactory);
								}
							}, plugin, ProcessType.REQUEST_CONSTRUCTION, conHandle.request.originalMessage()); 
							if (newRequest == null)
								log.warn("RQ: "+conHandle+" | Null HttpRequest was provided by "+rqPlgInstance+" after calling getNewRequest()," +
											" substitution is being ignored.");
							else {
								// if FINAL_REQUEST and same request was returned just to stop processing, we DO want to transfer original body
								if (newRequest != conHandle.request) {
									transferOriginalBody = false;
									conHandle.request = (ModifiableHttpRequestImpl) newRequest;
									conHandle.messageFactory = new HttpMessageFactoryImpl(this, conHandle.con, conHandle.request);
									conHandle.request.setAllowedThread();
								}
								if (action == RequestProcessingActions.NEW_REQUEST) {
									pluginsChangedResponse.add(rqPlgInstance);
									again = true;
								}
								break;
							}
						} else if (action == RequestProcessingActions.NEW_RESPONSE || action == RequestProcessingActions.FINAL_RESPONSE) {
							if (action == RequestProcessingActions.FINAL_RESPONSE)
								processResponse = false;
							HttpResponse newResponse = stats.executeProcess(new Callable<HttpResponse>() {
								@Override
								public HttpResponse call() throws Exception {
									return plugin.getResponse(conHandle.request, conHandle.messageFactory);
								}
							}, plugin, ProcessType.REQUEST_CONSTRUCTION_REPONSE, conHandle.request.originalMessage()); 
							if (newResponse == null)
								log.warn("RQ: "+conHandle+" | Null HttpResponse was provided by "+rqPlgInstance+" after calling getNewResponse()," +
											" substitution is being ignored.");
							else {
								transferOriginalBody = false; // not needed but still leaving it here
								sendResponse = true;
								HttpResponseImpl origResponse = (HttpResponseImpl) newResponse;
								origResponse.getServicesHandleInternal().finalize();
								conHandle.response = new ModifiableHttpResponseImpl(modulesManager, origResponse.getHeader().clone(), origResponse);
								conHandle.response.setData(origResponse.getData());
								break;
							}
						}
					}
				} catch (Throwable t) {
					log.info("RQ: "+conHandle+" | Throwable raised while processing request by "+rqPlgInstance,t);
					// TODO revert changes maybe ?
				}
			}
		} while (again);
		if (!lateProcessing) {
			conHandle.request.getServicesHandleInternal().finalize();
			if (!conHandle.rqLateProcessing) {
				// we just executed real-time processing and no late processing is planned
				doRequestLateProcessing(conHandle);
			}
		}
		if (sendResponse) {
			if (processResponse)
				return RequestProcessingActions.NEW_RESPONSE;
			else
				return RequestProcessingActions.FINAL_RESPONSE;
		} else if (transferOriginalBody) {
			return RequestProcessingActions.PROCEED;
		} else {
			return RequestProcessingActions.NEW_REQUEST;
		}
	}

	private void proceedWithRequest(final ConnectionHandle conHandle, final ClientResourceHandler resourceHandler) {
		HttpHeader header = conHandle.request.getHeader().getBackedHeader(); 
		conHandle.con.setKeepalive(new ConnectionSetupResolver(header).isKeepalive()); // plugins can decide whether to keep connection with client alive
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				if (log.isTraceEnabled()) {
					log.trace("RQ: "+conHandle+" | Request processing time :"+(System.currentTimeMillis()-conHandle.requestTime));
					log.trace("RQ: "+conHandle+" | Proceeding in handling request");
				}
				conHandle.con.processRequest(resourceHandler);
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestAdvancing", requestSendingTaskInfo(conHandle)));
	}
	
	public void newResponse(Connection con, HttpHeader response) {
		ConnectionHandle conHandle = requestHandles.get(con);
		conHandle.responseTime = System.currentTimeMillis();
		HttpResponseImpl origResponse =  new HttpResponseImpl(modulesManager, new HeaderWrapper(response.clone()), conHandle.request);
		origResponse.setReadOnly();
		conHandle.response = new ModifiableHttpResponseImpl(modulesManager, new HeaderWrapper(response) ,origResponse);
		//conHandle.response.getHeader().setField("AgeORIG", conHandle.response.getWebResponseHeader().getField("Age"));
		conHandle.messageFactory.setResponse(conHandle.response);
		if (log.isTraceEnabled())
			log.trace("RP: "+conHandle+" | New response received ( "
					+origResponse.getHeader().getFullLine()+" | requested "
					+conHandle.request.getHeader().getFullLine()+") - "+conHandle.response);
	}
	
	public void newResponse(Connection con, HttpHeader header, final Runnable proceedTask) {
		if (log.isTraceEnabled())
			log.trace("RP: "+requestHandles.get(con)+" | New response constructed");
		if (requestHandles.get(con).response == null)
			newResponse(con, header);
		processResponse(con, proceedTask);
	}

	public boolean transferResponse(Connection con, HttpHeader response) {
		con.setMayCache(false);
		final ConnectionHandle conHandle = requestHandles.get(con);
		Set<Class<? extends ProxyService>> desiredServices = new HashSet<Class<? extends ProxyService>>();
		for (ProcessingPluginInstance<ResponseProcessingPlugin> rpPlgInstance : responsePlugins) {
			if (!integrationManager.isPluginEnabled(conHandle.response, rpPlgInstance.plgInstance.getName(), ResponseProcessingPlugin.class)) {
				if (log.isTraceEnabled())
					log.trace("RP: "+conHandle+" | Plugin "+rpPlgInstance+" skipped during services need discovery");
				continue;
			}
			try {
				final Set<Class<? extends ProxyService>> pluginsDesiredSvcs	= new HashSet<Class<? extends ProxyService>>();
				final ResponseProcessingPlugin plugin = rpPlgInstance.plugin;
				stats.executeProcess(new Runnable() {
					@Override
					public void run() {
						plugin.desiredResponseServices(pluginsDesiredSvcs,conHandle.response.getHeader(),conHandle.con.getChunking());
					}
				}, plugin, ProcessType.RESPONSE_DESIRED_SERVICES, conHandle.response.originalMessage());
				if (ServicesHandleBase.contentNeeded(pluginsDesiredSvcs)) {
					if (log.isDebugEnabled())
						log.debug("RP: "+conHandle+" | Plugin "+rpPlgInstance+" wants content modifying service for response");
					return true;
				} else
					desiredServices.addAll(pluginsDesiredSvcs);
			} catch (Throwable t) {
				log.info("RP: Throwable raised while obtaining set of desired services from "+rpPlgInstance,t);
			}
		}
		if (conHandle.response.getServicesHandle().needContent(desiredServices,con.getChunking()))
			return true;
		if (log.isDebugEnabled())
			log.debug("RP: "+conHandle+" | No plugin wants content modifying service for response");
		conHandle.rpLateProcessing = true;
		return false;
	}

	public void processResponse(final Connection con, final Runnable proceedTask) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		boolean adaptiveHandling = conHandle.handler instanceof AdaptiveHandler; 
		if ((!adaptiveHandling || conHandle.rpLateProcessing) && !proxyDying) {
			proxy.getNioHandler().runThreadTask(new Runnable() {
				@Override
				public void run() {
					conHandle.transferRpBody = doResponseProcessing(conHandle,false); // header-only real-time processing
					if (!conHandle.transferRpBody) {
						// substitutive response was constructed
						conHandle.handler.setDontSendBytes();
						if (conHandle.response.getData() != null) {
							// substitutive response with body was constructed
							sendResponse(conHandle, false, true);
						}
					}
					proceedWithResponse(conHandle, proceedTask, conHandle.transferRpBody);
				}
			}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseProcessing", responseProcessingTaskInfo(conHandle)));
		} else {
			proceedTask.run(); // let the AdaptiveHandler cache response body (or if proxy dying, don't bother running task through NioHandler)
			if (proxyDying && log.isTraceEnabled())
				log.trace("RP: "+conHandle+" | Response processing time :"+(System.currentTimeMillis()-conHandle.responseTime));
		}
	}
	
	private void fixResponseHeader(ConnectionHandle conHandle) {
		Connection con = conHandle.con;
		HttpHeader header = conHandle.response.getHeader().getBackedHeader();
		con.setChunking(ConnectionSetupResolver.isChunked(header));
		con.fixResponseHeader(header, conHandle.handler.changesContentSize());
	}
	
	public void responseContentCached(final ConnectionHandle conHandle, final AdaptiveHandler handler) {
		if (conHandle.rpLateProcessing) {
			if (log.isTraceEnabled())
				log.trace("RP: "+conHandle+" | "+conHandle.response.originalMessage().getData().length+" bytes of response cached for late processing");
			doResponseLateProcessing(conHandle);
		} else {
			if (log.isTraceEnabled())
				log.trace("RP: "+conHandle+" | "+conHandle.response.getData().length+" bytes of response cached for real-time processing");
			proxy.getNioHandler().runThreadTask(new Runnable() {
				@Override
				public void run() {
					doResponseProcessing(conHandle,false); // full response real-time processing
					final ModifiableHttpResponseImpl response = conHandle.response;
					final byte[] modifiedContent = conHandle.response.getData();
					proceedWithResponse(conHandle, new Runnable() {
						@Override
						public void run() {
							handler.sendResponse(response.getHeader().getBackedHeader(),modifiedContent);
						}
					}, true);
				}
			}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseProcessing", responseProcessingTaskInfo(conHandle)));
		}
	}
	
	private void doResponseLateProcessing(final ConnectionHandle conHandle) {
		pluginHandler.submitTaskToThreadPool(new Runnable() {
			@Override
			public void run() {
				doResponseProcessing(conHandle, true);
				conHandle.rpLateProcessing = false; // to avoid saving conHandle in prevRequestHandles
			}
		});
	}
	
	private byte[] doResponseChunkProcessing(final ConnectionHandle conHandle, byte[] data) {
		conHandle.response.setAllowedThread();
		Set<String> blacklistedPlugins = integrationManager.getBlackList(conHandle.request,ResponseChunksProcessingPlugin.class);
		final ResponseChunkServiceHandleImpl svcHandle = new ResponseChunkServiceHandleImpl(conHandle.response, modulesManager, data);
		final boolean finalization = data == null;
		for (ProcessingPluginInstance<ResponseChunksProcessingPlugin> rpPlgInstance : responseChunksPlugins) {
			if (blacklistedPlugins.contains(rpPlgInstance.plgInstance.getName())) {
				if (log.isTraceEnabled())
					log.trace("RP: "+conHandle+" | Plugin "+rpPlgInstance+" skipped during chunk processing");
				continue;
			}
			try {
				final ResponseChunksProcessingPlugin plugin = rpPlgInstance.plugin;
				stats.executeProcess(new Runnable() {
					@Override
					public void run() {
						if (finalization)
							plugin.finalizeProcessing(conHandle.response, svcHandle);
						else
							plugin.processResponseChunk(conHandle.response, svcHandle);
					}
				}, plugin, ProcessType.RESPONSE_CHUNK_PROCESSING, conHandle.response);
			} catch (Throwable t) {
				log.info("RP: "+conHandle+" | Throwable raised while processing response chunk by "+rpPlgInstance,t);
			}
			if (!finalization && svcHandle.getActualData() == null)
				break;
		}
		svcHandle.finalize();
		byte[] retVal = svcHandle.getData();
		conHandle.response.addData(retVal);
		return retVal;
	}
	
	private boolean doResponseProcessing(final ConnectionHandle conHandle, boolean lateProcessing) {
		conHandle.response.setAllowedThread();
		if (log.isTraceEnabled()) {
			if (lateProcessing)
				log.trace("RP: "+conHandle+" | Running late response processing");
			else
				log.trace("RP: "+conHandle+" | Running real-time response processing");
		}
		boolean transferOriginalBody = true;
		boolean again = false;
		Set<ProcessingPluginInstance<ResponseProcessingPlugin>> pluginsChangedResponse
			= new HashSet<AdaptiveEngine.ProcessingPluginInstance<ResponseProcessingPlugin>>();
		Set<String> blacklistedPlugins = integrationManager.getBlackList(conHandle.response,ResponseProcessingPlugin.class);
		do {
			if (again) {
				// new message provided
				byte[] data = conHandle.response.getData();
				if (data != null && data.length > 0) {
					conHandle.response.setData(null);
					doResponseChunkProcessing(conHandle, data);
					doResponseChunkProcessing(conHandle, null);
				}
			}
			again = false;
			for (ProcessingPluginInstance<ResponseProcessingPlugin> rpPlgInstance : responsePlugins) {
				if (pluginsChangedResponse.contains(rpPlgInstance)) {
					if (log.isTraceEnabled())
						log.trace("RP: "+conHandle+" | Plugin "+rpPlgInstance+" skipped since it already procesed the response and returned new one");
					continue;
				} if (blacklistedPlugins.contains(rpPlgInstance.plgInstance.getName())) {
					if (log.isTraceEnabled())
						log.trace("RP: "+conHandle+" | Plugin "+rpPlgInstance+" skipped during processing");
					continue;
				}
				log.trace("RP: "+conHandle+" | Processing response with plugin "+rpPlgInstance);
				try {
					final ResponseProcessingPlugin plugin = rpPlgInstance.plugin;
					if (lateProcessing) {
						stats.executeProcess(new Runnable() {
							@Override
							public void run() {
								plugin.processTransferedResponse(conHandle.response);
							}
						}, plugin, ProcessType.RESPONSE_LATE_PROCESSING, conHandle.response.originalMessage());
					} else {
						ResponseProcessingActions action = stats.executeProcess(new Callable<ResponseProcessingActions>() {
							@Override
							public ResponseProcessingActions call()
									throws Exception {
								return plugin.processResponse(conHandle.response);
							}
						}, plugin, ProcessType.RESPONSE_PROCESSING, conHandle.response.originalMessage());
						if (action == ResponseProcessingActions.NEW_RESPONSE || action == ResponseProcessingActions.FINAL_RESPONSE) {
							HttpResponse newResponse = stats.executeProcess(new Callable<HttpResponse>() {
								@Override
								public HttpResponse call() throws Exception {
									return plugin.getNewResponse(conHandle.response, conHandle.messageFactory);
								}
							}, plugin, ProcessType.RESPONSE_CONSTRUCTION, conHandle.response.originalMessage()); 
							if (newResponse == null)
								log.warn("RP: "+conHandle+" | Null HttpResponse was provided by "+rpPlgInstance+" after calling getNewResponse()," +
											" substitution is being ignored.");
							else {
								// if FINAL_RESPONSE and same response was returned just to stop processing, we DO want to transfer original body
								if (newResponse != conHandle.response) {
									transferOriginalBody = false;
									conHandle.response = (ModifiableHttpResponseImpl) newResponse;
									conHandle.response.setAllowedThread();
								}
								if (action == ResponseProcessingActions.NEW_RESPONSE) {
									pluginsChangedResponse.add(rpPlgInstance);
									again = true;
								}
								break;
							}
						}
					}
				} catch (Throwable t) {
					log.info("RP: "+conHandle+" | Throwable raised while processing response by "+rpPlgInstance,t);
					// TODO revert changes maybe ?
				}
			}
		} while (again);
		if (!lateProcessing) {
			conHandle.response.getServicesHandleInternal().finalize();
			fixResponseHeader(conHandle);
			if (!conHandle.rpLateProcessing) {
				// we just executed real-time processing and no late processing is planned
				doResponseLateProcessing(conHandle);
			}
		}
		return transferOriginalBody;
	}
	
	private void proceedWithResponse(final ConnectionHandle conHandle, final Runnable proceedTask, final boolean logProceed) {
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				if (log.isTraceEnabled() && logProceed) {
					log.trace("RP: "+conHandle+" | Response processing time :"+(System.currentTimeMillis()-conHandle.responseTime));
					log.trace("RQ: "+conHandle+" | Proceeding in handling response");
				}
				proceedTask.run();
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseAdvancing", responseSendingTaskInfo(conHandle)));
	}

	
	private void sendResponse(final ConnectionHandle conHandle, final boolean processResponse, final boolean processingDone) {
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				AdaptiveHandler handlerFactory = (AdaptiveHandler)conHandle.con.getProxy().getNamedHandlerFactory(AdaptiveHandler.class.getName());
				conHandle.handler = handlerFactory;
				if (processResponse) {
					doResponseProcessing(conHandle,false);
				} else if (!processingDone)
					conHandle.response.getServicesHandleInternal().finalize();
					doResponseLateProcessing(conHandle);
				if (log.isTraceEnabled())
					log.trace("RQ: "+conHandle+" | Sending response");
				Connection con = conHandle.con;
				TrafficLoggerHandler tlh = con.getTrafficLoggerHandler();
				HttpHeader requestHeader = conHandle.request.getHeader().getBackedHeader();
				final HttpHeader responseHeader = conHandle.response.getHeader().getBackedHeader();
				//conHandle.response.getProxyResponseHeaders().setHeader("Transfer-Encoding","chunked");
				handlerFactory.nextInstanceWillNotCache();
				final AdaptiveHandler sendingHandler = (AdaptiveHandler)handlerFactory.getNewInstance(conHandle.con, tlh, requestHeader, responseHeader, null, con.getMayCache(), con.getMayFilter(), -1);
				conHandle.handler = sendingHandler;
				conHandle.con.fixResponseHeader(responseHeader, sendingHandler.changesContentSize());
				proceedWithResponse(conHandle, new Runnable() {
					@Override
					public void run() {
						sendingHandler.sendResponse(responseHeader, conHandle.response.getData());
					}
				},true);
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseProcessing", responseProcessingTaskInfo(conHandle)));
	}
	
	private String requestProcessingTaskInfo(ConnectionHandle conHandle) {
		return "Running plugins processing on request \""
			+ conHandle.request.originalMessage().getHeader().getBackedHeader().getRequestLine()
			+ "\" from " + conHandle.request.clientSocketAddress();
	}
	
	private String requestChunkProcessingTaskInfo(ConnectionHandle conHandle, int chunkLenght) {
		return "Running "+((chunkLenght == 0) ? "finalization by " : "")+"chunk plugins processing on " + chunkLenght
			+ " bytes long chunk of request \""	+ conHandle.request.originalMessage().getHeader().getBackedHeader().getRequestLine()
			+ "\" from " + conHandle.request.clientSocketAddress();
	}

	private String responseProcessingTaskInfo (ConnectionHandle conHandle) {
		return "Running plugins processing on response \""
			+ conHandle.response.originalMessage().getHeader().getBackedHeader().getStatusLine()
			+ "\" for request \"" + conHandle.request.getHeader().getBackedHeader().getRequestLine()
			+ "\" from " + conHandle.request.clientSocketAddress();
	}
	
	private String responseChunkProcessingTaskInfo (ConnectionHandle conHandle, int chunkLenght) {
		return "Running "+((chunkLenght == 0) ? "finalization by " : "")+"chunk plugins processing on " + chunkLenght
			+ " bytes long chunk of response \"" + conHandle.response.originalMessage().getHeader().getBackedHeader().getStatusLine()
			+ "\" for request \"" + conHandle.request.getHeader().getBackedHeader().getRequestLine()
			+ "\" from " + conHandle.request.clientSocketAddress();
	}

	private String requestSendingTaskInfo(ConnectionHandle conHandle) {
		return "Proceeding in handling request \""
			+ conHandle.request.getHeader().getBackedHeader().getRequestLine()
			+ "\" (orig: \""+ conHandle.request.originalMessage().getHeader().getBackedHeader().getRequestLine()
			+ "\") from " + conHandle.request.clientSocketAddress();
	}

	private String responseSendingTaskInfo (ConnectionHandle conHandle) {
		return "Proceeding in handling response \""
			+ conHandle.response.getHeader().getBackedHeader().getStatusLine()
			+ "\" (orig: \""+ conHandle.response.originalMessage().getHeader().getBackedHeader().getStatusLine()
			+ "\") for request \"" + conHandle.request.getHeader().getBackedHeader().getRequestLine()
			+ "\" from " + conHandle.request.clientSocketAddress();
	}

	public HttpProxy getProxy() {
		return proxy;
	}

	public PluginHandler getPluginHandler() {
		return pluginHandler;
	}

	public EventsHandler getEventsHandler() {
		return eventsHandler;
	}

	public void setup(SProperties prop) {
		if (prop == null)
			prop = new SProperties();
		File confDir = proxy.getConfigFile().getParentFile();
		String log4jConfFile = prop.getProperty("logging_conf");
		File loggingConfFile = null;
		if (log4jConfFile != null) {
			loggingConfFile = new File(confDir,log4jConfFile); 
			if (!loggingConfFile.canRead())
				log4jConfFile = null;
		}
		if (loggingConfFile != null) {
			try {
				DOMConfigurator.configure(loggingConfFile.toURI().toURL());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			String loggingLvL = prop.getProperty("logging_level");
			if (loggingLvL == null)
				loggingLvL = System.getProperty("sk.fiit.adaptiveproxy.logging_level", "INFO").trim();
			Level lvl = Level.toLevel(loggingLvL);
			Logger root = Logger.getRootLogger();
			root.setLevel(lvl);
			Logger.getLogger("org.apache").setLevel(Level.WARN);
			log.setLevel(lvl);
		    root.addAppender(new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%22t] %-5p %-27c{1} %x - %m%n")));
			//BasicConfigurator.configure();
			log.info("No Log4j configuration file specified, using default configuration");
		}
		String pluginsHomeProp = prop.getProperty("plugins_home");
		boolean defaultPlgDir = pluginsHomeProp != null;
		if (pluginsHomeProp == null)
			pluginsHomeProp = "../plugins"; 
		File pluginsHomeDir = new File(confDir,pluginsHomeProp);
		String pluginsOrderFileName = prop.getProperty("plugins_order_file","plugins_ordering");
		pluginsOrderFile = new File(pluginsHomeDir,pluginsOrderFileName);
		if (!pluginsHomeDir.isDirectory() || !pluginsHomeDir.canRead()) {
			log.info("Unable to find or access "+((!defaultPlgDir)? "default ":"")+"plugins directory "+pluginsHomeDir.getAbsolutePath());
		} else {
			File svcsDir = new File(pluginsHomeDir,prop.getProperty("services_folder","services"));
			File sharedLibDir = null;
			String sharedLibDirName = prop.getProperty("shared_libs_folder","shared_libs");
			if (!sharedLibDirName.trim().isEmpty())
				sharedLibDir = new File(pluginsHomeDir,sharedLibDirName);
			Set<String> excludeFiles = new HashSet<String>();
			excludeFiles.add(pluginsOrderFileName);
			String coreThreadsNumber = prop.getProperty("thread_pool_core_threads");
			int coreThreads = -1;
			if (coreThreadsNumber == null)
				log.info("No setting for number of core threads of plugins thread pool, default value will be used");
			else {
				try {
					coreThreads = Integer.parseInt(coreThreadsNumber);
				} catch (Exception e) {
					log.info("Invalid number of core threads of plugins thread pool, default value will be used");
				}
			}
			pluginHandler.setup(pluginsHomeDir,svcsDir,sharedLibDir,excludeFiles,coreThreads);
		}
		boolean pluginsToggling = Boolean.valueOf(prop.getProperty("enable_plugins_toggling","true"));
		log.info("Plugins toggling is "+((pluginsToggling) ? "enabled" : "disabled"));
		integrationManager.setToggling(pluginsToggling);
		modulesManager.setPattern(prop.getProperty("string_services_pattern"));
		reloadPlugins();
		log.info("AdaptiveProxy set up and ready for action");
	}
	
	public void reloadPlugins() {
		log.info("Reloading plugins");
		pluginHandler.reloadPlugins();
		eventsHandler.setup();
		modulesManager.initPlugins(pluginHandler);
		requestPlugins.clear();
		requestChunksPlugins.clear();
		responsePlugins.clear();
		responseChunksPlugins.clear();
		log.info("Loading request processing plugins");
		List<ProcessingPluginInstance<RequestProcessingPlugin>> requestPluginsList = getProcessingPluginInstances(RequestProcessingPlugin.class);
		log.info("Loading request chunks processing plugins");
		List<ProcessingPluginInstance<RequestChunksProcessingPlugin>> requestChunksPluginsList = getProcessingPluginInstances(RequestChunksProcessingPlugin.class);
		log.info("Loading response processing plugins");
		List<ProcessingPluginInstance<ResponseProcessingPlugin>> responsePluginsList = getProcessingPluginInstances(ResponseProcessingPlugin.class);
		log.info("Loading response chunks processing plugins");
		List<ProcessingPluginInstance<ResponseChunksProcessingPlugin>> responseChunksPluginsList = getProcessingPluginInstances(ResponseChunksProcessingPlugin.class);
		
		boolean pluginsOrderingSuccess;
		if (pluginsOrderFile != null && pluginsOrderFile.canRead()) {
			pluginsOrderingSuccess = addProcessingPluginsInOrder(pluginsOrderFile,requestPluginsList,requestChunksPluginsList,
																	responsePluginsList,responseChunksPluginsList);
		} else {
			log.info("Unable to find or read plugins ordering file "+pluginsOrderFile.getAbsolutePath());
			pluginsOrderingSuccess = false;
		}
		if (!pluginsOrderingSuccess) {
			log.info("Loading of configuration of plugin ordering failed for some reason. "+
					"Processing plugins will not be called in some desired fashion");
		}
		
		requestPlugins.addAll(requestPluginsList);
		requestChunksPlugins.addAll(requestChunksPluginsList);
		responsePlugins.addAll(responsePluginsList);
		responseChunksPlugins.addAll(responseChunksPluginsList);
		
		stats.pluginsRealoaded(pluginHandler);
		integrationManager.pluginsReloaded();
	}
	
	private <T extends ProxyPlugin> List<ProcessingPluginInstance<T>> getProcessingPluginInstances(Class<T> pluginClass) {
		List<PluginInstance> plgInstaces = pluginHandler.getAllPlugins();
		List<T> plugins = pluginHandler.getPlugins(pluginClass);
		List<ProcessingPluginInstance<T>> retVal = new LinkedList<ProcessingPluginInstance<T>>();
		for (PluginInstance pluginInstance : plgInstaces) {
			if (plugins.contains(pluginInstance.getInstance()))
				retVal.add(new ProcessingPluginInstance<T>(pluginInstance));
		}
		return retVal;
	}
	
	@SuppressWarnings("unchecked")
	private boolean addProcessingPluginsInOrder(File pluginsOrderFile, List<ProcessingPluginInstance<RequestProcessingPlugin>> requestPluginsList,
			List<ProcessingPluginInstance<RequestChunksProcessingPlugin>> requestChunksPluginsList,
			List<ProcessingPluginInstance<ResponseProcessingPlugin>> responsePluginsList,
			List<ProcessingPluginInstance<ResponseChunksProcessingPlugin>> responseChunksPluginsList) {
		Scanner sc = null;
		try {
			sc = new Scanner(new FileInputStream(pluginsOrderFile), "UTF-8");
		} catch (FileNotFoundException e) {
			log.info("Unable to locate plugins ordering file at "+pluginsOrderFile.getPath());
		}
		if (sc == null)
			return false;
		
		boolean wasRequestMark = false;
		boolean wasResponseMark = false;
		Map<String, ProcessingPluginInstance<?>> rqPlgInstances = new HashMap<String, AdaptiveEngine.ProcessingPluginInstance<?>>();
		Map<String, ProcessingPluginInstance<?>> rpPlgInstances = new HashMap<String, AdaptiveEngine.ProcessingPluginInstance<?>>();
		for (ProcessingPluginInstance<RequestProcessingPlugin> rqPluginInstance : requestPluginsList) {
			rqPlgInstances.put(rqPluginInstance.plgInstance.getName(), rqPluginInstance);
		}
		for (ProcessingPluginInstance<RequestChunksProcessingPlugin> rqPluginInstance : requestChunksPluginsList) {
			rqPlgInstances.put(rqPluginInstance.plgInstance.getName(), rqPluginInstance);
		}
		for (ProcessingPluginInstance<ResponseProcessingPlugin> rpPluginInstance : responsePluginsList) {
			rpPlgInstances.put(rpPluginInstance.plgInstance.getName(), rpPluginInstance);
		}
		for (ProcessingPluginInstance<ResponseChunksProcessingPlugin> rpPluginInstance : responseChunksPluginsList) {
			rpPlgInstances.put(rpPluginInstance.plgInstance.getName(), rpPluginInstance);
		}
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if (line.startsWith("#") || line.isEmpty())
				continue;
			if (!wasRequestMark && ORDERING_REQUEST_TEXT.equals(line)) {
				wasRequestMark = true;
				continue;
			}
			if (!wasResponseMark && ORDERING_RESPONSE_TEXT.equals(line)) {
				wasResponseMark = true;
				continue;
			}
			if (wasRequestMark) {
				boolean sucess = false;
				ProcessingPluginInstance<?> plgInstance = (wasResponseMark) ? rpPlgInstances.get(line) : rqPlgInstances.get(line);
				if (plgInstance != null) {
					if (wasResponseMark) {
						sucess = addPlugin((ProcessingPluginInstance<ResponseProcessingPlugin>)plgInstance, responsePlugins, responsePluginsList)
						 || addPlugin((ProcessingPluginInstance<ResponseChunksProcessingPlugin>)plgInstance, responseChunksPlugins, responseChunksPluginsList);
					} else {
						sucess = addPlugin((ProcessingPluginInstance<RequestProcessingPlugin>)plgInstance, requestPlugins, requestPluginsList)
						 || addPlugin((ProcessingPluginInstance<RequestChunksProcessingPlugin>)plgInstance, requestChunksPlugins, requestChunksPluginsList);
					}
				}
				if (!sucess)
					log.info("Can't insert plugin with name '"+line+"' into "+((wasResponseMark)?"response":"resuest")+" processing order," +
							" because such plugin is not present");
			}
		}
		return true;
	}
	
	private <T extends ProxyPlugin> boolean addPlugin(ProcessingPluginInstance<T> pluginInstance, List<ProcessingPluginInstance<T>> targetPluginsList,
			List<ProcessingPluginInstance<T>> sourcePluginsList) {
		if (pluginInstance == null)
			return false;
		targetPluginsList.add(pluginInstance);
		sourcePluginsList.remove(pluginInstance);
		return true;
	}
	
	public void setProxyIsDying() {
		proxyDying = true;
	}
	
	public boolean isProxyDying(){
		return proxyDying;
	}

	public void responseHandlerUsed(Connection connection, Handler handler) {
		ConnectionHandle conHandle = requestHandles.get(connection);
		conHandle.handler = handler;
		if (handler instanceof AdaptiveHandler) {
			AdaptiveHandler aHandler = ((AdaptiveHandler) handler); 
			ResponseContentListener rpModifier = new ResponseContentListener(conHandle, aHandler);
			aHandler.setChunksListener(rpModifier);
		}
		if (log.isTraceEnabled())
			log.trace("RP: "+conHandle+" | Handler "+handler.toString()+" used for response "+conHandle.response
					+" on " +conHandle.request.getHeader().getBackedHeader().getRequestLine());
	}

	public ModulesManager getModulesManager() {
		return modulesManager;
	}

	public PluginsIntegrationManager getIntegrationManager() {
		return integrationManager;
	}
	
	public Statistics getStatistics() {
		return stats;
	}
}
