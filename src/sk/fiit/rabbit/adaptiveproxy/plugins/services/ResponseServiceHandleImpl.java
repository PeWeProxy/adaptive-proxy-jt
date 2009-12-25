package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import rabbit.http.HttpHeader;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponseImpl;

public final class ResponseServiceHandleImpl extends ServicesHandleBase<ModifiableHttpResponseImpl> {
	static List<ResponseServicePlugin> plugins;
	
	public static void initPlugins(PluginHandler pluginHandler) {
		plugins = new LinkedList<ResponseServicePlugin>();
		plugins.addAll(pluginHandler.getPlugins(ResponseServicePlugin.class));
		Collections.sort(plugins, new ServicePluginsComparator(plugins));
	}
	
	public static List<ResponseServicePlugin> getLoadedModules() {
		List<ResponseServicePlugin> retVal = new LinkedList<ResponseServicePlugin>();
		retVal.addAll(plugins);
		return retVal;
	}
	
	public ResponseServiceHandleImpl(ModifiableHttpResponseImpl response) {
		super(response);
		doContentNeedDiscovery(plugins);
	}

	@Override
	boolean discoverContentNeed(ServicePlugin plugin) {
		return ((ResponseServicePlugin)plugin).wantResponseContent(httpMessage.getWebResponseHeaders());
	}
	
	@Override
	String getText4Logging(loggingTextTypes type) {
		if (type == loggingTextTypes.CAPITAL)
			return "Response";
		if (type == loggingTextTypes.SHORT)
			return "RP";
		return "response";
	}
	
	@Override
	void doSpecificServiceDiscovery() {
		for (ResponseServicePlugin plugin : plugins) {
			List<ResponseServiceProvider> providers = null;
			try {
				providers = plugin.provideResponseServices(httpMessage);
			} catch (Throwable t) {
				log.info("Throwable raised while obtaining service providers from ResponseServiceProvider of class '"+plugin.getClass()+"'",t);
			}
			if (providers != null && !providers.isEmpty())
				addServiceProviders(plugin,providers);
		}
	}
	
	@Override
	void doSetContext() {
		for (ServiceProvider serviceProvider : providersList) {
			((ResponseServiceProvider) serviceProvider).setResponseContext(httpMessage);
		}
	}
	
	@Override
	HttpHeader getOriginalHeader() {
		return httpMessage.getWebResponseHeaders().getBackedHeader();
	}
	
	@Override
	HttpHeader getProxyHeader() {
		return httpMessage.getProxyResponseHeaders().getBackedHeader();
	}
	
	@Override
	HttpHeader getRequestHeader() {
		return httpMessage.getProxyRequestHeaders().getBackedHeader();
	}
}
