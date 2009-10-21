package sk.fiit.rabbit.adaptiveproxy;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rabbit.handler.AdaptiveHandler;
import rabbit.http.HttpHeader;
import rabbit.httpio.request.ClientResourceHandler;
import rabbit.httpio.request.ContentFetcher;
import rabbit.httpio.request.ContentSeparator;
import rabbit.httpio.request.ContentSource;
import rabbit.httpio.request.DirectContentSource;
import rabbit.httpio.request.PrefetchedContentSource;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.HttpProxy;
import rabbit.proxy.TrafficLoggerHandler;
import rabbit.util.SProperties;
import rabbit.util.SimpleUserHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.events.EventsHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactoryImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequestImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponseImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.RequestProcessingPlugin.RequestProcessingActions;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.ResponseProcessingPlugin.ResponseProcessingActions;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.utils.XMLFileParser;

public class AdaptiveEngine  {
	private static final Logger log = Logger.getLogger(AdaptiveEngine.class);
	/*private static AdaptiveEngine instance;
	
	public static void setup(HttpProxy proxy) {
		instance = new AdaptiveEngine(proxy);
	}
	
	public static AdaptiveEngine getSingleton() {
		if (instance == null)
			throw new IllegalStateException("AdaptiveEngine singleton instance not initialized by setup() method");
		return instance;
	}*/
	
	private final Map<Connection, ConnectionHandle> requestHandles;
	private final HttpProxy proxy;
	private PluginHandler pluginHandler;
	private final EventsHandler loggingHandler;
	private final SimpleUserHandler userHandler;
	private final List<RequestProcessingPlugin> requestPlugins;
	private final List<ResponseProcessingPlugin> responsePlugins;
	private File pluginsOrderFile = null; 
	private boolean proxyDying = false;
	
	class ConnectionHandle {
		private final Connection con;
		private ModifiableHttpRequestImpl request = null;
		private ModifiableHttpResponseImpl response = null;
		private HttpMessageFactoryImpl messageFactory;
		private boolean requestChunking = false;
		private boolean adaptiveHandling = false;
		private final long requestTime;

		public ConnectionHandle(Connection con) {
			this.con = con;
			requestTime = System.currentTimeMillis();
		}
	}
	
	public AdaptiveEngine(HttpProxy proxy) {
		requestHandles = new HashMap<Connection, ConnectionHandle>();
		this.proxy = proxy;
		loggingHandler = new EventsHandler(this);
		userHandler = new SimpleUserHandler();
		userHandler.setFile("./conf/users", new Log4jLogger(log));
		pluginHandler = new PluginHandler();
		requestPlugins = new LinkedList<RequestProcessingPlugin>();
		responsePlugins = new LinkedList<ResponseProcessingPlugin>();
	}
	
	public HttpProxy getProxy() {
		return proxy;
	}
	
	public ModifiableHttpRequest getRequestForConnection(Connection con) {
		if (con == null) {
			log.debug("Trying to get request for null connection");
			return null;
		}
		if (!requestHandles.containsKey(con)) {
			log.debug("No handle for connection, can't return request");
			return null;
		}
		return requestHandles.get(con).request;
	}
	
	public ModifiableHttpResponse getResponseForConnection(Connection con) {
		if (con == null) {
			log.debug("Trying to get response for null connection");
			return null;
		}
		if (!requestHandles.containsKey(con)) {
			log.debug("No handle for connection, can't return response");
			return null;
		}
		return requestHandles.get(con).response;
	}
	
	public void newRequestAttempt(Connection con) {
		if (log.isTraceEnabled())
			log.trace("Registering new ConnectionHandle for connection "+con);
		requestHandles.put(con, new ConnectionHandle(con));
	}
	
	public void connectionClosed(Connection con) {
		if (log.isTraceEnabled())
			log.trace("Removing ConnectionHandle for connection "+con);
		requestHandles.remove(con);
	}
	
	public void newRequest(Connection con, boolean chunking) {
		ConnectionHandle conHandle = requestHandles.get(con);
		InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		conHandle.request = new ModifiableHttpRequestImpl(new HeaderWrapper(con.getClientRHeader()),clientSocketAdr);
		conHandle.messageFactory = new HttpMessageFactoryImpl(con,conHandle.request);
		conHandle.requestChunking = chunking;
		con.setProxyRHeader(conHandle.request.getProxyRequestHeaders().getBackedHeader());
	}
	
