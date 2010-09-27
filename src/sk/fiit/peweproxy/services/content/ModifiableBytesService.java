package sk.fiit.peweproxy.services.content;

/**
 * Modifiable byte content service provides full access to the data of the HTTP
 * message body on the lowest level of abstraction possible, as an array of bytes.
 * Implementation of this service modifies "Content-Length" HTTP message header
 * field if necessary (see <a
 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.13">RFC2616:
 * 14.13 Content-Length</a>).
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the four basic services and service plugins are not
 * allowed to provide implementations of it. Definition of this service and also
 * its implementation is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ModifiableBytesService extends ByteContentService {
	/**
	 * Sets the HTTP message body data to passed <code>data</code>.
	 * @param data new HTTP message body data
	 */
	void setData(byte[] data);
}
