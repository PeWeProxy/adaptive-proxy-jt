package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.ResponsePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;

public interface TimeoutEventPlugin extends RequestPlugin, ResponsePlugin{
	void requestReadTimeout(InetSocketAddress clientSocket);
	
	void requestReadTimeout(HttpRequest request);
	
	void requestDeliveryTimeout(HttpRequest request);
	
	void responseReadTimeout(HttpRequest request);
	
	void responseReadTimeout(HttpRequest request, HttpResponse response);
	
	void responseDeliveryTimeout(HttpRequest request, HttpResponse response);
}
