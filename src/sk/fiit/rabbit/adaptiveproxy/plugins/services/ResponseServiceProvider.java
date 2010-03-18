package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.services.ServicesHandle;

/**
 * Response service provider is a service provider that provides particular service
 * implementation over particular HTTP response message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseServiceProvider extends ServiceProvider {
	/**
	 * Sets the full context of a response message represented by representation of
	 * modifiable HTTP response message. Service handle associated with passed
	 * modifiable response is also able to provide content services over response
	 * message that can edit response body data, if the message has any body data.
	 * @param response modifiable response message
	 * @see ModifiableHttpResponse#getServiceHandle()
	 * @see ServicesHandle
	 */
	void setResponseContext(ModifiableHttpResponse response);
}
