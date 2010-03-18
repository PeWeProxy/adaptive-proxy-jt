package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;

public interface ConnectionEventPlugin extends ProxyPlugin {
	
	void clientMadeConnection(InetSocketAddress clientSocket);
	
	void clientClosedConnection(InetSocketAddress clientSocket);
	
	void proxyClosedConnection(InetSocketAddress clientSocket);
	
	void proxyClosedConnection(HttpRequest request);
}