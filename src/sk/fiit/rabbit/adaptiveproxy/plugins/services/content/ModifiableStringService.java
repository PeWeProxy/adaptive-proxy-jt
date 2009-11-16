package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import java.nio.charset.Charset;

public interface ModifiableStringService extends StringContentService {
	StringBuilder getModifiableContent();

	void setContent(String content);
	
	void setCharset(Charset charset);
}
