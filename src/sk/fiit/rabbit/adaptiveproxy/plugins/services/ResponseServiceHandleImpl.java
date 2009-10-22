package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import rabbit.http.HttpHeader;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponseImpl;

public final class ResponseServiceHandleImpl extends ServicesHandleBase {
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
	
	final ModifiableHttpResponseImpl response;
	
	public ResponseServiceHandleImpl(ModifiableHttpResponseImpl response) {
		super(response);
		this.response = response;
		doContentNeedDiscovery(plugins);
	}

	@Override
	boolean discoverContentNeed(ServicePlugin plugin) {
		return ((ResponseServicePlugin)plugin).wantResponseContent(response.getWebResponseHeaders());
	}
	
	@Override
	void doSpecificServiceDiscovery() {
		for (ResponseServicePlugin plugin : plugins) {
			List<ResponseServiceProvider> providers = null;
			try {
				providers = plugin.provideResponseServices(response);
			} catch (Exception e) {
				log.error("Exception thrown while obtaining service providers from ResponseServiceProvider of class '"+plugin.getClass()+"'");
			}
			if (providers != null)
				addServiceProviders(plugin,providers);
		}
	}
	
	@Override
	void doSetContext() {
		for (ServiceProvider serviceProvider : providersList) {
			((ResponseServiceProvider) serviceProvider).setResponseContext(response);
		}
	}
	
	@Override
	HttpHeader getOriginalHeader() {
		return response.getWebResponseHeaders().getBackedHeader();
	}
	
	@Override
	HttpHeader getProxyHeader() {
		return response.getProxyResponseHeaders().getBackedHeader();
	}
	
	@Override
	HttpHeader getRequestHeader() {
		return response.getProxyRequestHeaders().getBackedHeader();
	}
}
