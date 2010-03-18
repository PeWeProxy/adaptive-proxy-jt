package sk.fiit.rabbit.adaptiveproxy.services;

import java.util.List;
import java.util.Set;

/**
 * Service handle is an entity that provides implementations of services available for
 * particular HTTP message, which are obtained from service plugins.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ServicesHandle {
	/**
	 * Returns set of service classes implementations of which are available for use.
	 * @return set of available service classes
	 */
	public Set<Class<? extends ProxyService>> getAvailableServices();
	
	/**
	 * Returns an implementation of requested service designated by passed service
	 * class.
	 * @param <T> particular service
	 * @param serviceClass class of the requested service
	 * @return implementation of the requested service, if one is available
	 * @throws ServiceUnavailableException if no implementation is available
	 */
	<T extends ProxyService> T getService(Class<T> serviceClass) throws ServiceUnavailableException;
	
	/**
	 * Returns list of all available implementation of requested service designated
	 * by passed service class.
	 * @param <T> particular service
	 * @param serviceClass class of the requested service
	 * @return implementation of the requested service, if one is available
	 * @throws ServiceUnavailableException if no implementation is available
	 */
	<T extends ProxyService> List<T> getServices(Class<T> serviceClass) throws ServiceUnavailableException;
}
