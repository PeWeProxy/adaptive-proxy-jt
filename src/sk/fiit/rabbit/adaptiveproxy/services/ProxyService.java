package sk.fiit.rabbit.adaptiveproxy.services;

/**
 * Base interface for all services over HTTP messages. Interfaces that extend
 * this interface define signatures of unique services that provide read-only
 * of full access to the underlying HTTP messages on different level of abstraction.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ProxyService  {
	/**
	 * Returns this service's identification text.
	 * @return this services's identification
	 */
	String getServiceIdentification();
}
