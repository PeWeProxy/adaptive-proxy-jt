package sk.fiit.peweproxy.services.content;

import java.nio.charset.Charset;

/**
 * Modifiable string content service provides full access to the data of the textual
 * HTTP message body in the form of text. Implementation of this service modifies
 * "charset" part of the "Content-Type" HTTP message header field if necessary (see
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">RFC2616:
 * 14.17 Content-Type</a>).
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the five final services and service plugins are not
 * allowed to provide implementations of it. Definition of this service and also
 * its implementation is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ModifiableStringService extends StringContentService {
	/**
	 * Returns modifiable text of this textual HTTP message body as {@link StringBuilder}.
	 * @return returns modifiable text ofthis HTTP message body
	 */
	StringBuilder getModifiableContent();

	/**
	 * Sets the text content of textual HTTP message body to passed <code>content</code>.
	 * @param content new textual HTTP message body content
	 */
	void setContent(String content);
	
	/**
	 * Sets the charset of the textual HTTP message body content. Charset determines
	 * conversion of textual body content to sequence of bytes and modification
	 * of "Content-Type" HTTP message header field.
	 * @param charset new charset of textual HTTP message body content
	 */
	void setCharset(Charset charset);
}
