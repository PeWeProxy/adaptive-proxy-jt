package sk.fiit.peweproxy.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.HttpResponseImpl;
import sk.fiit.peweproxy.plugins.services.ChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseChunksServiceModule;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;
import sk.fiit.peweproxy.utils.Statistics.ProcessType;

public class ResponseChunkServiceHandleImpl extends ChunkServicesHandleImpl<ResponseChunksServiceModule> {
	
	public ResponseChunkServiceHandleImpl(HttpResponseImpl response, ModulesManager modulesManager,
			byte[] data) {
		super(response, modulesManager.getLoadedResponseChunksModules(), modulesManager, data);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedSvcs(ResponseChunksServiceModule module) {
		return manager.getProvidedResponseChunksServices(module);
	}
	
	@Override
	void discoverDesiredServices(final ResponseChunksServiceModule plugin,
			final Set<Class<? extends ProxyService>> desiredServices) throws Throwable {
		manager.getAdaptiveEngine().getStatistics().executeProcess(new Runnable() {
			@Override
			public void run() {
				plugin.desiredResponseChunkServices(desiredServices,httpMessage.getHeader());
			}
		}, plugin, ProcessType.RESPONSE_DESIRED_SERVICES, httpMessage);
	}
	
	@Override
	String getText4Logging(LogText type) {
		String superStr = super.getText4Logging(type);
		if (superStr != null)
			return superStr;
		if (type == LogText.MSG_CAPITAL)
			return "Response";
		if (type == LogText.MSG_SHORT)
			return "RP";
		if (type == LogText.SVC_CAPITAL)
			return "ResponseChunk";
		return "response";
	}
	
	@Override
	<Service extends ProxyService> ServiceProvider<Service> callProvideService(ResponseChunksServiceModule module,
			Class<Service> serviceClass) {
		return module.provideResponseChunkService((HttpResponse)httpMessage, this, serviceClass, finalization);
	}
	
	@Override
	<Service extends ProxyService> void callDoChanges(ChunkServiceProvider<?, Service> svcProvider) {
		// doChanges can be called only on modifiable message, so it's OK to crash if the cast fails
		((ResponseChunkServiceProvider<?,Service>) svcProvider).doChanges((HttpResponseImpl)httpMessage, this);
	}
	
	@Override
	ProcessType serviceProvidingType() {
		return ProcessType.REQUEST_PROVIDE_SERVICE;
	}
	
	@Override
	ProcessType serviceCommitingType() {
		return ProcessType.REQUEST_SERVICE_COMMIT;
	}
}