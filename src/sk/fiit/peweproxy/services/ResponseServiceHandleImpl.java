package sk.fiit.peweproxy.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.HttpResponseImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpResponseImpl;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;
import sk.fiit.peweproxy.utils.Statistics.ProcessType;

public final class ResponseServiceHandleImpl extends MessageServicesHandle<ResponseServiceModule> {
	
	public ResponseServiceHandleImpl(HttpResponseImpl response, ModulesManager modulesManager) {
		super(response, modulesManager.getLoadedResponseModules(), modulesManager);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedSvcs(ResponseServiceModule module) {
		return manager.getProvidedResponseServices(module);
	}
	
	@Override
	void discoverDesiredServices(final ResponseServiceModule plugin,
			final Set<Class<? extends ProxyService>> desiredServices,
			final boolean msgChunked) throws Throwable {
		manager.getAdaptiveEngine().getStatistics().executeProcess(new Runnable() {
			@Override
			public void run() {
				plugin.desiredResponseServices(desiredServices,httpMessage.getHeader(),msgChunked);
			}
		}, plugin, ProcessType.RESPONSE_DESIRED_SERVICES, httpMessage);
	}
	
	@Override
	String getText4Logging(LogText type) {
		String superStr = super.getText4Logging(type);
		if (superStr != null)
			return superStr;
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
	
	@Override
	ProcessType serviceProvidingType() {
		return ProcessType.RESPONSE_PROVIDE_SERVICE;
	}
	
	@Override
	ProcessType serviceCommitingType() {
		return ProcessType.RESPONSE_SERVICE_COMMIT;
	}
}