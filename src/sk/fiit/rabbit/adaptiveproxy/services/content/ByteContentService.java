package sk.fiit.rabbit.adaptiveproxy.services.content;

import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

public interface ByteContentService extends ProxyService {
	byte[] getData();
}
