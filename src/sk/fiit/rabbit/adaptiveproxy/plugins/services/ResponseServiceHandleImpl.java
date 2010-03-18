package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rabbit.http.HttpHeader;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponseImpl;

public final class ResponseServiceHandleImpl extends ServicesHandleBase<ModifiableHttpResponseImpl,ResponseServicePlugin> {
	static List<ResponseServicePlugin> plugins;
	static Map<ServicePlugin, Set<Class<? extends ProxyService>>> providedServices;
	
	public static void initPlugins(PluginHandler pluginHandler) {
		plugins = new LinkedList<ResponseServicePlugin>();
		plugins.addAll(pluginHandler.getPlugins(ResponseServicePlugin.class));
		providedServices = new HashMap<ServicePlugin, Set<Class<? extends ProxyService>>>();
		Map<ServicePlugin, Set<Class<? extends ProxyService>>> dependencies
			= new HashMap<ServicePlugin, Set<Class<? extends ProxyService>>>();;
		for (ResponseServicePlugin plugin : plugins) {
			try {
				Set<Class<? extends ProxyService>> pluginDependencies = plugin.getDependencies();
				if (pluginDependencies != null)
					dependencies.put(plugin, pluginDependencies);
				Set<Class<? extends ProxyService>> pluginServices = plugin.getProvidedServices();
				if (pluginServices != null)
					providedServices.put(plugin, pluginServices);
			} catch (Throwable e) {
				// TODO: handle exception
			}
		}
		Collections.sort(plugins, new ServicePluginsComparator(dependencies,providedServices));
	}
	
	public static List<ResponseServicePlugin> getLoadedModules() {
		List<ResponseServicePlugin> retVal = new LinkedList<ResponseServicePlugin>();
		retVal.addAll(plugins);
		return retVal;
	}
	
	public ResponseServiceHandleImpl(ModifiableHttpResponseImpl response) {
		super(response, plugins);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedServices(ResponseServicePlugin plugin) {
		return providedServices.get(plugin);
	}
	
	@Override
	Set<Class<? extends ProxyService>> discoverDesiredServices(ResponseServicePlugin plugin) {
		try {
			return plugin.desiredResponseServices(httpMessage.getWebResponseHeaders());
		} catch (Throwable t) {
			log.info("RP | Throwable raised while obtaining set of desired services from ResponseServicePlugin of class '"+plugin.getClass()+"'",t);
		}
		return Collections.emptySet();
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
				log.info("RP | Throwable raised while obtaining service providers from ResponseServicePlugin of class '"+plugin.getClass()+"'",t);
			}
			if (providers != null && !providers.isEmpty())
				addServiceProviders(plugin,providers);
		}
	}
	
	@Override
	void doSetContext() {
		for (ServiceProvider serviceProvider : providersList) {
			try {
				((ResponseServiceProvider) serviceProvider).setResponseContext(httpMessage);
			} catch (Throwable t) {
				log.info("RP: | Throwable raised while seting response context to provider "+serviceProvider,t);
			}
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