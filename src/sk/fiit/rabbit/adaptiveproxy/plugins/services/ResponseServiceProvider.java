package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

/**
 * Response service provider is a service provider that provides particular service
 * implementation over particular HTTP response message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseServiceProvider<Service extends ProxyService> extends ServiceProvider<Service> {
	/**
	 * Signals this response service provider to apply all changes that were made by using
	 * provided service implementation. In this method, the service provider should
	 * introduce all changes made to the inner model of provided service implementation
	 * to the actual HTTP response message.
	 * @param response modifiable response message to introduce changes into
	 */
	void doChanges(ModifiableHttpResponse response);
}
