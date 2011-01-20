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
	 * Readonly annotation signals AdaptiveProxy platform that particular method of
	 * service implementation does not change implementation's inner model. If used on
	 * definition of service method, realization of this method in every service
	 * implementation will be considered readonly.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface readonly { }
	
	/**
	 * Message independent annotation signals AdaptiveProxy platform that using the service
	 * implementations, once constructed, do not change the data of a particular HTTP
	 * message in any way. If that's the case, services usage mechanism that guarantees
	 * the data of the message to be actual, can be omitted. This annotation is to be used
	 * definitions (interfaces) of the service that are related to the AdaptiveProxy platform
	 * itself.
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface messageIdependent{ }
}
