package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeaders;

/**
 * Representation of read-only HTTP reponse message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpResponse extends HttpRequest {

	/**
	 * Returns read-only HTTP response header received from the web source.
	 * @return original HTTP response header received from the web source
	 */
	ResponseHeaders getWebResponseHeaders();
	
	/**
	 * Returns read-only HTTP response header that will be (or was) sent by proxy
	 * server back to the client.
	 * @return sending HTTP response header
	 */
	ResponseHeaders getProxyResponseHeaders();
}
