package sk.fiit.peweproxy.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpRequestImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequestImpl;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;
import sk.fiit.peweproxy.utils.Statistics.ProcessType;

public class RequestServiceHandleImpl extends ServicesHandleBase<RequestServiceModule> {
	
	public RequestServiceHandleImpl(HttpRequestImpl request, ModulesManager modulesManager) {
		super(request, modulesManager.getLoadedRequestModules(), modulesManager);
	}
	
	Set<Class<? extends ProxyService>> getProvidedSvcs(RequestServiceModule module) {
		return manager.getProvidedRequestServices(module);
	}
	
	@Override
	void discoverDesiredServices(final RequestServiceModule plugin,
			final Set<Class<? extends ProxyService>> desiredServices) throws Throwable {
		manager.getAdaptiveEngine().getStatistics().executeProcess(new Runnable() {
			@Override
			public void run() {
				plugin.desiredRequestServices(desiredServices,httpMessage.getHeader());
			}
		}, plugin, ProcessType.REQUEST_DESIRED_SERVICES, httpMessage);
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
		return module.provideRequestService((HttpRequest)httpMessage, serviceClass);
	}
	
	@Override
	<Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider) {
		// doChanges can be called only on modifiable message, so it's OK to crash if the cast fails
		((RequestServiceProvider<Service>) svcProvider).doChanges((ModifiableHttpRequestImpl)httpMessage);
	}
	
	@Override
	ProcessType serviceProvidingType() {
		return ProcessType.REQUEST_PROVIDE_SERVICE;
	}
}