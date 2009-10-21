package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface ByteContentService extends ProxyService {
	byte[] getData();
}
