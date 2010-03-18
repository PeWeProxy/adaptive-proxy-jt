package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeaders;

/**
 * A HTTP message factory is an entity, that could serve proxy plugins as a builder
 * of HTTP messages that are valid according to standards defined in
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC2616</a>.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface HttpMessageFactory {
	/**
	 * Returns constructed HTTP request based on copy of passed HTTP request header
	 * <code>baseHeader</code> (if not null). If <code>withContent</code> is
	 * <code>true</code>, constructed request will be able to hold body data 
	 * (initialized to empty byte array). Parameter <code>clientSocket</code>
	 * is used to set client's connection endpoint info of constructed response to
	 * existing value. 
	 * @param clientSocket socket representing connection's endpoint on the client's side,
	 * may be null 
	 * @param baseHeader HTTP request header to base constructed message on, may be null
	 * @param withContent whether constructed request should have body
	 * @return constructed HTTP request message
	 */
	ModifiableHttpRequest constructHttpRequest(InetSocketAddress clientSocket, RequestHeaders baseHeader, boolean withContent);
	
	/**
	 * Returns constructed HTTP response based on copy of passed HTTP response header
	 * <code>baseHeader</code> (if not null). If <code>withContent</code> is
	 * <code>true</code>, constructed response will be able to hold body data
	 * (initialized to empty byte array).
	 * @param baseHeader HTTP response header to base constructed message on, may be null 
	 * @param withContent whether constructed response should message
	 */
	ModifiableHttpResponse constructHttpResponse(ResponseHeaders baseHeader, boolean withContent);
}
