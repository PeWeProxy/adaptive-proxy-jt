package sk.fiit.rabbit.adaptiveproxy.plugins.headers;

public interface WritableRequestHeaders extends RequestHeaders, WritableHeaders {

	public void setHTTPVersion(String version);
	
	public void setMehtod(String method);
	
	public void setRequestLine(String line);
	
	public void setRequestURI(String requestURI);
}
