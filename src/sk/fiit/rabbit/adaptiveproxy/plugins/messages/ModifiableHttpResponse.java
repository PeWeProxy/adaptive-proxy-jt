package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.WritableResponseHeaders;

public interface ModifiableHttpResponse extends HttpResponse {
	WritableResponseHeaders getProxyResponseHeaders();
}
