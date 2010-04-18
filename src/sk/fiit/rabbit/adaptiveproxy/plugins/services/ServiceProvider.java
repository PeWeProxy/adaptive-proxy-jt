package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

/**
 * Base interface for all service providers. Service provider is an entity that provides
 * particular service implementation over particular HTTP message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ServiceProvider<Service extends ProxyService> {
	/**
	 * Returns implementation of the service this service provider provides.
	 * @return implementation of provided service
	 */
	Service getService();
	
	boolean initChangedModel();
	
	/**
	 * Returns runtime class of service implementation this service provider provides.
	 * @return class of provided service implementation
	 */
	//Class<? extends ProxyService> getServiceClass();
}
