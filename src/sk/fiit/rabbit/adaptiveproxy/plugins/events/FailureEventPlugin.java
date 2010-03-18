package sk.fiit.rabbit.adaptiveproxy.plugins.events;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;

/**
 * Interface for proxy plugins interested in signals of failures of data transfers
 * through the connections between the proxy server and the client or source of resource
 * requested by client.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface FailureEventPlugin extends ProxyPlugin {
	/**
	 * Signals the plugin that an error occured while reading request header data from
	 * connection with the client.
	 * @param clientSocket socket representing connection's endpoint on the client's side 
	 */
	void requestReadFailed(InetSocketAddress clientSocket);
	
	/**
	 * Signals the plugin that an error occured while reading request body data from
	 * connection with the client.
	 * @param request request message constructed from received request header
	 */
	void requestReadFailed(HttpRequest request);
	
	/**
	 * Signals the plugin that an error occured while sending request data through
	 * connection with source of requested resource (web server / another proxy in chain).
	 * @param request request message for which delivery failed
	 */
	void requestDeliveryFailed(HttpRequest request);
	
	/**
	 * Signals the plugin that an error occured while reading response header data from
	 * connection with web source of requested resource (web server / another proxy in
	 * chain).
	 * @param request request message for which response header could not be read
	 */
	void responseReadFailed(HttpRequest request);
	
	/**
	 * Signals the plugin that an error occured while reading response body data from
	 * connection with source of requested resource (web server / another proxy in chain).
	 * @param response response message constructed from received response header
	 */
	void responseReadFailed(HttpResponse response);
	
	/**
	 * Signals the plugin that an error occured while sending response data through
	 * connection with the client.
	 * @param response response message for which delivery failed
	 */
	void responseDeliveryFailed(HttpResponse response);
}
