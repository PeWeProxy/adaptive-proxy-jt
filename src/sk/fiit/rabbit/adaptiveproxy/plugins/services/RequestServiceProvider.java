package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.services.ServicesHandle;

/**
 * Request service provider is a service provider that provides particular service
 * implementation over particular HTTP request message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestServiceProvider extends ServiceProvider {
	/**
	 * Sets the full context of a request message represented by representation of
	 * modifiable HTTP request message. Service handle associated with passed
	 * modifiable request is also able to provide content services over request
	 * message that can edit request body data, if the message has any body data.
	 * @param request modifiable request message
	 * @see ModifiableHttpRequest#getServiceHandle()
	 * @see ServicesHandle
	 */
	void setRequestContext(ModifiableHttpRequest request);
}
