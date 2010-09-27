package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.services.ServicesHandle;

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
	
	/**
	 * Returns whether body data of this HTTP message are accessible. Returning <code>false
	 * </code> does not mean that this message does not carry any body data, just that the
	 * proxy server has not body data prefetched.
	 * @return
	 */
	boolean hasBody();
	
	/**
	 * Returns a copy of this HTTP message for which no thread access checking will be
	 * performed so that it can be used in another threads.
	 * <br><br><b><i>Warning !</i></b><br>
	 * Avoid constructing and returning a message clone when asked for request / response
	 * substitution in a processing plugin code. The message returned is going to be used
	 * in further processing and so it's thread access checking will be enabled again and
	 * accessing the clone from other threads will therefore be disabled.
	 * @return copy of this message with thread access checking off
	 */
	public HttpMessage clone();
}
