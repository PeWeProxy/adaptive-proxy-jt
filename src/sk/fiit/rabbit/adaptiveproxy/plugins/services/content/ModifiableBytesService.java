package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface ModifiableBytesService extends ProxyService {
	void setData(byte[] data);
}
