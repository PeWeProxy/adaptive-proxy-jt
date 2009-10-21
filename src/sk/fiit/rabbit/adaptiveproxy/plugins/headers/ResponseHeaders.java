package sk.fiit.rabbit.adaptiveproxy.plugins.headers;

public interface ResponseHeaders extends ReadableHeaders {
	
	public String getReasonPhrase();
	
	public String getStatusCode() ;
	
	public String getStatusLine();
}
