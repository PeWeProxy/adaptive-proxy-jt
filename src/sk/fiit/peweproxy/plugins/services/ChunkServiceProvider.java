package sk.fiit.peweproxy.plugins.services;

import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;

public interface ChunkServiceProvider<Type, Service extends ProxyService> extends ServiceProvider<Service> {
	void ceaseContent(Type chunkPart, ChunkServicesHandle chunkServiceshandle);
}
