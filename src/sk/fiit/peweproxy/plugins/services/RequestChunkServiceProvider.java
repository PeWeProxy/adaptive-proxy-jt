package sk.fiit.peweproxy.plugins.services;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServicesHandle;

public interface RequestChunkServiceProvider<Type, Service extends ProxyService>
	extends ChunkServiceProvider<Type, Service> {
	
	void doChanges(HttpRequest request, ServicesHandle chunkServicesHandle);
}
