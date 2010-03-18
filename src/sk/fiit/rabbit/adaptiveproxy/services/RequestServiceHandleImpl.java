package sk.fiit.rabbit.adaptiveproxy.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rabbit.http.HttpHeader;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequestImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceProvider;

public class RequestServiceHandleImpl extends ServicesHandleBase<ModifiableHttpRequestImpl,RequestServicePlugin> {
	static List<RequestServicePlugin> plugins;
	static Map<ServicePlugin, Set<Class<? extends ProxyService>>> providedServices;
	
	public static void initPlugins(PluginHandler pluginHandler) {
		plugins = new LinkedList<RequestServicePlugin>();
		plugins.addAll(pluginHandler.getPlugins(RequestServicePlugin.class));
		providedServices = new HashMap<ServicePlugin, Set<Class<? extends ProxyService>>>();
		Map<ServicePlugin, Set<Class<? extends ProxyService>>> dependencies
			= new HashMap<ServicePlugin, Set<Class<? extends ProxyService>>>();;
		for (RequestServicePlugin plugin : plugins) {
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
	
	public static List<RequestServicePlugin> getLoadedModules() {
		List<RequestServicePlugin> retVal = new LinkedList<RequestServicePlugin>();
		retVal.addAll(plugins);
		return retVal;
	}
	
	public RequestServiceHandleImpl(ModifiableHttpRequestImpl request) {
		super(request, plugins);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedServices(RequestServicePlugin plugin) {
		return providedServices.get(plugin);
	}
	
	@Override
	Set<Class<? extends ProxyService>> discoverDesiredServices(RequestServicePlugin plugin) {
		try {
			return plugin.desiredRequestServices(httpMessage.getClientRequestHeaders());
		} catch (Throwable t) {
			log.info("Throwable raised while obtaining set of desired services from RequestServicePlugin of class '"+plugin.getClass()+"'",t);
		}
		return Collections.emptySet();
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
			try {
				((RequestServiceProvider) serviceProvider).setRequestContext(httpMessage);
			} catch (Throwable t) {
				log.info("RQ: | Throwable raised while seting request context to provider "+serviceProvider,t);
			}
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