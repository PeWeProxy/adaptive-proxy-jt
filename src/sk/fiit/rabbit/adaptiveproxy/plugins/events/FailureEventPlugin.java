package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;

public interface FailureEventPlugin extends ProxyPlugin {
	void requestReadFailed(HttpRequest request);
	
	void requestReadFailed(InetSocketAddress clientSocket);
	
	void requestDeliveryFailed(HttpRequest request);
	
	void responseReadFailed(HttpRequest request);
	
	void responseReadFailed(HttpResponse response);
	
	void responseDeliveryFailed(HttpResponse response);
}
