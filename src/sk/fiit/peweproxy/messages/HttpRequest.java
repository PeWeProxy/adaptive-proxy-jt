package sk.fiit.peweproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.peweproxy.headers.RequestHeader;

/**
 * Representation of read-only HTTP requests message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpRequest extends HttpMessage {
	/**
	 * Returns read-only HTTP request header received from the client.
	 * @return original HTTP request header received from the client
	 */
	RequestHeader getClientRequestHeader();
	
	/**
	 * Returns read-only HTTP request header that will be (or was) sent by proxy
	 * server to the web source.
	 * @return sending HTTP request header
	 */
	RequestHeader getProxyRequestHeader();
	
	/**
	 * Returns client's endpoint info (socket address) of connection through which
	 * this request was received.
	 * @return client's connection endpoint socket address
	 */
	InetSocketAddress getClientSocketAddress();
	
	public HttpRequest clone();
}
