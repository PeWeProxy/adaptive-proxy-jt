package sk.fiit.peweproxy.plugins.events;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.khelekore.rnio.impl.DefaultTaskIdentifier;
import rabbit.proxy.Connection;
import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginHandler;

public class EventsHandler {
	static final Logger log = Logger.getLogger(EventsHandler.class);
	
	private final AdaptiveEngine adaptiveEngine;
	private final List<ConnectionEventPlugin> connectionEventPlugins;
	private final List<TimeoutEventPlugin> timeoutEventPlugins;
	private final List<FailureEventPlugin> failureEventPlugins;
	
	public EventsHandler(AdaptiveEngine adaptiveEngine) {
		this.adaptiveEngine = adaptiveEngine;
		connectionEventPlugins = new LinkedList<ConnectionEventPlugin>();
		timeoutEventPlugins = new LinkedList<TimeoutEventPlugin>();
		failureEventPlugins = new LinkedList<FailureEventPlugin>();
	}
	
	public void setup() {
		PluginHandler pluginHandler = adaptiveEngine.getPluginHandler();
		connectionEventPlugins.addAll(pluginHandler.getPlugins(ConnectionEventPlugin.class));
		timeoutEventPlugins.addAll(pluginHandler.getPlugins(TimeoutEventPlugin.class));
		failureEventPlugins.addAll(pluginHandler.getPlugins(FailureEventPlugin.class));
	}
	
	public void logClientMadeCon(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (ConnectionEventPlugin plugin : connectionEventPlugins) {
					try {
						plugin.clientMadeConnection(clientSocketAdr);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logClientClosedCon",
				"Dispatching 'client made the connection' message to all ConnectionEventPlugin plugins"));
	}
	
	public void logClientClosedCon(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				for (ConnectionEventPlugin plugin : connectionEventPlugins) {
					try {
						plugin.clientClosedConnection(clientSocketAdr);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		};
		if (adaptiveEngine.isProxyDying())
			task.run();
		else
			adaptiveEngine.getProxy().getNioHandler().runThreadTask(task, new DefaultTaskIdentifier(getClass().getSimpleName()+".logClientClosedCon",
				"Dispatching 'client closed the connection' message to all ConnectionEventPlugin plugins"));
	}
	
	public void logProxyClosedCon(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final boolean requestPresent = (request != null);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				for (ConnectionEventPlugin plugin : connectionEventPlugins) {
					try {
						if (requestPresent)
							plugin.proxyClosedConnection(request);
						else
							plugin.proxyClosedConnection(clientSocketAdr);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		};
		if (adaptiveEngine.isProxyDying())
			task.run();
		else
			adaptiveEngine.getProxy().getNioHandler().runThreadTask(task, new DefaultTaskIdentifier(getClass().getSimpleName()+".logProxyClosedCon",
				"Dispatching 'proxy closed the connection' message to all ConnectionEventPlugin plugins"));
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
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestReadFailed",
		"Dispatching 'request read failed' message to all FailureEventPlugin plugins"));
	}

	public void logRequestDeliveryFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.requestDeliveryFailed(request);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestDeliveryFailed",
		"Dispatching 'request delivery failed' message to all FailureEventPlugin plugins"));
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
							plugin.responseReadFailed(response);
						else
							plugin.responseReadFailed(request);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseReadFailed",
		"Dispatching 'response read failed' message to all FailureEventPlugin plugins"));
	}

	public void logResponseDeliveryFailed(Connection con) {
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.responseDeliveryFailed(response);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseDeliveryFailed",
		"Dispatching 'response delivery failed' message to all FailureEventPlugin plugins"));
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
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestReadTimeout",
		"Dispatching 'request read timeout' message to all TimeoutEventPlugin plugins"));
	}

	public void logRequestDeliveryTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.requestDeliveryTimeout(request);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logRequestDeliveryTimeout",
		"Dispatching 'request delivery timeout' message to all TimeoutEventPlugin plugins"));
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
							plugin.responseReadTimeout(response);
						else
							plugin.responseReadTimeout(request);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseReadTimeout",
		"Dispatching 'response read timeout' message to all TimeoutEventPlugin plugins"));
	}

	public void logResponseDeliveryTimeout(Connection con) {
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getNioHandler().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.responseDeliveryTimeout(response);
					} catch (Throwable t) {
						log.info("Throwable raised during processing event by event plugin '"+plugin+"'",t);
					}
				}
			}
		}, new DefaultTaskIdentifier(getClass().getSimpleName()+".logResponseDeliveryTimeout",
		"Dispatching 'response delivery timeout' message to all TimeoutEventPlugin plugins"));
	}
	
	public List<ConnectionEventPlugin> getLoadedConnectionEventPlugins() {
		List<ConnectionEventPlugin> retVal = new LinkedList<ConnectionEventPlugin>();
		retVal.addAll(connectionEventPlugins);
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