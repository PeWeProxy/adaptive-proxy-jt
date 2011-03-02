package sk.fiit.peweproxy.plugins.services;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServicesHandle;

public interface ResponseChunkServiceProvider<Type, Service extends ProxyService>
	extends ChunkServiceProvider<Type, Service> {
	
	void doChanges(HttpResponse response, ServicesHandle chunkServicesHandle);
}
