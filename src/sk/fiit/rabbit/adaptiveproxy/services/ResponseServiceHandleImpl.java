package sk.fiit.rabbit.adaptiveproxy.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rabbit.http.HttpHeader;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponseImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceProvider;

public final class ResponseServiceHandleImpl extends ServicesHandleBase<ModifiableHttpResponseImpl,ResponseServiceModule> {
	static List<ResponseServiceModule> plugins;
	static Map<ServiceModule, Set<Class<? extends ProxyService>>> providedServices;
	
	public static void initPlugins(PluginHandler pluginHandler) {
		plugins = new LinkedList<ResponseServiceModule>();
		plugins.addAll(pluginHandler.getPlugins(ResponseServiceModule.class));
		providedServices = new HashMap<ServiceModule, Set<Class<? extends ProxyService>>>();
		Map<ServiceModule, Set<Class<? extends ProxyService>>> dependencies
			= new HashMap<ServiceModule, Set<Class<? extends ProxyService>>>();;
		for (ResponseServiceModule plugin : plugins) {
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
	
	public static List<ResponseServiceModule> getLoadedModules() {
		List<ResponseServiceModule> retVal = new LinkedList<ResponseServiceModule>();
		retVal.addAll(plugins);
		return retVal;
	}
	
	public ResponseServiceHandleImpl(ModifiableHttpResponseImpl response) {
		super(response, plugins);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedServices(ResponseServiceModule plugin) {
		return providedServices.get(plugin);
	}
	
	@Override
	Set<Class<? extends ProxyService>> discoverDesiredServices(ResponseServiceModule plugin) {
		try {
			return plugin.desiredResponseServices(httpMessage.getWebResponseHeader());
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
		for (ResponseServiceModule plugin : plugins) {
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
		return httpMessage.getWebResponseHeader().getBackedHeader();
	}
	
	@Override
	HttpHeader getProxyHeader() {
		return httpMessage.getProxyResponseHeader().getBackedHeader();
	}
	
	@Override
	HttpHeader getRequestHeader() {
		return httpMessage.getProxyRequestHeader().getBackedHeader();
	}
}