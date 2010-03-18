package sk.fiit.rabbit.adaptiveproxy.headers;

public interface RequestHeaders extends ReadableHeaders {
	
	public String getRequestLine();
	
	public String getRequestURI();
	
	public String getMethod();
	
	public String getHTTPVersion();
	
	public boolean isSecure();
	
	public int size();
}
