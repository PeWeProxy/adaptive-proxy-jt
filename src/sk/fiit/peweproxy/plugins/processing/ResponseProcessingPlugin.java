package sk.fiit.peweproxy.plugins.processing;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.FullResponsePlugin;

/**
 * Interface for proxy plugins involved in processing of HTTP responses.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseProcessingPlugin extends FullResponsePlugin {
	/**
	 * Values of this enum are used to signal proxy server what actions should it take
	 * in further processing of a HTTP response message.
	 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
	 *
	 */
	public enum ResponseProcessingActions {
		/**AdaptiveProxy should proceed with passed HttpResponse processing*/
		PROCEED,
		/**AdaptiveProxy should call getNewResponse() method to get substitutive HttpResponse
		 * on which it should do further response processing*/
		NEW_RESPONSE,
		/**AdaptiveProxy should call getNewResponse() method to get substitutive HttpResponse
		 * which it should send without further response processing*/
		FINAL_RESPONSE,
	}
	
	/**
	 * Process passed <code>response</code> by this response processing plugin. In this method
	 * a response processing plugin performs it's real-time message processing. Returning
	 * action type signals proxy server how to continue in further response processing. 
	 * @param response response message to process
	 * @return signal for further processing of a response
	 */
	ResponseProcessingActions processResponse(ModifiableHttpResponse response);
	
	/**
	 * Returns substitutive response message for which currently processed response should
	 * be replaced. In case of signaling <code>FINAL_RESPONSE</code> without need to
	 * substitute current response message for new one, plugins are expected to return
	 * <code>response</code> object.
	 * @param response response message currently processed
	 * @param messageFactory a HTTP message factory for new messages creation
	 * @return substitutive response message for further processing
	 */
	HttpResponse getNewResponse(ModifiableHttpResponse response, HttpMessageFactory messageFactory);
	
	/**
	 * Processes passed read-only <code>response</code>, that already was or is going to be
	 * sent by the proxy. In this method a response processing plugin  performs it's late
	 * message processing.
	 * @param response read-only response message to process
	 */
	void processTransferedResponse(HttpResponse response);
}
