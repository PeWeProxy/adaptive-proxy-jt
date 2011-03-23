package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ResponseChunksPlugin;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

/**
 * Interface for response chunk service modules. Response chunk service module is a service
 * module that provides implementations of services over chunk of body data of particular
 * HTTP response message.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseChunksServiceModule extends ResponseChunksPlugin {
	
	/**
	 * Called by platform to get services (their classes) which this response chunk service
	 * module is able to provide implementation for, depending on particular context of the
	 * chunk of the response messages body. For convenience, an empty set is passed so that
	 * a module only needs to fill it with services (their interfaces) it provides.
	 * @param providedServices set to be filled with classes of services this response service
	 * module provides implementation for
	 */
	void getProvidedResponseChunkServices(Set<Class<? extends ProxyService>> providedServices);
	
	
	/**
	 * Returns response chunk service provider that provides implementation of requested
	 * service identified by passed <code>serviceClass</code> over chunk of body data, 
	 * accessible through passed <code>chunkServicesHandle</code>, of response message
	 * identified by passed <code>response</code>.
	 * If this response chunk service module is unable to provide the service over this
	 * response message chunk, returns <code>null</code>.
	 * @param response read-only response message for chunk of which to provide response chunk
	 * service provider for 
	 * @param serviceClass class of the service to provide implementation for
	 * @return response chunk service provider providing requested service over response
	 * body chunk of interest or <code>null</code> if this service module is unable to
	 * provide the service for this response chunk
	 */
	<Service extends ProxyService> ResponseChunkServiceProvider<?, Service> provideResponseChunkService(HttpResponse response,
			ChunkServicesHandle chunkServicesHandle, Class<Service> serviceClass, boolean finalization) throws ServiceUnavailableException;
}
