package sk.fiit.rabbit.adaptiveproxy.headers;

/**
 * Base interface for modifiable HTTP request header.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface WritableRequestHeader extends RequestHeader, WritableHeader {

	/**
	 * Sets the request line value of this request message header.
	 * @param line new request line for this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1">RFC 2616: 5.1 Request-Line</a>
	 */
	public void setRequestLine(String line);
	
	/**
	 * Sets the value of the method of this request message header.
	 * @param method new method for this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.1">RFC 2616: 5.1.1 Method</a>
	 */
	public void setMehtod(String method);
	
	/**
	 * Sets the value of the request URI of this request message header.
	 * @param requestURI new request URI for this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2">RFC 2616: 5.1.2 Request-URI</a>
	 */
	public void setRequestURI(String requestURI);
	
	/**
	 * Sets the value of the "Host" header field of this request message header.
	 * @param host new "Host" field value for this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.23">RFC 2616: 14.23 Host</a>
	 */
	//public void setHost(String host);
	
	/**
	 * Sets the the request line and host header fields of this request message header
	 * to correctly point to resource on host referenced by <code>hostURI</code> under
	 * relative path <code>resourcePath</code>.
	 * @param destination new absolute URI address of the requested resource 
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2">RFC 2616: 5.1.2 Request-URI</a>
	 */
	public void setDestination(String destination);
}
