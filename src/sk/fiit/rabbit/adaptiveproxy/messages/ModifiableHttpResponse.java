package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.WritableResponseHeaders;

/**
 * Representation of modifiable HTTP response message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ModifiableHttpResponse extends HttpResponse {
	/**
	 * Returns read-only HTTP response header that will be (or was) sent by proxy
	 * server back to the client.
	 * @return sending HTTP response header
	 */
	WritableResponseHeaders getProxyResponseHeaders();
}
