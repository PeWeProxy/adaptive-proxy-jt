package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.ResponseHeader;

/**
 * Representation of read-only HTTP reponse message.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpResponse extends HttpMessage {
	
	/**
	 * Returns a request message that resulted in HTTP response represented by
	 * this response message.
	 * @return request message this message is a response for
	 */
	HttpRequest getRequest();

	/**
	 * Returns read-only representation of HTTP response header of this request
	 * message.
	 * @return read-only HTTP response header
	 */
	ResponseHeader getResponseHeader();
	
	/**
	 * Returns original HTTP response as sent by the web resource.
	 * @return original HTTP response
	 */
	HttpResponse getOriginalResponse();
	
	public HttpResponse clone();
}
