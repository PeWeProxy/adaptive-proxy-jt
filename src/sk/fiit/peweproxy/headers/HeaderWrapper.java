package sk.fiit.peweproxy.headers;

import java.util.List;
import rabbit.http.HttpHeader;
import sk.fiit.peweproxy.messages.HttpMessageImpl;

public final class HeaderWrapper implements WritableRequestHeader, WritableResponseHeader, Cloneable {
	enum HTTP_Version {
		HTTP_11 (HTTPVersion.HTTP_11,"HTTP/1.1"),
		HTTP_10 (HTTPVersion.HTTP_10,"HTTP/1.0"),
		HTTP_09 (HTTPVersion.HTTP_09,"HTTP/0.9");
		
		HTTPVersion APIversion;
		String versionString;
		
		private HTTP_Version(HTTPVersion APIversion, String versionString) {
			this.APIversion = APIversion;
			this.versionString = versionString;
		}
			
		static HTTPVersion getType4String(String versionstring) {
			HTTPVersion retVal = null;
			for (HTTP_Version type : HTTP_Version.values()) {
				if (type.versionString.equals(versionstring)) {
					retVal = type.APIversion;
					break;
				}
			}
			return retVal;
		}
		
		static HTTP_Version getType4APIType(HTTPVersion version) {
			for (HTTP_Version type : HTTP_Version.values()) {
				if (type.APIversion == version)
					return type;
			}
			return null;
		}
	}
	private final HttpHeader backedHeader; 
	private HttpMessageImpl<?> message;
	
	public HeaderWrapper(HttpHeader header) {
		backedHeader = header;
	}
	
	public void setHttpMessage(HttpMessageImpl<?> message) {
		this.message = message;
	}
	
	public HttpMessageImpl<?> getHttpMessage() {
		return message;
	}
	
	public HttpHeader getBackedHeader() {
		return backedHeader;
	}
	
	@Override
	public void addField(String name, String value) {
		backedHeader.addHeader(name, value);
	}
	
	/*@Override
	public String getContent() {
		return backedHeader.getContent();
	}*/
	
	@Override
	public String getField(String name) {
		return backedHeader.getHeader(name);
	}
	
	@Override
	public List<String> getFields(String name) {
		return backedHeader.getHeaders(name);
	}
	
	private String getHTTPVersionInternal() {
		if (backedHeader.isRequest())
			return backedHeader.getHTTPVersion();
		else
			return backedHeader.getResponseHTTPVersion();
	}
	
	@Override
	public String getHTTPVersionString() {
		return getHTTPVersionInternal();
	}
	
	@Override
	public HTTPVersion getHTTPVersion() {
		return HTTP_Version.getType4String(getHTTPVersionInternal());
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
	public String getDestionation() {
		return getDestionationInternal();
	}
	
	private String getDestionationInternal() {
		String reqURI = backedHeader.getRequestURI();
		String host = backedHeader.getHeader("Host");
		if (reqURI.charAt(0) == '/') {
			// Podla RFC by proxy srv VZDY mal dostat plnu URI v Request-Line
			// tudiz toto by sa NIKDY nemalo stat
			String protocol = "";
			String versionString = getHTTPVersionInternal();
			if (versionString.toLowerCase().contains("http/"))
				protocol = "http://";
			else if (versionString.toLowerCase().contains("https/"))
				protocol = "https://";
			return protocol+host+reqURI;
		}
		return reqURI;
	}
	
	@Override
	public String getStatusCode() {
		return backedHeader.getStatusCode();
	}
	
	@Override
	public String getStatusLine() {
		return backedHeader.getStatusLine();
	}
	
	/*@Override
	public boolean isTunneling() {
		return backedHeader.isSecure();
	}*/
	
	@Override
	public void removeField(String name) {
		backedHeader.removeHeader(name);
	}
	
	@Override
	public void removeValue(String name, String value) {
		backedHeader.removeValue(value);
	}
	
	/*@Override
	public void setContent(String content) {
		backedHeader.setContent(content);
	}*/
	
	@Override
	public void setExistingValue(String name, String value, String newValue) {
		backedHeader.setExistingValue(null, value, newValue);
	}
	
	@Override
	public void setField(String name, String value) {
		backedHeader.setHeader(name, value);
	}
	
	@Override
	public void setHTTPVersionString(String version) {
		if (backedHeader.isRequest())
			backedHeader.setHTTPVersion(version);
		else
			backedHeader.setResponseHTTPVersion(version);
	}
	
	@Override
	public void setHTTPVersion(HTTPVersion version) {
		String versionString = HTTP_Version.getType4APIType(version).versionString;
		if (backedHeader.isRequest())
			backedHeader.setHTTPVersion(versionString);
		else
			backedHeader.setResponseHTTPVersion(versionString);
		
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
	public void setDestination(String destination) {
		String reqURI = backedHeader.getRequestURI();
		String host = backedHeader.getHeader("Host");
		if (reqURI.charAt(0) == '/' && host != null) {
			// nemalo by sa vykonat kedze klienti MUSIA posielat uplnu
			// absolutnu URI ked idu cez proxy
			int hStart = destination.indexOf("://");
			hStart = (hStart == -1) ? 0 : hStart+3;
			int resStart = destination.indexOf('/', hStart);
			if (resStart != -1) {
				backedHeader.setHeader("Host", destination.substring(hStart, resStart));
				backedHeader.setRequestURI(destination.substring(resStart));
			} else {
				backedHeader.setHeader("Host", destination.substring(hStart));
				backedHeader.setRequestURI("/");
			}
		} else {
			int hStart = destination.indexOf("://");
			if (hStart != -1) {
				hStart += 3;
				int resStart = destination.indexOf('/', hStart);
				if (resStart != -1) {
					backedHeader.setRequestURI(destination);
					if (host != null)
						backedHeader.setHeader("Host", destination.substring(hStart, resStart));
				} else {
					backedHeader.setRequestURI(destination+"/");
					if (host != null)
						backedHeader.setHeader("Host", destination.substring(hStart));
				}
			} else {
				String protocol = "http://";
				int protoEnd = reqURI.indexOf("://");
				if (protoEnd != -1)
					protocol = reqURI.substring(0, protoEnd+3) ;
				int resStart = destination.indexOf('/');
				if (resStart == -1) {
					if (host != null)
						backedHeader.setHeader("Host", destination);
					destination = destination+"/";
				} else {
					if (host != null)
						backedHeader.setHeader("Host", destination.substring(0, resStart));
				}
				backedHeader.setRequestURI(protocol+destination);
			}
		}
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
	
	public String getFullRequestLine() {
		StringBuilder sb = new StringBuilder(getMethod());
		sb.append(' ');
		sb.append(getDestionationInternal());
		sb.append(' ');
		sb.append(getHTTPVersionInternal());
		return sb.toString();
	}
	
	@Override
	public HeaderWrapper clone() {
		return new HeaderWrapper(backedHeader.clone());
	}
	
	@Override
	public String toString() {
		return backedHeader.toString();
	}
}
