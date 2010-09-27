package sk.fiit.peweproxy.plugins.events;

import java.net.InetSocketAddress;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.plugins.ProxyPlugin;

/**
 * Interface for proxy plugins interested in signals of creation and termination of
 * connections between the proxy server and the client.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ConnectionEventPlugin extends ProxyPlugin {
	/**
	 * Signals the plugin that the connection between the client and proxy server was
	 * established.
	 * @param clientSocket socket representing connection's endpoint on the client's side 
	 */
	void clientMadeConnection(InetSocketAddress clientSocket);
	/**
	 * Signals the plugin that the client closed his endpoint of the connection with
	 * proxy server.
	 * @param clientSocket socket representing connection's endpoint on the client's side 
	 */
	void clientClosedConnection(InetSocketAddress clientSocket);
	
	/**
	 * Signals the plugin that the proxy server closed its endpoint of the connection
	 * with client.
	 * @param clientSocket socket representing connection's endpoint on the client's side 
	 */
	void proxyClosedConnection(InetSocketAddress clientSocket);
	
	/**
	 * Signals the plugin that the proxy server closed its endpoint of the connection
	 * with client after receiving <code>request</code>.
	 * @param request request message received via the closing connection
	 */
	void proxyClosedConnection(HttpRequest request);
}
