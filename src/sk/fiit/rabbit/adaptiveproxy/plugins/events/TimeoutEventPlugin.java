package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;

public interface TimeoutEventPlugin extends ProxyPlugin {
	void requestReadTimeout(InetSocketAddress clientSocket);
	
	void requestReadTimeout(HttpRequest request);
	
	void requestDeliveryTimeout(HttpRequest request);
	
	void responseReadTimeout(HttpRequest request);
	
	void responseReadTimeout(HttpResponse response);
	
	void responseDeliveryTimeout(HttpResponse response);
}
