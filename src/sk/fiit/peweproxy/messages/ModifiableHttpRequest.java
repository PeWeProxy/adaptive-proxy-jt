package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.WritableRequestHeader;

/**
 * Representation of modifiable HTTP request message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ModifiableHttpRequest extends HttpRequest {
	/**
	 * Returns modifiable representation of HTTP request header of this request
	 * message.
	 * @return modifiable HTTP request header
	 */
	WritableRequestHeader getRequestHeader();
	
	public ModifiableHttpRequest clone();
}
