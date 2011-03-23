package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;

/**
 * Interface for request service modules. Request service module is a service module
 * that provides implementations of services over particular HTTP requests.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestServiceModule extends ProxyPlugin {
	/**
	 * Called by platform to get services (their classes) over request message that this request
	 * service module wishes to be able to get and use later during message processing. This set is
	 * used to decide whether there is a need for the basic data services and the proxy server
	 * should precache all request body data before processing phase. For convenience, an empty set
	 * is passed so that a plugin only needs to fill it with desired services (their interfaces).
	 * @param desiredServices set to be filled with classes of desired services
	 * @param clientRQHeader read-only request header
	 */
	void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader);
	
	/**
	 * Called by platform to get services (their classes) which this request service module is
	 * able to provide implementation for, depending on particular request messages context. For
	 * convenience, an empty set is passed so that a module only needs to fill it with services
	 * (their interfaces) it provides.
	 * @param providedServices set to be filled with classes of services this request service
	 * module provides implementation for
	 */
	void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices);
	
	/**
	 * Returns request service provider that provides implementation of requested service
	 * identified by passed <code>serviceClass</code> over passed request message.
	 * If this request service module is unable to provide the service over this
	 * request message, returns <code>null</code>.
	 * @param request request message to provide request service provider for
	 * @param serviceClass class of the service to provide implementation for
	 * @return request service provider providing requested service over passed request
	 * or <code>null</code> if this service module is unable to provide the service
	 * for this request
	 */
	<Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(HttpRequest request,
				Class<Service> serviceClass) throws ServiceUnavailableException;
}