	public void cacheRequestIfNeeded(final Connection con, ContentSeparator separator, Long dataSize) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		BufferHandle bufHandle = con.getRequestBufferHandle();
		ClientResourceHandler resourceHandler = null;
		if (separator != null) {
			// request has some content
			TrafficLoggerHandler tlh = con.getTrafficLoggerHandler();
			boolean prefetch = conHandle.request.getServiceHandle().wantContent();
			if (!prefetch) {
				for (RequestProcessingPlugin requestPlugin : requestPlugins) {
					if (requestPlugin.wantRequestContent(conHandle.request.getClientRequestHeaders())) {
						prefetch = true;
						break;
					}
				}
			}
			if (prefetch) {
				RequestContentCahcedListener contentCachedListener = new RequestContentCahcedListener(con);
				new ContentFetcher(con,bufHandle,tlh,separator,contentCachedListener,dataSize);
				return;
			} else {
				ContentSource directSource = new DirectContentSource(con,bufHandle,tlh,separator);
				resourceHandler = new ClientResourceHandler(con,directSource,conHandle.requestChunking);
			}
		}
		final ClientResourceHandler handler = resourceHandler;
		proxy.getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				if (runRequestAdapters(conHandle))
					processWithRequest(con, handler);
			}
		});
	}
	
	void processWithRequest(final Connection con, final ClientResourceHandler resourceHandler) {
		proxy.getTaskRunner().runMainTask(new Runnable() {
			@Override
			public void run() {
				con.processRequest(resourceHandler);
			}
		});
	}
	
	public void requestContentCached(final Connection con, final byte[] requestContent, final Queue<Integer> dataIncrements) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		proxy.getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				conHandle.request.setData(requestContent);
				if (runRequestAdapters(conHandle)) {
					ClientResourceHandler resourceHandler = null;
					byte[] requestContent = conHandle.request.getData();
					if (requestContent != null) {
						PrefetchedContentSource contentSource = new PrefetchedContentSource(requestContent,dataIncrements);
						resourceHandler = new ClientResourceHandler(conHandle.con,contentSource,conHandle.requestChunking);
					}
					processWithRequest(conHandle.con, resourceHandler);
				}
			}
		});
	}
	
	private void sendResponse(final ConnectionHandle conHandle, boolean processResponse) {
		if (processResponse) {
			runResponseAdapters(conHandle);
		} else {
			conHandle.response.getServiceHandle().doChanges();
		}
		AdaptiveHandler handlerFactory = (AdaptiveHandler)conHandle.con.getProxy().getNamedHandlerFactory(AdaptiveHandler.class.getName());
		BufferHandle bufHandle = new SimpleBufferHandle(ByteBuffer.wrap(new byte[4096]));
		Connection con = conHandle.con;
		TrafficLoggerHandler tlh = con.getTrafficLoggerHandler();
		HttpHeader requestHeaders = conHandle.request.getProxyRequestHeaders().getBackedHeader();
		final HttpHeader responseHeaders = conHandle.response.getProxyResponseHeaders().getBackedHeader();
		//conHandle.response.getProxyResponseHeaders().setHeader("Transfer-Encoding","chunked");
		handlerFactory.nextInstanceWillNotCache();
		final AdaptiveHandler sendingHandler = (AdaptiveHandler)handlerFactory.getNewInstance(conHandle.con, tlh, requestHeaders, bufHandle, responseHeaders, null, con.getMayCache(), con.getMayFilter(), -1);
		proxy.getTaskRunner().runMainTask(new Runnable() {
			@Override
			public void run() {
				sendingHandler.sendResponse(responseHeaders, conHandle.response.getData());
			}
		});
	}
	
	private boolean runRequestAdapters(ConnectionHandle conHandle) {
		RequestServiceHandleImpl serviceHandle = conHandle.request.getServiceHandle();
		serviceHandle.doServiceDiscovery();
		boolean again = false;
		Set<RequestProcessingPlugin> pluginsChangedResponse = new HashSet<RequestProcessingPlugin>();
		do {
			again = false;
			for (RequestProcessingPlugin requestPlugin : requestPlugins) {
				if (pluginsChangedResponse.contains(requestPlugin))
					// skip this plugin to prevent cycling
					continue;
				boolean sendResponse = false;
				boolean processResponse = true;
				try {
					RequestProcessingActions action = requestPlugin.processRequest(conHandle.request);
					if (action == RequestProcessingActions.NEW_REQUEST || action == RequestProcessingActions.FINAL_REQUEST) {
						conHandle.request = (ModifiableHttpRequestImpl)requestPlugin.getNewRequest(conHandle.request, conHandle.messageFactory);
						if (action == RequestProcessingActions.NEW_REQUEST) {
							pluginsChangedResponse.add(requestPlugin);
							again = true;
						}
						break;
					} else if (action == RequestProcessingActions.NEW_RESPONSE || action == RequestProcessingActions.FINAL_RESPONSE) {
						if (action == RequestProcessingActions.FINAL_RESPONSE)
							processResponse = false;
						conHandle.response = (ModifiableHttpResponseImpl)requestPlugin.getResponse(conHandle.request, conHandle.messageFactory);
						sendResponse = true;
					}
				} catch (Exception e) {
					log.debug("Exception thrown while processing request with RequestProcessingPlugin '"+requestPlugin+"'",e);
				}
				if (sendResponse) {
					sendResponse(conHandle,processResponse);
					return false;
				}
			}
		} while (again);
		conHandle.request.getServiceHandle().doChanges();
		return true;
	}
	
	public void newResponse(Connection con, HttpHeader response) {
		ConnectionHandle conHandle = requestHandles.get(con);
		conHandle.response = new ModifiableHttpResponseImpl(new HeaderWrapper(response),conHandle.request);
	}
	
	public void processResponse(final Connection con, final Runnable proceedTask) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		if (!conHandle.adaptiveHandling && !proxyDying) {
			proxy.getTaskRunner().runThreadTask(new Runnable() {
				@Override
				public void run() {
					runResponseAdapters(conHandle);	
					proxy.getTaskRunner().runMainTask(new Runnable() {
						@Override
						public void run() {
							log.trace("Request handling time :"+(System.currentTimeMillis()-conHandle.requestTime));
							proceedTask.run();
						}
					});
				}
			});
		} else {
			proceedTask.run();
		}
	}
	
	public boolean cacheResponse(Connection con, HttpHeader response) {
		con.setMayCache(false);
		ConnectionHandle conHandle = requestHandles.get(con);
		// conHandle.response.proxyRPHeaders were modified meanwhile
		conHandle.response = new ModifiableHttpResponseImpl(new HeaderWrapper(response),conHandle.request);
		conHandle.adaptiveHandling = true;
		if (conHandle.response.getServiceHandle().wantContent())
			return true;
		for (ResponseProcessingPlugin responsePlugin : responsePlugins) {
			if (responsePlugin.wantResponseContent(conHandle.response.getWebResponseHeaders()))
				return true;
		}
		return false;
	}
	
	public void responseContentCached(Connection con, final byte[] responseContent, final AdaptiveHandler handler) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		if (proxy.getTaskRunner().isRunningInMainThread()) {
			proxy.getTaskRunner().runThreadTask(new Runnable() {
				@Override
				public void run() {
					processCachedResponse(conHandle, responseContent, handler);
				}
			});
		} else
			processCachedResponse(conHandle, responseContent, handler);
	}
	
	private void processCachedResponse(final ConnectionHandle conHandle, byte[] responseContent, final AdaptiveHandler handler) {
		conHandle.response.setData(responseContent);
		runResponseAdapters(conHandle);
		final ModifiableHttpResponseImpl response = conHandle.response;
		final byte[] modifiedContent = conHandle.response.getData();
		proxy.getTaskRunner().runMainTask(new Runnable() {
			@Override
			public void run() {
				log.trace("Request handling time :"+(System.currentTimeMillis()-conHandle.requestTime));
				handler.sendResponse(response.getProxyResponseHeaders().getBackedHeader(),modifiedContent);
			}
		});
	}
	
	private void runResponseAdapters(ConnectionHandle conHandle) {
		ResponseServiceHandleImpl serviceHandle = conHandle.response.getServiceHandle();
		serviceHandle.doServiceDiscovery();
		boolean again = false;
		Set<ResponseProcessingPlugin> pluginsChangedResponse = new HashSet<ResponseProcessingPlugin>();
		do {
			again = false;
			for (ResponseProcessingPlugin responsePlugin : responsePlugins) {
				if (pluginsChangedResponse.contains(responsePlugin))
					// skip this plugin to prevent cycling
					continue;
				try {
					ResponseProcessingActions action = responsePlugin.processResponse(conHandle.response);
					if (action == ResponseProcessingActions.NEW_RESPONSE || action == ResponseProcessingActions.FINAL_RESPONSE) {
						conHandle.response = (ModifiableHttpResponseImpl)responsePlugin.getNewResponse(conHandle.response, conHandle.messageFactory);
						if (action == ResponseProcessingActions.NEW_RESPONSE) {
							pluginsChangedResponse.add(responsePlugin);
							again = true;
						}
						break;
					}
				} catch (Exception e) {
					log.debug("Exception thrown while processing response with ResponseProcessingPlugin '"+responsePlugin+"'",e);
				}
			}
		} while (again);
		conHandle.response.getServiceHandle().doChanges();
	}
	
	public void newProxyResponse(Connection con, HttpHeader header, final Runnable proceedTask) {
		newResponse(con, header);
		processResponse(con, proceedTask);
	}
	
	public PluginHandler getPluginHandler() {
		return pluginHandler;
	}
	
	public EventsHandler getEventsHandler() {
		return loggingHandler;
	}
	
	public void setup(SProperties prop) {
		String pluginsHomeProp = prop.getProperty("pluginsHome","plugins");
		File pluginsHomeDir = new File(new File(System.getProperty("user.dir")),pluginsHomeProp);
		if (!pluginsHomeDir.isDirectory() || !pluginsHomeDir.canRead()) {
			log.warn("pluginsHome property does not point to readable directory");
		} else {
			String pluginsOrderFileName = prop.getProperty("pluginsOrderFile","plugins_ordering.xml");
			pluginsOrderFile = new File(pluginsHomeDir,pluginsOrderFileName);
			Set<String> excludeFiles = new HashSet<String>();
			excludeFiles.add(pluginsOrderFileName);
			pluginHandler.setPluginRepository(pluginsHomeDir,excludeFiles);
		}
		reloadPlugins();
	}
	
	public void reloadPlugins() {
		pluginHandler.reloadPlugins();
		loggingHandler.setup();
		RequestServiceHandleImpl.initPlugins(pluginHandler);
		ResponseServiceHandleImpl.initPlugins(pluginHandler);
		requestPlugins.clear();
		responsePlugins.clear();
		Set<RequestProcessingPlugin> requestPluginsSet = pluginHandler.getPlugins(RequestProcessingPlugin.class);
		Set<ResponseProcessingPlugin> responsePluginsSet = pluginHandler.getPlugins(ResponseProcessingPlugin.class);
		boolean pluginsOrderingSuccess;
		if (pluginsOrderFile != null && pluginsOrderFile.canRead()) {
			pluginsOrderingSuccess = loadProcessingPlugins(pluginsOrderFile,requestPluginsSet,responsePluginsSet);
		} else {
			log.warn("Unable to find or read plugins ordering file "+pluginsOrderFile.getAbsolutePath());
			pluginsOrderingSuccess = false;
		}
		if (!pluginsOrderingSuccess) {
			log.warn("Loading of configuration of plugin ordering failed for some reason. "+
					"Processing plugins will not be called in some desired fashion");
		}
		requestPlugins.addAll(requestPluginsSet);
		responsePlugins.addAll(responsePluginsSet);
	}
	
	private boolean loadProcessingPlugins(File pluginsOrderFile, Set<RequestProcessingPlugin> requestPluginsSet,
			Set<ResponseProcessingPlugin> responsePluginsSet) {
		Document pluginsOrderDoc = XMLFileParser.parseFile(pluginsOrderFile);
		if (pluginsOrderDoc != null) {
			Element docRoot = pluginsOrderDoc.getDocumentElement();
			Element requestPluginsElement = (Element)docRoot.getElementsByTagName("request").item(0);
			loadPluginsOrder(requestPluginsElement, this.requestPlugins, requestPluginsSet, RequestProcessingPlugin.class);
			Element responsePluginsElement = (Element)docRoot.getElementsByTagName("response").item(0);
			loadPluginsOrder(responsePluginsElement, this.responsePlugins, responsePluginsSet, ResponseProcessingPlugin.class);
			return true;
			
		} else {
			log.warn("Corrupted plugins ordering file "+pluginsOrderFile.getAbsolutePath());
			return false;
		}
	}
	
	private <T extends ProxyPlugin> void loadPluginsOrder(Element pluginsElement, List<T> pluginsList,
			Set<T> pluginsSet, Class<T> pluginsClass) {
		NodeList plugins = pluginsElement.getElementsByTagName("plugin");
		for (int i = 0; i < plugins.getLength(); i++) {
			Element pluginElement = (Element)plugins.item(i);
			String pluginName = pluginElement.getTextContent();
			T plugin = pluginHandler.getPlugin(pluginName, pluginsClass);
			if (plugin != null) {
				pluginsList.add(plugin);
				pluginsSet.remove(plugin);
			}
		}
	}
	
	public List<RequestProcessingPlugin> getLoadedRequestPlugins() {
		List<RequestProcessingPlugin> retVal = new LinkedList<RequestProcessingPlugin>();
		retVal.addAll(requestPlugins);
		return retVal;
	}
	
	public List<ResponseProcessingPlugin> getLoadedResponsePlugins() {
		List<ResponseProcessingPlugin> retVal = new LinkedList<ResponseProcessingPlugin>();
		retVal.addAll(responsePlugins);
		return retVal;
	}
	
	public void setProxyIsDying() {
		proxyDying = true;
	}
	
	public boolean isProxyDying(){
		return proxyDying;
	}
}
