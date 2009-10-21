package sk.fiit.rabbit.adaptiveproxy.plugins.headers;

public interface WritableHeaders extends ReadableHeaders {
	
	public void setHeader(String type, String value);
	
	public void setExistingValue(String current, String newValue);
	
	public void addHeader(String type, String value);
	
	public void removeHeader(String type);
	
	public void removeValue(String value);
}
