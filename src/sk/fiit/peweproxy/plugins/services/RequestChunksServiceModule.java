package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.plugins.RequestChunksPlugin;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

public interface RequestChunksServiceModule extends RequestChunksPlugin {
	
	void getProvidedRequestChunkServices(Set<Class<? extends ProxyService>> providedServices);
	
	<Service extends ProxyService> RequestChunkServiceProvider<?, Service> provideRequestChunkService(HttpRequest request,
			ChunkServicesHandle chunkServicesHandle, Class<Service> serviceClass, boolean finalization) throws ServiceUnavailableException;
}
