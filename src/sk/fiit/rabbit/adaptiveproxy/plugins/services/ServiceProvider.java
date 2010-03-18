package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

/**
 * Base interface for all service providers. Service provider is an entity that provides
 * particular service implementation over particular HTTP message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ServiceProvider {
	/**
	 * Returns implementation of the service this service provider provides.
	 * @return implementation of provided service
	 */
	ProxyService getService();
	
	/**
	 * Returns runtime class of service implementation this service provider provides.
	 * @return class of provided service implementation
	 */
	Class<? extends ProxyService> getServiceClass();
	
	/**
	 * Signals this service provider to apply all changes that were made by using
	 * provided service implementation. In this method, the service provider should
	 * introduce all changes made to the inner model of provided service implementation
	 * since the last call of this method to the actual HTTP message.
	 */
	void doChanges();
}
