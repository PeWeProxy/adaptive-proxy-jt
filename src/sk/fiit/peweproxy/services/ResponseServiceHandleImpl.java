package sk.fiit.peweproxy.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.HttpResponseImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpResponseImpl;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;

public final class ResponseServiceHandleImpl extends ServicesHandleBase<ResponseServiceModule> {
	
	public ResponseServiceHandleImpl(HttpResponseImpl response, ModulesManager modulesManager) {
		super(response, modulesManager.getResponseModules(), modulesManager);
	}
	
	Set<Class<? extends ProxyService>> getProvidedSvcs(ResponseServiceModule module) {
		return manager.getProvidedResponseServices(module);
	}
	
	@Override
	void discoverDesiredServices(ResponseServiceModule plugin,
			Set<Class<? extends ProxyService>> desiredServices) {
		plugin.desiredResponseServices(desiredServices,httpMessage.getHeader());
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
		return module.provideResponseService((HttpResponse)httpMessage, serviceClass);
	}
	
	@Override
	<Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider) {
		// doChanges can be called only on modifiable message, so it's OK to crash if the cast fails
		((ResponseServiceProvider<Service>) svcProvider).doChanges((ModifiableHttpResponseImpl)httpMessage);
	}
}