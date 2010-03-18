package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeaders;

public interface HttpMessageFactory {
	ModifiableHttpRequest constructHttpRequest(HttpRequest request, RequestHeaders fromHeaders, String contentType);
	
	ModifiableHttpResponse constructHttpResponse(String contentType);
	
	ModifiableHttpResponse constructHttpResponse(HttpResponse response, String contentType);
}
