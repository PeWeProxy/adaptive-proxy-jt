package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import java.net.InetSocketAddress;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;

public interface HttpRequest extends HttpMessage {
	RequestHeaders getClientRequestHeaders();
	
	RequestHeaders getProxyRequestHeaders();
	
	InetSocketAddress getClientSocketAddress();
}
