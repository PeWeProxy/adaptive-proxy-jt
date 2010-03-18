package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.services.ServicesHandle;

/**
 * Base interface for representations of all HTTP messages.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpMessage {
	
	/**
	 * Returns service handle which provides implementations of services over this HTTP
	 * message.
	 * @return service handle for this HTTP message
	 */
	ServicesHandle getServiceHandle();
}
