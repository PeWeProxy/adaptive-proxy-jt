package sk.fiit.peweproxy.headers;

/**
 * Representation of modifiable HTTP response header.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface WritableResponseHeader extends ResponseHeader, WritableHeader {
	
	/**
	 * Sets the status line value of this response message header.
	 * @param line new status line value for this response header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1">RFC 2616: 6.1 Status-Line</a>
	 */
	public void setStatusLine(String line);
	
	/**
	 * Sets the status code value of this response message header.
	 * @param status new status code for this response header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">RFC 2616: 6.1.1 Status Code and Reason Phrase</a>
	 */
	public void setStatusCode(String status);
	
	/**
	 * Sets the reason phrase value of this response message header.
	 * @param reason new reason phrase for this response header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">RFC 2616: 6.1.1 Status Code and Reason Phrase</a>
	 */
	public void setReasonPhrase(String reason);
}
