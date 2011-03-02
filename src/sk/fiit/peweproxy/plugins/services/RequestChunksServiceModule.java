package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ChunkRemains;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

public interface RequestChunksServiceModule extends ProxyPlugin {
	
	void getProvidedRequestChunkServices(Set<Class<? extends ProxyService>> providedServices);
	
	<Service extends ProxyService> RequestChunkServiceProvider<?, Service> provideRequestChunkService(HttpRequest request,
			ChunkServicesHandle chunkServicesHandle, ChunkRemains remainsStore, Class<Service> serviceClass) throws ServiceUnavailableException;
}
