package sk.fiit.peweproxy.headers;

/**
 * Base interface for read-only HTTP response header.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseHeader extends ReadableHeader {
	
	/**
	 * Returns the status line value of this response message header.
	 * @return status line value of this response header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1">RFC 2616: 6.1 Status-Line</a>
	 */
	public String getStatusLine();
	
	/**
	 * Returns the status code value of this response message header.
	 * @return status code of this response header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">RFC 2616: 6.1.1 Status Code and Reason Phrase</a>
	 */
	public String getStatusCode() ;
	
	/**
	 * Returns the reason phrase value of this response message header.
	 * @return reason phrase of this response header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">RFC 2616: 6.1.1 Status Code and Reason Phrase</a>
	 */
	public String getReasonPhrase();
}
