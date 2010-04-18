package sk.fiit.rabbit.adaptiveproxy.services;

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
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface readonly { }
}
