package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;

public interface ResponseServiceProvider extends ServiceProvider {
	void setResponseContext(ModifiableHttpResponse response);
}
