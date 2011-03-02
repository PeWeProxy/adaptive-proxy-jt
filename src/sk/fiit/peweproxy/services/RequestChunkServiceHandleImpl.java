package sk.fiit.peweproxy.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpRequestImpl;
import sk.fiit.peweproxy.plugins.services.ChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.RequestChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.RequestChunksServiceModule;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;
import sk.fiit.peweproxy.utils.Statistics.ProcessType;

public class RequestChunkServiceHandleImpl extends ChunkServicesHandleImpl<RequestChunksServiceModule> {
	
	public RequestChunkServiceHandleImpl(HttpRequestImpl request, ModulesManager modulesManager,
			byte[] data) {
		super(request, modulesManager.getLoadedRequestChunksModules(), modulesManager, data);
	}
	
	@Override
	Set<Class<? extends ProxyService>> getProvidedSvcs(RequestChunksServiceModule module) {
		return manager.getProvidedRequestChunksServices(module);
	}
	
	@Override
	String getText4Logging(LogText type) {
		String superStr = super.getText4Logging(type);
		if (superStr != null)
			return superStr;
		if (type == LogText.CAPITAL)
			return "Request";
		if (type == LogText.SHORT)
			return "RQ";
		return "request";
	}
	
	@Override
	<Service extends ProxyService> ServiceProvider<Service> callProvideService(RequestChunksServiceModule module,
			Class<Service> serviceClass) {
		return module.provideRequestChunkService((HttpRequest)httpMessage, this, remainsStore, serviceClass);
	}
	
	@Override
	<Service extends ProxyService> void callDoChanges(ChunkServiceProvider<?, Service> svcProvider) {
		// doChanges can be called only on modifiable message, so it's OK to crash if the cast fails
		((RequestChunkServiceProvider<?,Service>) svcProvider).doChanges((HttpRequestImpl)httpMessage, this);
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