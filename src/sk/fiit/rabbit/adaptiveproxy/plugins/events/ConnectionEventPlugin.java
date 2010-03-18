package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;

public interface ConnectionEventPlugin extends ProxyPlugin {
	
	void clientMadeConnection(InetSocketAddress clientSocket);
	
	void clientClosedConnection(InetSocketAddress clientSocket);
	
	void proxyClosedConnection(InetSocketAddress clientSocket);
	
	void proxyClosedConnection(HttpRequest request);
}