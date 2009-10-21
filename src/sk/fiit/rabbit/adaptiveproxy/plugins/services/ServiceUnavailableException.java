package sk.fiit.rabbit.adaptiveproxy.plugins.services;

public final class ServiceUnavailableException extends Exception {
	private static final long serialVersionUID = 7393269656517250304L;
	final Class<? extends ProxyService> clazz;
	
	public ServiceUnavailableException(Class<? extends ProxyService> serviceClass) {
		clazz = serviceClass;
	}
	
	public Class<? extends ProxyService> getServiceClass() {
		return clazz;
	}
}
