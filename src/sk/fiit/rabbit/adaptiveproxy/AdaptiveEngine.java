package sk.fiit.rabbit.adaptiveproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import rabbit.handler.AdaptiveHandler;
import rabbit.handler.BaseHandler;
import rabbit.handler.Handler;
import rabbit.http.HttpHeader;
import rabbit.httpio.request.ClientResourceHandler;
import rabbit.httpio.request.ContentFetcher;
import rabbit.httpio.request.ContentSeparator;
import rabbit.httpio.request.ContentSource;
import rabbit.httpio.request.DirectContentSource;
import rabbit.httpio.request.PrefetchedContentSource;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import rabbit.nio.DefaultTaskIdentifier;
import rabbit.proxy.Connection;
import rabbit.proxy.HttpProxy;
import rabbit.proxy.TrafficLoggerHandler;
import rabbit.util.SProperties;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicesHandleBase;

public class AdaptiveEngine  {
	private static final Logger log = Logger.getLogger(AdaptiveEngine.class);
	
	private static final File homeDir = new File(System.getProperty("user.dir"));
	private static final String ORDERING_REQUEST_TEXT = "[request]";
	private static final String ORDERING_RESPONSE_TEXT = "[response]";
	private static final String DE_PATTERN_TEXTMSGS = "^text/.*|^application/xhtml(\\+xml)?.*";
	
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
	//private final SimpleUserHandler userHandler;
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
	
	public AdaptiveEngine(HttpProxy proxy) {
		requestHandles = new HashMap<Connection, ConnectionHandle>();
		this.proxy = proxy;
		loggingHandler = new EventsHandler(this);
		//userHandler = new SimpleUserHandler();
		//userHandler.setFile("./conf/users");
		pluginHandler = new PluginHandler();
		requestPlugins = new LinkedList<RequestProcessingPlugin>();
		responsePlugins = new LinkedList<ResponseProcessingPlugin>();
	}
	
	public ModifiableHttpRequest getRequestForConnection(Connection con) {
		if (con == null) {
			log.warn("Trying to get request for null connection");
			return null;
		}
		if (!requestHandles.containsKey(con)) {
			//stackTraceWatcher.printStackTrace(con);
			log.warn("No handle for connection, can't return request");
			return null;
		}
		return requestHandles.get(con).request;
	}
	
	public ModifiableHttpResponse getResponseForConnection(Connection con) {
		if (con == null) {
			log.warn("Trying to get response for null connection");
			return null;
		}
		if (!requestHandles.containsKey(con)) {
			//stackTraceWatcher.printStackTrace(con);
			log.warn("No handle for connection, can't return response");
			return null;
		}
		return requestHandles.get(con).response;
	}
	
	public void newRequestAttempt(Connection con) {
		ConnectionHandle newHandle = new ConnectionHandle(con);
		requestHandles.put(con, newHandle);
		if (log.isTraceEnabled())
			log.trace("RQ: "+newHandle+" | Registering new connection for connection "+con);
	}
	
	public void connectionClosed(Connection con) {
		//stackTraceWatcher.addStackTrace(con);
		ConnectionHandle conHandle = requestHandles.remove(con);
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | Removing connection for connection "+con);
	}
	
	public void newRequest(Connection con, boolean chunking) {
		ConnectionHandle conHandle = requestHandles.get(con);
		InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		conHandle.request = new ModifiableHttpRequestImpl(new HeaderWrapper(con.getClientRHeader()),clientSocketAdr);
		conHandle.messageFactory = new HttpMessageFactoryImpl(con,conHandle.request);
		conHandle.requestChunking = chunking;
		con.setProxyRHeader(conHandle.request.getProxyRequestHeaders().getBackedHeader());
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | New request received ("
					+conHandle.request.getClientRequestHeaders().getRequestLine()+")");
	}
	
