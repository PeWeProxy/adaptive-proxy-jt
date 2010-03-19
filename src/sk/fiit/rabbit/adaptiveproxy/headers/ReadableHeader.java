package sk.fiit.rabbit.adaptiveproxy.headers;

import java.util.List;

/**
 * Base interface for read-only HTTP message header.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
 */
public interface ReadableHeader {
	public enum HTTPVersion {
		HTTP_09,
		HTTP_10,
		HTTP_11,
	}
	
	/**
	 * Returns the HTTP version value of this message header.
	 * @return HTTP version text of this message header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.1">RFC 2616: 3.1 HTTP Version</a>
	 */
	public String getHTTPVersionString();
	
	/**
	 * Returns the <code>HTTPVersion</code> type associated with HTTP version value of this message header.
	 * @return enum type for HTTP version text of this message header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.1">RFC 2616: 3.1 HTTP Version</a>
	 */
	public HTTPVersion getHTTPVersion();
	
	/**
	 * Returns the value of the header field with name <code>type</code>, or
	 * <code>null</code> if no such field is present in this HTTP header.
	 * @param name name of the header field
	 * @return value of the header field or <code>null</code> if no such field
	 * is present
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public String getField(String name);
	
	/**
	 * Returns the list if values of the header fields with name <code>type</code>,
	 * or an empty list if no such fields are present in this HTTP header.
	 * @param name name of the header fields
	 * @return list of values of the header fields or <code>null</code> if no such
	 * fields are present
	 *  @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public List<String> getFields(String name);
	
	/**
	 * Returns the number of fields in this HTTP header.
	 * @return number of fields in this HTTP header
	 *  @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public int size();
}
