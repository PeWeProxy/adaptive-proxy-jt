package sk.fiit.peweproxy.plugins.processing;

import java.util.Set;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;

/**
 * Interface for proxy plugins involved in processing of HTTP responses.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseProcessingPlugin extends ProxyPlugin {
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
	 * Called by platform to get services (their classes) over response message that this plugin
	 * wishes to be able to get and use later during response processing. This set is used to decide
	 * whether there is a need for the basic data services and the proxy server should precache all
	 * response body data before processing phase. For convenience, an empty set is passed so that a
	 * plugin only needs to fill it with desired services (their interfaces).
	 * <br><br>
	 * Returning {@link ResponseProcessingActions#PROCEED} signals AdaptiveProxy platform that the
	 * plugin has decided on services and the platform should continue in asking other plugins
	 * for desired services. Returning other value signals that the plugin already knows (based
	 * on data available so far), that it will replace original response for new one regardless
	 * of what the original response's body would be. In that case there's no need to get delayed
	 * by precaching original response's body data, plugin is asked to provide substitutive response
	 * which is further processed according to returned {@link ResponseProcessingActions}.
	 * @param desiredServices set to be filled with classes of desired services
	 * @param response read-only response message
	 * @return signal for further processing of response. Returning
	 * {@link ResponseProcessingActions#PROCEED} means plugin has decided on desired services and
	 * plan to use those on original response during processing phase. Returning other value means
	 * the plugin knows in advance that it will interrupt processing of original response.
	 */
	ResponseProcessingActions desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices,
			HttpResponse response);
	
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
