package sk.fiit.rabbit.adaptiveproxy.headers;

public interface WritableResponseHeaders extends ResponseHeaders, WritableHeaders {
	
	public void setReasonPhrase(String reason);
	
	public void setStatusCode(String status);
	
	public void setStatusLine(String line);
}
