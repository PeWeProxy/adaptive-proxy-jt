package sk.fiit.peweproxy.services.content;

import sk.fiit.peweproxy.services.ProxyService;

/**
 * Byte content service provides read-only access to the data of the HTTP message
 * body on the lowest level of abstraction possible, as an array of bytes.
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the five final services and service plugins are not
 * allowed to provide implementations of it. Definition of this service and also
 * its implementation is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ByteContentService extends ProxyService {
	/**
	 * Returns HTTP message body data as an array of bytes.
	 * @return byte array of message body
	 */
	@readonly
	byte[] getData();
}
