package sk.fiit.rabbit.adaptiveproxy.plugins.headers;

import java.util.List;

public interface ReadableHeaders {
	public String getHeader(String type);
	
	public List<String> getHeaders(String type);
}
