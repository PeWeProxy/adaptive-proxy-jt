package sk.fiit.rabbit.adaptiveproxy.plugins.processing;

import sk.fiit.rabbit.adaptiveproxy.plugins.ResponsePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;

public interface ResponseProcessingPlugin extends ResponsePlugin {
	public enum ResponseProcessingActions {
		/**AdaptiveProxy should proceed with passed HttpResponse processing*/
		PROCEED,
		/**AdaptiveProxy should call getNewResponse() method to get substitutive HttpResponse
		 * on which it should do further response processing*/
		NEW_RESPONSE,
		/**AdaptiveProxy should call getNewResponse() method to get substitutive HttpResponse
		 * which it should send without any changes*/
		FINAL_RESPONSE,
	}
	
	ResponseProcessingActions processResponse(ModifiableHttpResponse response);
	
	HttpResponse getNewResponse(ModifiableHttpResponse response, HttpMessageFactory messageFactory);
}
