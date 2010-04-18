package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

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
public interface RequestServiceModule extends ServiceModule, RequestPlugin {
	/**
	 * Returns set of service classes which this request service module is able to provide
	 * implementation for, depending on particular request messages context. 
	 * @return set of service classes this request service module provides implementation for
	 */
	Set<Class<? extends ProxyService>> getProvidedRequestServices();
	
	/**
	 * Returns service provider that provide implementation of requested service
	 * identified by passed <code>serviceClass</code> over passed request message.
	 * If this request service module is unable to provide the service over this
	 * request message, returns <code>null</code>.
	 * @param request request message to provide request service provider for
	 * @param serviceClass class of the service to provide implementation for
	 * @return service provider providing requested service over passed request
	 * or <code>null</code> if this service module is unable to provide the service
	 * for this request
	 */
	<Service extends ProxyService> ServiceProvider<Service> provideRequestService(HttpRequest request,
				Class<Service> serviceClass);
}
