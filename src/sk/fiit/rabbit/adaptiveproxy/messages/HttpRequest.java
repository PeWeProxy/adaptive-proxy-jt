package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeaders;

public interface HttpRequest extends HttpMessage {
	RequestHeaders getClientRequestHeaders();
	
	RequestHeaders getProxyRequestHeaders();
	
	InetSocketAddress getClientSocketAddress();
}
