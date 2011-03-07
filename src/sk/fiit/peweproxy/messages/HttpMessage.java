package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.user.UserIdentificationService;

/**
 * Base interface for representations of all HTTP messages.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpMessage {
	
	/**
	 * Returns services handle which provides implementations of services over this HTTP
	 * message.
	 * @return services handle for this HTTP message
	 */
	ServicesHandle getServicesHandle();
	
	/**
	 * Returns whether body data of this HTTP message is accessible. Returning <code>false
	 * </code> does not mean that this message does not carry any body data, just that the
	 * proxy server has not body data prefetched.
	 * @return whether body data of this message is accessible
	 */
	boolean bodyAccessible();
	
	/**
	 * Returns <code>true</code> if all data of this HTTP message was already transfered through
	 * AdaptiveProxy, <code>false</code> otherwise.  
	 * @return whether all data of this HTTP message was already transfered via proxy
	 */
	boolean isComplete();
	
	/**
	 * Returns whether body data of this HTTP message was processed by chunk processing plugins.
	 * @return whether this message was chunk-processed
	 */
	boolean wasChunkProcessed();
	
	/**
	 * Returns processing storage that plugins may use to store message-specific data. Every HTTP message
	 * instance has it's own processing storage instance.
	 * @return processing storage for this message
	 */
	ProcessingStorage getStorage();
	
	/**
	 * Returns the unique textual identifier of the user that initiated this message. This
	 * identifier is obtained by using plugged in {@link UserIdentificationService}
	 * realizations. AdaptiveProxy platform calls all available realizations one by one until
	 * one is able to provide valid ID. Returned ID is then cached for later calls of this
	 * method. If there's no module to provide valid ID for this message, <code>null</code>
	 * is cached.
	 * @return identifier of the user that initiated the message, or <code>null</code> if no
	 * module is able to prvoide one
	 * @see UserIdentificationService
	 */
	String getUserIdentification();
	
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
