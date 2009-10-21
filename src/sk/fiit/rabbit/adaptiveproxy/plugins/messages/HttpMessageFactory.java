package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;

public interface HttpMessageFactory {
	ModifiableHttpRequest constructHttpRequest(HttpRequest request, RequestHeaders fromHeaders, boolean withContent);
	
	ModifiableHttpResponse constructHttpResponse(boolean withContent);
	
	ModifiableHttpResponse constructHttpResponse(HttpResponse response, boolean withContent);
}