	public void cacheRequestIfNeeded(final Connection con, ContentSeparator separator, Long dataSize) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		BufferHandle bufHandle = con.getRequestBufferHandle();
		ClientResourceHandler resourceHandler = null;
		if (separator != null) {
			// request has some content
			if (log.isTraceEnabled())
				log.trace("RQ: "+conHandle+" | Request separator used: "+separator.toString());
			TrafficLoggerHandler tlh = con.getTrafficLoggerHandler();
			boolean prefetch = conHandle.request.getServiceHandle().wantContent();
			if (!prefetch) {
				for (RequestProcessingPlugin requestPlugin : requestPlugins) {
					if (requestPlugin.wantRequestContent(conHandle.request.getClientRequestHeaders())) {
						if (log.isDebugEnabled())
							log.debug("RQ: "+conHandle+" | Plugin "+requestPlugin+" wants request content");
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
				if (log.isDebugEnabled())
					log.debug("RQ: "+conHandle+" | No plugin wants request content");
				ContentSource directSource = new DirectContentSource(con,bufHandle,tlh,separator);
				resourceHandler = new ClientResourceHandler(con,directSource,conHandle.requestChunking);
			}
		} else {
			if (log.isTraceEnabled())
				log.trace("RQ: "+conHandle+" | Request has no body");
		}
		final ClientResourceHandler handler = resourceHandler;
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				if (runRequestAdapters(conHandle))
					proceedWithRequest(conHandle, handler);
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestProcessing", requestProcessingTaskInfo(conHandle)));
	}
	
	public void requestContentCached(final Connection con, final byte[] requestContent, final Queue<Integer> dataIncrements) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | "+requestContent.length+" bytes of request cached");
		proxy.getNioHandler().runThreadTask(new Runnable() {
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
					proceedWithRequest(conHandle, resourceHandler);
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestProcessing", requestProcessingTaskInfo(conHandle)));
	}
	
	private boolean runRequestAdapters(ConnectionHandle conHandle) {
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | Running request adapters");
		boolean again = false;
		Set<RequestProcessingPlugin> pluginsChangedResponse = new HashSet<RequestProcessingPlugin>();
		do {
			// Discover services EVERY TIME there's new request
			conHandle.request.getServiceHandle().doServiceDiscovery();
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
				} catch (Throwable t) {
					log.info("RQ: "+conHandle+" | Throwable raised while processing request with RequestProcessingPlugin '"+requestPlugin+"'",t);
					// TODO revert changes maybe ?
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

	void proceedWithRequest(final ConnectionHandle conHandle, final ClientResourceHandler resourceHandler) {
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				if (log.isTraceEnabled()) {
					log.trace("RQ: "+conHandle+" | Request handling time :"+(System.currentTimeMillis()-conHandle.requestTime));
					log.trace("RQ: "+conHandle+" | Proceeding in processing request");
				}
				conHandle.con.processRequest(resourceHandler);
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".requestAdvancing", requestSendingTaskInfo(conHandle)));
	}
	
	public void newResponse(Connection con, HttpHeader response) {
		ConnectionHandle conHandle = requestHandles.get(con);
		conHandle.responseTime = System.currentTimeMillis();
		conHandle.response = new ModifiableHttpResponseImpl(new HeaderWrapper(response),conHandle.request);
		if (log.isTraceEnabled())
			log.trace("RP: "+conHandle+" | New response received ( "
					+response.getReasonPhrase()+" | requested "
					+conHandle.request.getProxyRequestHeaders().getRequestLine()+")");
	}
	
	public void newResponse(Connection con, HttpHeader header, final Runnable proceedTask) {
		if (log.isTraceEnabled())
			log.trace("RP: "+requestHandles.get(con)+" | New response constructed");
		newResponse(con, header);
		processResponse(con, proceedTask);
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
			if (responsePlugin.wantResponseContent(conHandle.response.getWebResponseHeaders())) {
				if (log.isDebugEnabled())
					log.debug("RP: "+conHandle+" | Plugin "+responsePlugin+" wants request content");
				return true;
			}
		}
		if (log.isDebugEnabled())
			log.debug("RP: "+conHandle+" | No plugin wants response");
		return false;
	}

	public void processResponse(final Connection con, final Runnable proceedTask) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		if (!conHandle.adaptiveHandling && !proxyDying) {
			proxy.getNioHandler().runThreadTask(new Runnable() {
				@Override
				public void run() {
					runResponseAdapters(conHandle);
					proceedWithResponse(conHandle, new Runnable() {
						@Override
						public void run() {
							proceedTask.run();
						}
					});
				}
			}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseProcessing", responseProcessingTaskInfo(conHandle)));
		} else {
			if (log.isTraceEnabled())
				log.trace("RP: "+conHandle+" | Response handling time :"+(System.currentTimeMillis()-conHandle.responseTime));
			proceedTask.run();
		}
	}
	
	public void responseContentCached(Connection con, final byte[] responseContent, final AdaptiveHandler handler) {
		final ConnectionHandle conHandle = requestHandles.get(con);
		if (log.isTraceEnabled())
			log.trace("RP: "+conHandle+" | "+responseContent.length+" bytes of response cached");
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				processCachedResponse(conHandle, responseContent, handler);
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseProcessing", responseProcessingTaskInfo(conHandle)));
	}
	
	private void processCachedResponse(final ConnectionHandle conHandle, byte[] responseContent, final AdaptiveHandler handler) {
		conHandle.response.setData(responseContent);
		runResponseAdapters(conHandle);
		final ModifiableHttpResponseImpl response = conHandle.response;
		final byte[] modifiedContent = conHandle.response.getData();
		proceedWithResponse(conHandle, new Runnable() {
			@Override
			public void run() {
				handler.sendResponse(response.getProxyResponseHeaders().getBackedHeader(),modifiedContent);
			}
		});
	}
	
	private void runResponseAdapters(ConnectionHandle conHandle) {
		if (log.isTraceEnabled())
			log.trace("RP: "+conHandle+" | Running response adapters");
		boolean again = false;
		Set<ResponseProcessingPlugin> pluginsChangedResponse = new HashSet<ResponseProcessingPlugin>();
		do {
			// Discover services EVERY TIME there's new response
			conHandle.response.getServiceHandle().doServiceDiscovery();
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
				} catch (Throwable t) {
					log.info("RP: "+conHandle+" | Throwable raised while processing response with ResponseProcessingPlugin '"+responsePlugin+"'",t);
					// TODO revert changes maybe ?
				}
			}
		} while (again);
		conHandle.response.getServiceHandle().doChanges();
	}
	
	private void proceedWithResponse(final ConnectionHandle conHandle, final Runnable proceedTask) {
		proxy.getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				if (log.isTraceEnabled()) {
					log.trace("RP: "+conHandle+" | Response handling time :"+(System.currentTimeMillis()-conHandle.responseTime));
					log.trace("RQ: "+conHandle+" | Proceeding in processing response");
				}
				proceedTask.run();
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".responseAdvancing", responseSendingTaskInfo(conHandle)));
	}

	
	private void sendResponse(final ConnectionHandle conHandle, boolean processResponse) {
		if (processResponse) {
			runResponseAdapters(conHandle);
		} else {
			conHandle.response.getServiceHandle().doChanges();
		}
		if (log.isTraceEnabled())
			log.trace("RQ: "+conHandle+" | Sending response");
		AdaptiveHandler handlerFactory = (AdaptiveHandler)conHandle.con.getProxy().getNamedHandlerFactory(AdaptiveHandler.class.getName());
		BufferHandle bufHandle = new SimpleBufferHandle(ByteBuffer.wrap(new byte[4096]));
		Connection con = conHandle.con;
		TrafficLoggerHandler tlh = con.getTrafficLoggerHandler();
		HttpHeader requestHeaders = conHandle.request.getProxyRequestHeaders().getBackedHeader();
		final HttpHeader responseHeaders = conHandle.response.getProxyResponseHeaders().getBackedHeader();
		//conHandle.response.getProxyResponseHeaders().setHeader("Transfer-Encoding","chunked");
		handlerFactory.nextInstanceWillNotCache();
		final AdaptiveHandler sendingHandler = (AdaptiveHandler)handlerFactory.getNewInstance(conHandle.con, tlh, requestHeaders, bufHandle, responseHeaders, null, con.getMayCache(), con.getMayFilter(), -1);
		proceedWithResponse(conHandle, new Runnable() {
			@Override
			public void run() {
				sendingHandler.sendResponse(responseHeaders, conHandle.response.getData());
			}
		});
	}
	
	private String requestProcessingTaskInfo(ConnectionHandle conHandle) {
		return "Running plugins processing on request \""
			+ conHandle.request.getClientRequestHeaders().getRequestLine()
			+ "\" from " + conHandle.request.getClientSocketAddress();
	}

	private String responseProcessingTaskInfo (ConnectionHandle conHandle) {
		return "Running plugins processing on response \""
			+ conHandle.response.getWebResponseHeaders().getStatusLine()
			+ "\" for request \"" + conHandle.request.getProxyRequestHeaders().getRequestLine()
			+ "\" from " + conHandle.request.getClientSocketAddress();
	}

	private String requestSendingTaskInfo(ConnectionHandle conHandle) {
		return "Proceeding in handling request \""
			+ conHandle.request.getProxyRequestHeaders().getRequestLine()
			+ "\" (orig: \""+ conHandle.request.getClientRequestHeaders().getRequestLine()
			+ "\") from " + conHandle.request.getClientSocketAddress();
	}

	private String responseSendingTaskInfo (ConnectionHandle conHandle) {
		return "Proceeding in handling response \""
			+ conHandle.response.getProxyResponseHeaders().getStatusLine()
			+ "\" (orig: \""+ conHandle.response.getWebResponseHeaders().getStatusLine()
			+ "\") for request \"" + conHandle.request.getProxyRequestHeaders().getRequestLine()
			+ "\" from " + conHandle.request.getClientSocketAddress();
	}

	public HttpProxy getProxy() {
		return proxy;
	}

	public PluginHandler getPluginHandler() {
		return pluginHandler;
	}

	public EventsHandler getEventsHandler() {
		return loggingHandler;
	}

	public void setup(SProperties prop) {
		if (prop == null)
			prop = new SProperties();
		String log4jConfFile = prop.getProperty("logging_conf");
		File loggingConfFile = null;
		if (log4jConfFile != null) {
			loggingConfFile = new File(homeDir,log4jConfFile); 
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
			Logger.getRootLogger().setLevel(lvl);
			Logger.getLogger("org.apache").setLevel(Level.WARN);
			log.setLevel(lvl);
			BasicConfigurator.configure();
			log.info("No Log4j configuration file specified, using default configuration");
		}
		String pluginsHomeProp = prop.getProperty("pluginsHome");
		boolean homeDirConfigPresent = pluginsHomeProp != null;
		if (pluginsHomeProp == null) pluginsHomeProp = "plugins"; 
		File pluginsHomeDir = new File(homeDir,pluginsHomeProp);
		String pluginsOrderFileName = prop.getProperty("pluginsOrderFile","plugins_ordering");
		pluginsOrderFile = new File(pluginsHomeDir,pluginsOrderFileName);
		if (!pluginsHomeDir.isDirectory() || !pluginsHomeDir.canRead()) {
			log.info("Unable to find or access "+((!homeDirConfigPresent)? "default ":"")+"plugins directory "+pluginsHomeDir.getAbsolutePath());
		} else {
			File svcsDir = new File(pluginsHomeDir,prop.getProperty("services_folder","services"));
			File sharedLibDir = null;
			String sharedLibDirName = prop.getProperty("shared_libs_folder","shared_libs");
			if (!sharedLibDirName.trim().isEmpty())
				sharedLibDir = new File(pluginsHomeDir,sharedLibDirName);
			Set<String> excludeFiles = new HashSet<String>();
			excludeFiles.add(pluginsOrderFileName);
			pluginHandler.setPluginRepository(pluginsHomeDir,svcsDir,sharedLibDir,excludeFiles);
		}
		String patternString = prop.getProperty("stringServicesPattern", DE_PATTERN_TEXTMSGS);
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(patternString);
		} catch (PatternSyntaxException e) {
			log.info("Invalid or no pattern for string content services, default one will be used", e);
			pattern = Pattern.compile(DE_PATTERN_TEXTMSGS);
		}
		ServicesHandleBase.setStringServicesPattern(pattern);
		reloadPlugins();
		log.info("AdaptiveProxy set up and ready for action");
	}
	
	public void reloadPlugins() {
		log.info("Reloading plugins");
		pluginHandler.reloadPlugins();
		loggingHandler.setup();
		log.info("Loading request service plugins");
		RequestServiceHandleImpl.initPlugins(pluginHandler);
		log.info("Loading response service plugins");
		ResponseServiceHandleImpl.initPlugins(pluginHandler);
		requestPlugins.clear();
		responsePlugins.clear();
		log.info("Loading request processing plugins");
		Set<RequestProcessingPlugin> requestPluginsSet = pluginHandler.getPlugins(RequestProcessingPlugin.class);
		log.info("Loading response processing plugins");
		Set<ResponseProcessingPlugin> responsePluginsSet = pluginHandler.getPlugins(ResponseProcessingPlugin.class);
		boolean pluginsOrderingSuccess;
		if (pluginsOrderFile != null && pluginsOrderFile.canRead()) {
			pluginsOrderingSuccess = addProcessingPluginsInOrder(pluginsOrderFile,requestPluginsSet,responsePluginsSet);
		} else {
			log.info("Unable to find or read plugins ordering file "+pluginsOrderFile.getAbsolutePath());
			pluginsOrderingSuccess = false;
		}
		if (!pluginsOrderingSuccess) {
			log.info("Loading of configuration of plugin ordering failed for some reason. "+
					"Processing plugins will not be called in some desired fashion");
		}
		requestPlugins.addAll(requestPluginsSet);
		responsePlugins.addAll(responsePluginsSet);
	}
	
	private boolean addProcessingPluginsInOrder(File pluginsOrderFile, Set<RequestProcessingPlugin> requestPluginsSet,
			Set<ResponseProcessingPlugin> responsePluginsSet) {
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
				if (wasResponseMark)
					sucess = loadPlugin(line, responsePlugins, responsePluginsSet, ResponseProcessingPlugin.class);
				else
					sucess = loadPlugin(line, requestPlugins, requestPluginsSet, RequestProcessingPlugin.class);
				if (!sucess)
					log.info("Can't insert plugin with name '"+line+"' into "+((wasResponseMark)?"response":"resuest")+" processing order, because such plugin is not present");
			}
		}
		return true;
	}
	
	private <T extends ProxyPlugin> boolean loadPlugin(String pluginName, List<T> pluginsList,
		Set<T> pluginsSet, Class<T> pluginsClass) {
		T plugin = pluginHandler.getPlugin(pluginName, pluginsClass);
		if (plugin == null)
			return false;
		pluginsList.add(plugin);
		pluginsSet.remove(plugin);
		return true;
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

	public void responseHandlerUsed(Connection connection, Handler handler) {
		ConnectionHandle conHandle = requestHandles.get(connection);
		if (log.isTraceEnabled())
			log.trace("RP: "+conHandle+" | Handler "+handler.toString()+" used for response on "
					+conHandle.request.getProxyRequestHeaders().getRequestLine());
	}
}
