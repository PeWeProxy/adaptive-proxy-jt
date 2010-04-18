package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

abstract class BaseServiceProvider<MessageType extends HttpMessageImpl<?>, Service extends ProxyService>
	implements RequestServiceProvider<Service>, ResponseServiceProvider<Service>, ProxyService {
	final MessageType httpMessage;
	
	public BaseServiceProvider(MessageType httpMessage) {
		if (httpMessage == null)
			throw new IllegalStateException("HTTP message can not be null");
		if (httpMessage.getData() == null)
			throw new IllegalStateException("No data in message");
		this.httpMessage = httpMessage;
	}
	
	@Override
	public String getServiceIdentification() {
		return "AdaptiveProxy."+getServiceClass().getSimpleName();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Service getService() {
		return (Service)this;
	}
	
	@Override
	public boolean initChangedModel() {
		return false;
	}
	
	abstract Class<Service> getServiceClass();
}
