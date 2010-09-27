package sk.fiit.peweproxy.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
	
	/**
	 * Readonly annotation signals proxy server that particular method of service
	 * implementation does not change implementation's inner model. If used on
	 * definition of service method, realization of this method in every service
	 * implementation will be considered readonly.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface readonly { }
}
