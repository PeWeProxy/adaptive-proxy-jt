package sk.fiit.peweproxy.services.content;

import sk.fiit.peweproxy.services.ProxyService;

/**
 * String content service provides read-only access to the data of the textual HTTP
 * message body in the form of text.
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the five final services and service plugins are not
 * allowed to provide implementations of it. Definition of this service and also
 * its implementation is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface StringContentService extends ProxyService {
	/**
	 * Returns text of textual body of the HTTP message
	 * @return text of the HTTP message body
	 */
	@readonly
	public String getContent();
}
