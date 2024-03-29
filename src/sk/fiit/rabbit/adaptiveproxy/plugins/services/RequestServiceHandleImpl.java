package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import rabbit.http.HttpHeader;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequestImpl;

public class RequestServiceHandleImpl extends ServicesHandleBase<ModifiableHttpRequestImpl> {
	static List<RequestServicePlugin> plugins;
	
	public static void initPlugins(PluginHandler pluginHandler) {
		plugins = new LinkedList<RequestServicePlugin>();
		plugins.addAll(pluginHandler.getPlugins(RequestServicePlugin.class));
		Collections.sort(plugins, new ServicePluginsComparator(plugins));
	}
	
	public static List<RequestServicePlugin> getLoadedModules() {
		List<RequestServicePlugin> retVal = new LinkedList<RequestServicePlugin>();
		retVal.addAll(plugins);
		return retVal;
	}
	
	public RequestServiceHandleImpl(ModifiableHttpRequestImpl request) {
		super(request);
		doContentNeedDiscovery(plugins);
	}
	
	@Override
	boolean discoverContentNeed(ServicePlugin plugin) {
		return ((RequestServicePlugin)plugin).wantRequestContent(httpMessage.getClientRequestHeaders());
	}
	
	@Override
	String getText4Logging(loggingTextTypes type) {
		if (type == loggingTextTypes.CAPITAL)
			return "Request";
		if (type == loggingTextTypes.SHORT)
			return "RQ";
		return "request";
	}
	
	@Override
	void doSpecificServiceDiscovery() {
		for (RequestServicePlugin plugin : plugins) {
			List<RequestServiceProvider> providers = null;
			try {
				providers = plugin.provideRequestServices(httpMessage);
			} catch (Throwable t) {
				log.info("Throwable raised while obtaining service providers from RequestServicePlugin of class '"+plugin.getClass()+"'",t);
			}
			if (providers != null && !providers.isEmpty())
				addServiceProviders(plugin,providers);
		}
	}
	
	@Override
	void doSetContext() {
		for (ServiceProvider serviceProvider : providersList) {
			((RequestServiceProvider) serviceProvider).setRequestContext(httpMessage);
		}
	}

	@Override
	HttpHeader getOriginalHeader() {
		return httpMessage.getClientRequestHeaders().getBackedHeader();
	}
	
	@Override
	HttpHeader getProxyHeader() {
		return httpMessage.getProxyRequestHeaders().getBackedHeader();
	}
	
	@Override
	HttpHeader getRequestHeader() {
		return getOriginalHeader();
	}
}
