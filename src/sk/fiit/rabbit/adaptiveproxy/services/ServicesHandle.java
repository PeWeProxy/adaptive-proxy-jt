package sk.fiit.rabbit.adaptiveproxy.services;

/**
 * Service handle is an entity that provides implementations of services available for
 * particular HTTP message, which are obtained from service modules.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ServicesHandle {
	/**
	 * Returns set of service classes implementations of which are available for use.
	 * @return set of available service classes
	 */
	//public Set<Class<? extends ProxyService>> getAvailableServices();
	
	<Service extends ProxyService> boolean isServiceAvailable(Class<Service> serviceClass);
	
	/**
	 * Returns an implementation of requested service specified by passed service
	 * class if there is any provided, otherwise throws an service unavailable
	 * exception.
	 * @param <Service> particular service
	 * @param serviceClass class of the requested service
	 * @return implementation of the requested service, if one is available
	 * @throws ServiceUnavailableException if no such implementation is available
	 */
	<Service extends ProxyService> Service getService(Class<Service> serviceClass) throws ServiceUnavailableException;
	
	/**
	 * Returns another implementation of the same service as passed service
	 * if there is any provided, otherwise throws an service unavailable
	 * exception.
	 * @param <Service> particular service
	 * @param previuosService already provided service implementation
	 * @return another implementation of the same service, if one is available
	 * @throws ServiceUnavailableException if no such implementation is available
	 */
	<Service extends ProxyService> Service getNextService(Service previuosService) throws ServiceUnavailableException;
}
