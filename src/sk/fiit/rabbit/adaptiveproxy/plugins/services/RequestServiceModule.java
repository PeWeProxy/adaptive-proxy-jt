package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.List;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;

public interface RequestServiceModule extends ServiceModule, RequestPlugin {
	List<RequestServiceProvider> provideRequestServices(HttpRequest request);
}
