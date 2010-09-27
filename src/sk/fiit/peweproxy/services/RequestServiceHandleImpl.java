package sk.fiit.peweproxy.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.ModifiableHttpRequestImpl;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;

public class RequestServiceHandleImpl extends ServicesHandleBase<ModifiableHttpRequestImpl,RequestServiceModule> {
	
	public RequestServiceHandleImpl(ModifiableHttpRequestImpl request, ModulesManager modulesManager) {
		super(request, modulesManager.getRequestModules(), modulesManager);
	}
	
	Set<Class<? extends ProxyService>> getProvidedSvcs(RequestServiceModule module) {
		return manager.getProvidedRequestServices(module);
	}
	
	@Override
	Set<Class<? extends ProxyService>> discoverDesiredServices(RequestServiceModule plugin) {
		return plugin.desiredRequestServices(httpMessage.getClientRequestHeader());
	}
	
	@Override
	String getText4Logging(LogText type) {
		if (type == LogText.CAPITAL)
			return "Request";
		if (type == LogText.SHORT)
			return "RQ";
		return "request";
	}
	
	@Override
	<Service extends ProxyService> ServiceProvider<Service> callProvideService(RequestServiceModule module,
			Class<Service> serviceClass) {
		return module.provideRequestService(httpMessage, serviceClass);
	}
	
	@Override
	<Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider) {
		((RequestServiceProvider<Service>) svcProvider).doChanges(httpMessage);
	}
}