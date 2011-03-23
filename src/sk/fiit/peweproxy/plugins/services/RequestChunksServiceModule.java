package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.plugins.RequestChunksPlugin;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

/**
 * Interface for request chunk service modules. Request chunk service module is a service
 * module that provides implementations of services over chunk of body data of particular
 * HTTP request message.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestChunksServiceModule extends RequestChunksPlugin {
	
	/**
	 * Called by platform to get services (their classes) which this request chunk service
	 * module is able to provide implementation for, depending on particular context of the
	 * chunk of the request messages body. For convenience, an empty set is passed so that
	 * a module only needs to fill it with services (their interfaces) it provides.
	 * @param providedServices set to be filled with classes of services this request service
	 * module provides implementation for
	 */
	void getProvidedRequestChunkServices(Set<Class<? extends ProxyService>> providedServices);
	
	/**
	 * Returns request chunk service provider that provides implementation of requested
	 * service identified by passed <code>serviceClass</code> over chunk of body data, 
	 * accessible through passed <code>chunkServicesHandle</code>, of request message
	 * identified by passed <code>request</code>.
	 * If this request chunk service module is unable to provide the service over this
	 * request message chunk, returns <code>null</code>.
	 * @param request read-only request message for chunk of which to provide request chunk
	 * service provider for 
	 * @param serviceClass class of the service to provide implementation for
	 * @return request chunk service provider providing requested service over request
	 * body chunk of interest or <code>null</code> if this service module is unable to
	 * provide the service for this request chunk
	 */
	<Service extends ProxyService> RequestChunkServiceProvider<?, Service> provideRequestChunkService(HttpRequest request,
			ChunkServicesHandle chunkServicesHandle, Class<Service> serviceClass, boolean finalization) throws ServiceUnavailableException;
}
