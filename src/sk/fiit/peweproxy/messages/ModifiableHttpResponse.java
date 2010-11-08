package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.WritableResponseHeader;

/**
 * Representation of modifiable HTTP response message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ModifiableHttpResponse extends HttpResponse {
	/**
	 * Returns modifiable representation of HTTP response header of this response
	 * message.
	 * @return modifiable HTTP response header
	 */
	WritableResponseHeader getResponseHeader();
	
	public ModifiableHttpResponse clone();
}
