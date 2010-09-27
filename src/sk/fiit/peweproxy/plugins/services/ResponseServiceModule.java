package sk.fiit.peweproxy.plugins.services;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ResponsePlugin;
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
public interface ResponseServiceModule extends ServiceModule, ResponsePlugin {
	/**
	 * Returns set of service classes which this response service module is able to provide
	 * implementation for, depending on particular response messages context. 
	 * @return set of service classes this response service module provides implementation for
	 */
	Set<Class<? extends ProxyService>> getProvidedResponseServices();
	
	/**
	 * Returns response service provider that provide implementation of requested service
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
