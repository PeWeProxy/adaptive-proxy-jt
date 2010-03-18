package sk.fiit.rabbit.adaptiveproxy.plugins.processing;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;

public interface RequestProcessingPlugin extends RequestPlugin {
	public enum RequestProcessingActions {
		/**AdaptiveProxy should proceed with passed HttpRequest processing*/
		PROCEED,
		/**AdaptiveProxy should call getNewRequest() method to get substitutive HttpRequest
		 * on which it should do further request processing*/
		NEW_REQUEST,
		/**AdaptiveProxy should call getNewRequest() method to get substitutive HttpRequest
		 * which it should send without any changes*/
		FINAL_REQUEST,
		/**AdaptiveProxy should call getResponse() method to get immediate HttpResponse
		 * on which it should do further response processing*/
		NEW_RESPONSE,
		/**AdaptiveProxy should call getResponse() method to get immediate HttpResponse
		 * which it should send without any changes*/
		FINAL_RESPONSE,
	}
	
	RequestProcessingActions processRequest(ModifiableHttpRequest request);
	
	HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest, HttpMessageFactory messageFactory);
	
	HttpResponse getResponse(ModifiableHttpRequest proxyRequest, HttpMessageFactory messageFactory);
}
