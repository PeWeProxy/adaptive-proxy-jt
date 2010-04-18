package sk.fiit.rabbit.adaptiveproxy.services;

/**
 * Service unavailable exception is thrown to indicate that no implementation of
 * requested service over HTTP message is available (provided) for particular message. 
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public final class ServiceUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 7393269656517250304L;
	final Class<? extends ProxyService> clazz;
	
	/**
	 * Constructs new <code>ServiceUnavailableException</code> with unavailable service
	 * class set to <code>serviceClass</code>.
	 * @param serviceClass class of the unavailable service
	 */
	public ServiceUnavailableException(Class<? extends ProxyService> serviceClass, String message, Throwable cause) {
		super(message,cause);
		clazz = serviceClass;
	}
	
	/**
	 * Returns class of the service implementation of which is not available over
	 * HTTP message.
	 * @return class of the unavailable service
	 */
	public Class<? extends ProxyService> getServiceClass() {
		return clazz;
	}
}
