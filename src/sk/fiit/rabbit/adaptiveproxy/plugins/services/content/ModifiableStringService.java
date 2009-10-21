package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import java.nio.charset.Charset;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface ModifiableStringService extends ProxyService {
	StringBuilder getModifiableContent();

	void setContent(String content);
	
	void setCharset(Charset charset);
}
