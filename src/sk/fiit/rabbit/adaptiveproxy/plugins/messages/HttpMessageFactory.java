package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;

public interface HttpMessageFactory {
	ModifiableHttpRequest constructHttpRequest(HttpRequest request, RequestHeaders fromHeaders, String contentType);
	
	ModifiableHttpResponse constructHttpResponse(String contentType);
	
	ModifiableHttpResponse constructHttpResponse(HttpResponse response, String contentType);
}
