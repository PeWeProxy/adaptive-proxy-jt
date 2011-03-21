package sk.fiit.peweproxy.plugins.processing;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;

/**
 * Interface for proxy plugins involved in processing of HTTP requests.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestProcessingPlugin extends ProxyPlugin {
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
	 * Called by platform to get services (their classes) over request message that this request
	 * processing plugin wishes to be able to get and use later during request processing. This set
	 * is used to decide whether there is a need for the basic data services and the proxy server
	 * should precache all request body data before processing phase. For convenience, an empty set
	 * is passed so that a plugin only needs to fill it with desired services (their interfaces).
	 * <br><br>
	 * Returning {@link RequestProcessingActions#PROCEED} signals AdaptiveProxy platform that the
	 * plugin has decided on services and the platform should continue in asking other plugins
	 * for desired services. Returning other value signals that the plugin already knows (based
	 * on data available so far), that it will replace original request for new one or construct
	 * response regardless of what the original request's body would be. In that case there's no
	 * need to get delayed by precaching original request's body data, plugin is asked to provide
	 * substitutive request / constructed response which is further processed according to
	 * returned {@link RequestProcessingActions}.
	 * @param desiredServices set to be filled with classes of desired services
	 * @param request read-only request message
	 * @return signal for further processing of request. Returning
	 * {@link RequestProcessingActions#PROCEED} means plugin has decided on desired services and
	 * plan to use those on original request during processing phase. Returning other value means
	 * the plugin knows in advance that it will interrupt processing of original request.
	 */
	RequestProcessingActions desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices,
			HttpRequest request);
	
	/**
	 * Processes passed <code>request</code> by this request processing plugin. In this method
	 * a request processing plugin performs it's real-time message processing. Returning
	 * action type signals proxy server how to continue in further request processing. 
	 * @param request full-access request message to process
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
	
	/**
	 * Processes passed read-only <code>request</code>, that already was or is going to be
	 * sent by the proxy. In this method a request processing plugin  performs it's late
	 * message processing.
	 * @param request read-only request message to process
	 */
	void processTransferedRequest(HttpRequest request);
}
