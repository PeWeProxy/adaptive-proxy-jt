package sk.fiit.rabbit.adaptiveproxy.headers;

/**
 * Base interface for modifiable HTTP message header.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
 */
public interface WritableHeader extends ReadableHeader {
	
	/**
	 * Sets the HTTP version value of this message header.
	 * @param version new HTTP version text for this message header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.1">RFC 2616: 3.1 HTTP Version</a>
	 */
	public void setHTTPVersionString(String version);
	
	/**
	 * Sets the HTTP version value of this request message header to text associated with passed
	 * <code>HTTPVersion</code> type.
	 * @param version enum type associated with new HTTP version text for this request header
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.1">RFC 2616: 3.1 HTTP Version</a>
	 */
	public void setHTTPVersion(HTTPVersion version);
	
	/**
	 * Sets the value of the header field with name <code>type</code> to <code>value</code>.
	 * This sets the value of the first header field with matching name in this header's
	 * collection of fields If no such field is present in this header, new one is added.
	 * @param name name of the header field
	 * @param value new value for the header field
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public void setField(String name, String value);
	
	/**
	 * Sets the value of the header field with name <code>type</code> and value <code>value</code> to new value
	 * <code>newValue</code>. This sets the value of the header field with matching name and value in this
	 * header's fields collection.
	 * @param name name of the header field
	 * @param value existing value of the header field
	 * @param newValue new value for the header field
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public void setExistingValue(String name, String value, String newValue);
	
	/**
	 * Adds new header field with name <code>name</code> and value set to <code>value</code>
	 * to this message header. Existing header fields with the same name are not modified.
	 * @param name name of the header field
	 * @param value new value for the header field
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public void addField(String name, String value);
	
	/**
	 * Removes all header fields with name <code>name</code> from this header.
	 * @param name name of the header field
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public void removeField(String name);
	
	/**
	 * Removes header field with name <code>name</code> and value <code>value</code> from this header.
	 * Only first header field with matching name and value is removed while iterating
	 * over this header's collection of fields.
	 * @param name name of the header field
	 * @param value existing value of the header field
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC 2616: 4.2 Message Headers</a>
	 */
	public void removeValue(String name, String value);
}
