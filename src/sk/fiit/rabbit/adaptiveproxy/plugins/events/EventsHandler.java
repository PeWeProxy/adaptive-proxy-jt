package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import rabbit.proxy.Connection;
import sk.fiit.rabbit.adaptiveproxy.AdaptiveEngine;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;

public class EventsHandler {
	private static final Logger log = Logger.getLogger(EventsHandler.class);
			
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
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (CloseEventPlugin plugin : closeEventPlugins) {
					try {
					plugin.clientClosedConnection(clientSocketAdr);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}
	
	public void logProxyClosedCon(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				for (CloseEventPlugin plugin : closeEventPlugins) {
					try {
						plugin.proxyClosedConnection(clientSocketAdr, request);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		};
		if (adaptiveEngine.isProxyDying())
			task.run();
		else
			adaptiveEngine.getProxy().getTaskRunner().runThreadTask(task);
	}

	public void logRequestReadFailed(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.requestReadFailed(clientSocketAdr, request);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logRequestDeliveryFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.requestDeliveryFailed(request);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logResponseReadFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.responseReadFailed(request, response);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logResponseDeliveryFailed(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (FailureEventPlugin plugin : failureEventPlugins) {
					try {
						plugin.responseDeliveryFailed(request, response);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logRequestReadTimeout(Connection con) {
		final InetSocketAddress clientSocketAdr = (InetSocketAddress) con.getChannel().socket().getRemoteSocketAddress();
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.requestReadTimeout(clientSocketAdr, request);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logRequestDeliveryTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.requestDeliveryTimeout(request);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logResponseReadTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.responseReadTimeout(request, response);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
	}

	public void logResponseDeliveryTimeout(Connection con) {
		final ModifiableHttpRequest request = adaptiveEngine.getRequestForConnection(con);
		final ModifiableHttpResponse response = adaptiveEngine.getResponseForConnection(con);
		adaptiveEngine.getProxy().getTaskRunner().runThreadTask(new Runnable() {
			@Override
			public void run() {
				for (TimeoutEventPlugin plugin : timeoutEventPlugins) {
					try {
						plugin.responseDeliveryTimeout(request, response);
					} catch (Exception e) {
						log.debug("Exception thrown during processing event by event plugin '"+plugin+"'",e);
					}
				}
			}
		});
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
