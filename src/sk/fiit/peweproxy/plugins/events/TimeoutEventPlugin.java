package sk.fiit.peweproxy.plugins.events;

import java.net.InetSocketAddress;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;

/**
 * Interface for proxy plugins interested in signals of timeouts of data transfers
 * through the connections between the proxy server and the client or source of resource
 * requested by client.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface TimeoutEventPlugin extends ProxyPlugin {
	/**
	 * Signals the plugin that a timeout occured while reading request header data from
	 * connection with the client.
	 * @param clientSocket socket representing connection's endpoint on the client's side 
	 */
	void requestReadTimeout(InetSocketAddress clientSocket);
	
	/**
	 * Signals the plugin that a timeout occured while reading request body data from
	 * connection with the client.
	 * @param request request message constructed from received request header
	 */
	void requestReadTimeout(HttpRequest request);
	
	/**
	 * Signals the plugin that a timeout occured while sending request data through
	 * connection with source of requested resource (web server / another proxy in chain).
	 * @param request request message for which deliver timed out
	 */
	void requestDeliveryTimeout(HttpRequest request);
	
	/**
	 * Signals the plugin that a timeout occured while reading response header data from
	 * connection with source of requested resource (web server / another proxy in chain).
	 * @param request request message for which response header reading timed out
	 */
	void responseReadTimeout(HttpRequest request);
	
	/**
	 * Signals the plugin that a timeout occured while reading response body data from
	 * connection with source of requested resource (web server / another proxy in chain).
	 * @param response response message constructed from received response header
	 */
	void responseReadTimeout(HttpResponse response);
	
	/**
	 * Signals the plugin that a timeout occured while sending response data through
	 * connection with the client.
	 * @param response response message for which deliver timed out
	 */
	void responseDeliveryTimeout(HttpResponse response);
}
