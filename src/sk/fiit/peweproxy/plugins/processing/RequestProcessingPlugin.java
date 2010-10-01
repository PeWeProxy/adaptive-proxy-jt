package sk.fiit.peweproxy.plugins.processing;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.RequestPlugin;

/**
 * Interface for proxy plugins involved in processing of HTTP requests.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestProcessingPlugin extends RequestPlugin {
	/**
	 * Values of this enum are used to signal proxy server what actions should it take
	 * in further processing of a HTTP request message.
	 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
	 *
	 */
	public enum RequestProcessingActions {
		/**AdaptiveProxy should proceed with passed HttpRequest processing*/
		PROCEED,
		/**AdaptiveProxy should call getNewRequest() method to get substitutive HttpRequest
		 * on which it should do further request processing*/
		NEW_REQUEST,
		/**AdaptiveProxy should call getNewRequest() method to get substitutive HttpRequest
		 * which it should send without further request processing*/
		FINAL_REQUEST,
		/**AdaptiveProxy should call getResponse() method to get immediate HttpResponse
		 * on which it should do further response processing*/
		NEW_RESPONSE,
		/**AdaptiveProxy should call getResponse() method to get immediate HttpResponse
		 * which it should send without further response processing*/
		FINAL_RESPONSE,
	}
	
	/**
	 * Process passed <code>request</code> by this request processing plugin. Returning
	 * action type signals proxy server how to continue in further request processing. 
	 * @param request request message to process
	 * @return signal for further processing of a request
	 */
	RequestProcessingActions processRequest(ModifiableHttpRequest request);
	
	/**
	 * Returns substitutive request message for which currently processed request should
	 * be replaced. In case of signaling <code>FINAL_REQUEST</code> without need to
	 * substitute current request message for new one, plugins are expected to return
	 * <code>request</code> object.
	 * @param request request message currently processed
	 * @param messageFactory a HTTP message factory for new messages creation
	 * @return substitutive request message for further processing
	 */
	HttpRequest getNewRequest(ModifiableHttpRequest request, HttpMessageFactory messageFactory);
	
	/**
	 * Returns created response message for further response processing.
	 * @param request request message currently processed
	 * @param messageFactory a HTTP message factory for new messages creation
	 * @return created response message for further processing
	 */
	HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory);
}
