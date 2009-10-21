package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicesHandle;

public interface HttpMessage {
	ServicesHandle getServiceHandle();
}
