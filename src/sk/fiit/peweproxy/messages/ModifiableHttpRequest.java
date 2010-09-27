package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.WritableRequestHeader;

/**
 * Representation of modifiable HTTP request message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ModifiableHttpRequest extends HttpRequest {
	/**
	 * Returns modifiable HTTP request header that will be (or was) sent by proxy
	 * server to the web source.
	 * @return sending HTTP request header
	 */
	WritableRequestHeader getProxyRequestHeader();
	
	public ModifiableHttpRequest clone();
}
