package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import rabbit.nio.DefaultTaskIdentifier;
import rabbit.proxy.Connection;
import sk.fiit.rabbit.adaptiveproxy.AdaptiveEngine;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;

public class EventsHandler {
	static final Logger log = Logger.getLogger(EventsHandler.class);
	
	private final AdaptiveEngine adaptiveEngine;
	private final List<CloseEventPlugin> closeEventPlugins;
	private final List<TimeoutEventPlugin> timeoutEventPlugins;
	private final List<FailureEventPlugin> failureEventPlugins;
	
	public EventsHandler(AdaptiveEngine adaptiveEngine) {
		this.adaptiveEngine = adaptiveEngine;
		closeEventPlugins = new LinkedList<CloseEventPlugin>();
		timeoutEventPlugins = new LinkedList<TimeoutEventPlugin>();
		failureEventPlugins = new LinkedList<FailureEventPlugin>();
	}
	
	public void setup() {
		PluginHandler pluginHandler = adaptiveEngine.getPluginHandler();
		closeEventPlugins.addAll(pluginHandler.getPlugins(CloseEventPlugin.class));
		timeoutEventPlugins.addAll(pluginHandler.getPlugins(TimeoutEventPlugin.class));
		failureEventPlugins.addAll(pluginHandler.getPlugins(FailureEventPlugin.class));
	}
	
	public void logClientClosedCon(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (CloseEventPlugin plugin : closeEventPlugins) {
					try {
						plugin.clientClosedConnection(clientSocketAdr);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logClientClosedCon",
				"Dispatching 'client closed the connection' message to all CloseEventPlugin plugins"));
	}
	
	public void logProxyClosedCon(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final boolean requestPresent = (request != null);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				for (CloseEventPlugin plugin : closeEventPlugins) {
					try {
						if (requestPresent)
							plugin.proxyClosedConnection(request);
						else
							plugin.proxyClosedConnection(clientSocketAdr);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		};
		if (adaptiveEngine.isProxyDying())
			task.run();
		else
			adaptiveEngine.getProxy().getNioHandler().runThreadTask(task, new DefaultTaskIdentifier(getClass().getSimpleName()+".logProxyClosedCon",
				"Dispatching 'proxy closed the connection' message to all CloseEventPlugin plugins"));
	}

	public void logRequestReadFailed(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final boolean requestPresent = (request != null);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						if (requestPresent)
							plugin.requestReadFailed(request);
						else
							plugin.requestReadFailed(clientSocketAdr);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestReadFailed",
		"Dispatching 'request read failed' message to all CloseEventPlugin plugins"));
	}

	public void logRequestDeliveryFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.requestDeliveryFailed(request);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestDeliveryFailed",
		"Dispatching 'request delivery failed' message to all CloseEventPlugin plugins"));
	}

	public void logResponseReadFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		final boolean responsePresent = (response != null);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						if (responsePresent)
							plugin.responseReadFailed(request, response);
						else
							plugin.responseReadFailed(request);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseReadFailed",
		"Dispatching 'response read failed' message to all CloseEventPlugin plugins"));
	}

	public void logResponseDeliveryFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.responseDeliveryFailed(request, response);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseDeliveryFailed",
		"Dispatching 'response delivery failed' message to all CloseEventPlugin plugins"));
	}

	public void logRequestReadTimeout(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final boolean requestPresent = (request != null);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						if (requestPresent)
							plugin.requestReadTimeout(request);
						else
							plugin.requestReadTimeout(clientSocketAdr);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestReadTimeout",
		"Dispatching 'request read timeout' message to all CloseEventPlugin plugins"));
	}

	public void logRequestDeliveryTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.requestDeliveryTimeout(request);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestDeliveryTimeout",
		"Dispatching 'request delivery timeout' message to all CloseEventPlugin plugins"));
	}

	public void logResponseReadTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		final boolean responePresent = (response != null);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						if (responePresent)
							plugin.responseReadTimeout(request, response);
						else
							plugin.responseReadTimeout(request);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseReadTimeout",
		"Dispatching 'response read timeout' message to all CloseEventPlugin plugins"));
	}

	public void logResponseDeliveryTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.responseDeliveryTimeout(request, response);
					} catch (Exception e) {
						log.error("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseDeliveryTimeout",
		"Dispatching 'response delivery timeout' message to all CloseEventPlugin plugins"));
	}
	
	public List<CloseEventPlugin> getLoadedCloseEventPlugins() {
		List<CloseEventPlugin> retVal = new LinkedList<CloseEventPlugin>();
		retVal.addAll(closeEventPlugins);
		return retVal;
	}
	
	public List<TimeoutEventPlugin> getLoadedTimeoutEventPlugins() {
		List<TimeoutEventPlugin> retVal = new LinkedList<TimeoutEventPlugin>();
		retVal.addAll(timeoutEventPlugins);
		return retVal;
	}
	
	public List<FailureEventPlugin> getLoadedFailureEventPlugins() {
		List<FailureEventPlugin> retVal = new LinkedList<FailureEventPlugin>();
		retVal.addAll(failureEventPlugins);
		return retVal;
	}
}
