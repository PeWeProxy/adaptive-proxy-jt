package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.ResponsePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;

public interface FailureEventPlugin extends RequestPlugin, ResponsePlugin {
	void requestReadFailed(HttpRequest request);
	
	void requestReadFailed(InetSocketAddress clientSocket);
	
	void requestDeliveryFailed(HttpRequest request);
	
	void responseReadFailed(HttpRequest request);
	
	void responseReadFailed(HttpRequest request, HttpResponse response);
	
	void responseDeliveryFailed(HttpRequest request, HttpResponse response);
}
