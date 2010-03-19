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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceProvider;

public class RequestServiceHandleImpl extends ServicesHandleBase<ModifiableHttpRequestImpl,RequestServiceModule> {
	static List<RequestServiceModule> plugins;
	static Map<ServiceModule, Set<Class<? extends ProxyService>>> providedServices;
	
	public static void initPlugins(PluginHandler pluginHandler) {
		plugins = new LinkedList<RequestServiceModule>();
		plugins.addAll(pluginHandler.getPlugins(RequestServiceModule.class));
		providedServices = new HashMap<ServiceModule, Set<Class<? extends ProxyService>>>();
		Map<ServiceModule, Set<Class<? extends ProxyService>>> dependencies
			= new HashMap<ServiceModule, Set<Class<? extends ProxyService>>>();;
		for (RequestServiceModule plugin : plugins) {
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
	
	public static List<RequestServiceModule> getLoadedModules() {
		List<RequestServiceModule> retVal = new LinkedList<RequestServiceModule>();
		retVal.addAll(plugins);
		return retVal;
	}
	
	public RequestServiceHandleImpl(ModifiableHttpRequestImpl request) {
		super(request, plugins);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedServices(RequestServiceModule plugin) {
		return providedServices.get(plugin);
	}
	
	@Override
	Set<Class<? extends ProxyService>> discoverDesiredServices(RequestServiceModule plugin) {
		try {
			return plugin.desiredRequestServices(httpMessage.getClientRequestHeaders());
		} catch (Throwable t) {
			log.info("Throwable raised while obtaining set of desired services from RequestServiceModule '"+plugin+"'",t);
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
		for (RequestServiceModule plugin : plugins) {
			List<RequestServiceProvider> providers = null;
			try {
				providers = plugin.provideRequestServices(httpMessage);
			} catch (Throwable t) {
				log.info("Throwable raised while obtaining service providers from RequestServiceModule '"+plugin+"'",t);
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