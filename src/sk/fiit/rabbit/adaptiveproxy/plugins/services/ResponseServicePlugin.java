package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.List;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.ResponsePlugin;

public interface ResponseServicePlugin extends ServicePlugin, ResponsePlugin {
	List<ResponseServiceProvider> provideResponseServices(HttpResponse response);
}
