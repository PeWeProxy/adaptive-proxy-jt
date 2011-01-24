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
	 * Readonly annotation is used to change default behaviour of platform's mechanism, that
	 * guarantees the HTTP message data to be actual for every access while managing commits
	 * of the changes made by services with least possible writes. This annotation can be
	 * used on service methods and service type itself to omit execution of some steps
	 * of the message guarding mechanism when using services in a way that does not require
	 * them to be performed. 
	 * <p> 
	 * Annotating <b>service method</b> with this annotation signals AdaptiveProxy platform
	 * that particular method of the service implementation does not change implementation's
	 * inner model. If used on definition of the service method (in service definition
	 * interface), realization of this method in every service implementation will be
	 * considered readonly. In case of an attempt to access the HTTP message data after
	 * calling method with readonly annotation, message guarding mechanism won't call
	 * service provider's <code>doChanges()</code> since there are no changes to commit to
	 * the message.   
	 * <p>
	 * Annotating <b>entire service definition</b> (interface) with this annotation signals
	 * AdaptiveProxy platform that using the service implementations, once constructed, do
	 * not depend on or change the data of a particular HTTP message in any way. If that's
	 * the case, message guarding mechanism can be omitted entirely and calling methods of
	 * the service won't fire commit of the changes made by other services before to make
	 * the data of the HTTP message actual.  
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD,ElementType.TYPE})
	public @interface readonly { }
}
