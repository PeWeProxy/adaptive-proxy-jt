package sk.fiit.peweproxy.services;

public interface ChunkServicesHandle extends ServicesHandle, ChunkRemains {
	<T, Service extends ProxyService> void ceaseContent(Service byService, T chunkPart);
}
