package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.WritableResponseHeaders;

public interface ModifiableHttpResponse extends HttpResponse {
	WritableResponseHeaders getProxyResponseHeaders();
}
