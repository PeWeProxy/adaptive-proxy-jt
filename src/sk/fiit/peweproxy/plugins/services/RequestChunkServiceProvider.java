package sk.fiit.peweproxy.plugins.services;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServicesHandle;

/**
 * Request chunk service provider is a chunk service provider that provides particular
 * service implementation over chunk of body data of particular HTTP request message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 * 
 */
public interface RequestChunkServiceProvider<Type, Service extends ProxyService>
	extends ChunkServiceProvider<Type, Service> {
	
	/**
	 * Signals this request chunk service provider to apply all changes that were made by
	 * using provided service implementation. In this method, the chunks service provider
	 * should introduce all changes made to the inner model of provided service implementation
	 * to the current chunk of the HTTP request message body via using other services provided
	 * by <code>chunkServicesHandle</code>.
	 * @param request read-only request message containing already sent data
	 * @param chunkServicesHandle chunk services handle for full access to the chunk data
	 */
	void doChanges(HttpRequest request, ServicesHandle chunkServicesHandle);
}
