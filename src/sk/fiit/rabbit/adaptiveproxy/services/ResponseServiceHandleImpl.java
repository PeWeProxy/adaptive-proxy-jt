package sk.fiit.rabbit.adaptiveproxy.services;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponseImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceProvider;

public final class ResponseServiceHandleImpl extends ServicesHandleBase<ModifiableHttpResponseImpl,ResponseServiceModule> {
	
	public ResponseServiceHandleImpl(ModifiableHttpResponseImpl response, ModulesManager modulesManager) {
		super(response, modulesManager.getResponseModules(), modulesManager);
	}
	
	Set<Class<? extends ProxyService>> getProvidedSvcs(ResponseServiceModule module) {
		return manager.getProvidedResponseServices(module);
	}
	
	@Override
	Set<Class<? extends ProxyService>> discoverDesiredServices(ResponseServiceModule plugin) {
		return plugin.desiredResponseServices(httpMessage.getWebResponseHeader());
	}
	
	@Override
	String getText4Logging(LogText type) {
		if (type == LogText.CAPITAL)
			return "Response";
		if (type == LogText.SHORT)
			return "RP";
		return "response";
	}
	
	@Override
	<Service extends ProxyService> ServiceProvider<Service> callProvideService(ResponseServiceModule module,
				Class<Service> serviceClass) throws ServiceUnavailableException {
		return module.provideResponseService(httpMessage, serviceClass);
	}
	
	@Override
	<Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider) {
		((ResponseServiceProvider<Service>) svcProvider).doChanges(httpMessage);
	}
}