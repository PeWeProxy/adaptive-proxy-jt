package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ChunkRemains;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

public interface ResponseChunksServiceModule extends ProxyPlugin {
	
	void getProvidedResponseChunkServices(Set<Class<? extends ProxyService>> providedServices);
	
	<Service extends ProxyService> ResponseChunkServiceProvider<?, Service> provideResponseChunkService(HttpResponse response,
			ChunkServicesHandle chunkServicesHandle, ChunkRemains remainsStore, Class<Service> serviceClass) throws ServiceUnavailableException;
}
