package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.services.ServicesHandle;

public interface HttpMessage {
	ServicesHandle getServiceHandle();
}
