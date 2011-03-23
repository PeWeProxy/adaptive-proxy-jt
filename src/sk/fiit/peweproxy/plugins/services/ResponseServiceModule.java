package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

/**
 * Interface for response service modules. Response service module is a service module
 * that provides implementations of services over particular HTTP responses.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseServiceModule extends ProxyPlugin {
	/**
	 * Called by platform to get services (their classes) over response message that this response
	 * service module wishes to be able to get and use later during message processing. This set is
	 * used to decide whether there is a need for the basic data services and the proxy server
	 * should precache all response body data before processing phase. For convenience, an empty set
	 * is passed so that a plugin only needs to fill it with desired services (their interfaces).
	 * @param desiredServices set to be filled with classes of desired services
	 * @param webRPHeader read-only response header
	 */
	void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader);
	
	/**
	 * Called by platform to get services (their classes) which this response service module is
	 * able to provide implementation for, depending on particular response messages context. For
	 * convenience, an empty set is passed so that a module only needs to fill it with services
	 * (their interfaces) it provides.
	 * @param providedServices set to be filled with classes of services this response service
	 * module provides implementation for
	 */
	void getProvidedResponseServices(Set<Class<? extends ProxyService>> providedServices);
	
	/**
	 * Returns response service provider that provides implementation of requested service
	 * identified by passed <code>serviceClass</code> over passed response message.
	 * If this response service module is unable to provide the service over this
	 * response message, returns <code>null</code>.
	 * @param response response message to provide response service provider for
	 * @param serviceClass class of the service to provide implementation for
	 * @return response service provider providing requested service over passed response
	 * or <code>null</code> if this service module is unable to provide the service
	 * for this response
	 */
	<Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(HttpResponse response,
				Class<Service> serviceClass) throws ServiceUnavailableException;
}
