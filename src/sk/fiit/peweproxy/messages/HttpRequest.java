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
	 * Returns read-only representation of HTTP request header of this request
	 * message.
	 * @return read-only HTTP request header
	 */
	RequestHeader getRequestHeader();
	
	/**
	 * Returns original HTTP request as sent by the client.
	 * @return original HTTP request
	 */
	HttpRequest getOriginalRequest();
	
	/**
	 * Returns client's endpoint info (socket address) of connection through which
	 * this request was received.
	 * @return client's connection endpoint socket address
	 */
	InetSocketAddress getClientSocketAddress();
	
	public HttpRequest clone();
}
