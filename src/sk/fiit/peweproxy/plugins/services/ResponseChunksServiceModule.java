package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ResponseChunksPlugin;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

public interface ResponseChunksServiceModule extends ResponseChunksPlugin {
	
	void getProvidedResponseChunkServices(Set<Class<? extends ProxyService>> providedServices);
	
	<Service extends ProxyService> ResponseChunkServiceProvider<?, Service> provideResponseChunkService(HttpResponse response,
			ChunkServicesHandle chunkServicesHandle, Class<Service> serviceClass, boolean finalization) throws ServiceUnavailableException;
}
