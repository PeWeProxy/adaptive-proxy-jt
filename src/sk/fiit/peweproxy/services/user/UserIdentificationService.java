package sk.fiit.peweproxy.services.user;

import sk.fiit.peweproxy.services.ProxyService;

/**
 * User identification service is able to provide unique textual identifier of the
 * user that initiated the HTTP message. For a request message, provided ID string identifies
 * the user that made the request. For a response message it identifies the user that made the
 * request that resulted in actual response. 
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface UserIdentificationService extends ProxyService {
	
	/**
	 * Returns the unique identifier of the user that initiated the message. Returned value
	 * must not be <code>null</code>.
	 * @return identifier of the user that initiated the message
	 */
	String getUserIdentification();
}
