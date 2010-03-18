package sk.fiit.rabbit.adaptiveproxy.headers;

import java.util.List;

import rabbit.http.HttpHeader;

public final class HeaderWrapper implements WritableRequestHeaders, WritableResponseHeaders {
	private final HttpHeader backedHeader; 
	
	public HeaderWrapper(HttpHeader header) {
		backedHeader = header;
	}
	
	public HttpHeader getBackedHeader() {
		return backedHeader;
	}
	
	@Override
	public void addHeader(String type, String value) {
		backedHeader.addHeader(type, value);
	}
	
	/*@Override
	public String getContent() {
		return backedHeader.getContent();
	}*/
	
	@Override
	public String getHeader(String type) {
		return backedHeader.getHeader(type);
	}
	
	@Override
	public List<String> getHeaders(String type) {
		return backedHeader.getHeaders(type);
	}
	
	@Override
	public String getHTTPVersion() {
		if (backedHeader.isRequest())
			return backedHeader.getHTTPVersion();
		else
			return backedHeader.getResponseHTTPVersion();
	}
	
	@Override
	public String getMethod() {
		return backedHeader.getMethod();
	}
	
	@Override
	public String getReasonPhrase() {
		return backedHeader.getReasonPhrase();
	}
	
	@Override
	public String getRequestLine() {
		return backedHeader.getRequestLine();
	}
	
	@Override
	public String getRequestURI() {
		return backedHeader.getRequestURI();
	}
	
	@Override
	public String getStatusCode() {
		return backedHeader.getStatusCode();
	}
	
	@Override
	public String getStatusLine() {
		return backedHeader.getStatusLine();
	}
	
	@Override
	public boolean isSecure() {
		return backedHeader.isSecure();
	}
	
	@Override
	public void removeHeader(String type) {
		backedHeader.removeHeader(type);
	}
	
	@Override
	public void removeValue(String value) {
		backedHeader.removeValue(value);
	}
	
	/*@Override
	public void setContent(String content) {
		backedHeader.setContent(content);
	}*/
	
	@Override
	public void setExistingValue(String current, String newValue) {
		backedHeader.setExistingValue(current, newValue);
	}
	
	@Override
	public void setHeader(String type, String value) {
		backedHeader.setHeader(type, value);
	}
	
	@Override
	public void setHTTPVersion(String version) {
		if (backedHeader.isRequest())
			backedHeader.setHTTPVersion(version);
		else
			backedHeader.setResponseHTTPVersion(version);
	}
	
	@Override
	public void setMehtod(String method) {
		backedHeader.setMehtod(method);
	}
	
	@Override
	public void setReasonPhrase(String reason) {
		backedHeader.setReasonPhrase(reason);
	}
	
	@Override
	public void setRequestLine(String line) {
		backedHeader.setRequestLine(line);
	}
	
	@Override
	public void setRequestURI(String requestURI) {
		backedHeader.setRequestURI(requestURI);
	}
	
	@Override
	public void setStatusCode(String status) {
		backedHeader.setStatusCode(status);
	}
	
	@Override
	public void setStatusLine(String line) {
		backedHeader.setStatusLine(line);
	}
	
	@Override
	public int size() {
		return backedHeader.size();
	}
	
	@Override
	public String toString() {
		return backedHeader.toString();
	}
}
