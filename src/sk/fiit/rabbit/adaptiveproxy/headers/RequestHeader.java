package sk.fiit.rabbit.adaptiveproxy.headers;

/**
 * Base interface for read-only HTTP request header.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestHeader extends ReadableHeader {
	/**
	 * Returns the request line value of this request message header.
	 * @return request line of this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1">RFC 2616: 5.1 Request-Line</a>
	 */
	public String getRequestLine();
	
	/**
	 * Returns the value of the method of this request message header.
	 * @return method of this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.1">RFC 2616: 5.1.1 Method</a>
	 */
	public String getMethod();
	
	/**
	 * Returns the value of the request URI of this request message header.
	 * @return request URI text of this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2">RFC 2616: 5.1.2 Request-URI</a>
	 */
	public String getRequestURI();
	
	/**
	 * Returns the value of the "Host" header field of this request message header.
	 * @return value of the Host field of this header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.23">RFC 2616: 14.23 Host</a>
	 */
	//public String getHost();
	
	/**
	 * Returns whether this request header designates tunneling request.
	 * @return whether is this tunneling request 
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.9">RFC 2616: 9.9 CONNECT</a>
	 */
	//public boolean isTunneling();
	
	/**
	 * Returns absolute URI address of resource requested by this request message header.
	 * @return absolute URI address of requested resource
	 */
	public String getDestionation();
}
