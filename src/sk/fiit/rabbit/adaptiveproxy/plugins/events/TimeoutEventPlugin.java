package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;

public interface TimeoutEventPlugin extends ProxyPlugin {
	void requestReadTimeout(InetSocketAddress clientSocket);
	
	void requestReadTimeout(HttpRequest request);
	
	void requestDeliveryTimeout(HttpRequest request);
	
	void responseReadTimeout(HttpRequest request);
	
	void responseReadTimeout(HttpResponse response);
	
	void responseDeliveryTimeout(HttpResponse response);
}
