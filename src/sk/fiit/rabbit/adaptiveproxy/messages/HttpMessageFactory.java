package sk.fiit.rabbit.adaptiveproxy.messages;

/**
 * A HTTP message factory is an entity, that could serve proxy plugins as a builder
 * of HTTP messages that are valid according to standards defined in
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC2616</a>.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpMessageFactory {
	/**
	 * Returns constructed HTTP request based on copy of passed HTTP request message
	 * <code>baseRequest</code> (if not <code>null</code>). If <code>contentType</code>
	 * is not <code>null</code>, constructed request will be able to hold body data 
	 * (initialized to empty byte array) and <i>Content-Type</i> header field will be
	 * set this value.
	 * @param baseRequest HTTP request message to base constructed message on, may be null
	 * @param contentType <i>Content-Type</i> header field value if request with body wanted,
	 * may be null
	 * @return constructed HTTP request message
	 */
	ModifiableHttpRequest constructHttpRequest(ModifiableHttpRequest baseRequest,  String contentType);
	
	/**
	 * Returns constructed HTTP response based on copy of passed HTTP response message
	 * <code>baseHeader</code> (if not null). If <code>contentType</code>
	 * is not <code>null</code>, constructed response will be able to hold body data 
	 * (initialized to empty byte array) and <i>Content-Type</i> header field will be
	 * set this value.
	 * @param baseResponse HTTP response message to base constructed message on, may be null 
	 * @param contentType <i>Content-Type</i> header field value if response with body wanted,
	 * may be null
	 */
	ModifiableHttpResponse constructHttpResponse(ModifiableHttpResponse baseResponse,  String contentType);
}
