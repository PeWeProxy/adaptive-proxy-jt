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
	
	/**
	 * Returns whether initialization of service implementation this provider provides
	 * changes realization's inner model. This value is used to determine if this provider
	 * should be considered only one with up-to-date inner model after it's creation.
	 * @return <code>true</code> if creation of this provider's service implementation
	 * changed it's inner model, <code>false</code> otherwise 
	 */
	boolean initChangedModel();
}
