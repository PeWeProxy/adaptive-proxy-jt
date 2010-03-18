package sk.fiit.rabbit.adaptiveproxy.messages;

import java.util.Date;
import org.apache.log4j.Logger;
import rabbit.http.HttpDateParser;
import rabbit.http.HttpHeader;
import rabbit.proxy.Connection;
import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.utils.ContentHeadersRemover;

public final class HttpMessageFactoryImpl implements HttpMessageFactory {
	private static final Logger log = Logger.getLogger(HttpMessageFactoryImpl.class.getName());
	
	private final Connection con;
	private final ModifiableHttpRequestImpl request;
	
	public HttpMessageFactoryImpl(Connection con, ModifiableHttpRequestImpl request) {
		this.con = con;
		this.request = request;
	}
	
	@Override
	public ModifiableHttpRequest constructHttpRequest(HttpRequest request,
			RequestHeaders fromHeaders, String contentType) {
		HeaderWrapper clientHeaders = null;
		if (fromHeaders != null) {
			clientHeaders = (HeaderWrapper) fromHeaders;
		} else {
			clientHeaders = new HeaderWrapper(new HttpHeader());
		}
		ModifiableHttpRequestImpl retVal = new ModifiableHttpRequestImpl(clientHeaders,request.getClientSocketAddress());
		if (contentType != null) {
			clientHeaders.setHeader ("Content-Type", contentType);
			// TODO skontrolovat ci toto neurobi pruser potom pri posielani (hint: chunking )
			clientHeaders.setHeader ("Content-Length", "0");
			retVal.setData(new byte[0]);
		}
		retVal.getServiceHandle().doServiceDiscovery();
		return retVal;
	}

	@Override
	public ModifiableHttpResponse constructHttpResponse(HttpResponse response, String contentType) {
		if (response == null)
			throw new IllegalArgumentException("'response' can not be null");
		HeaderWrapper webRPHeaders = ((ModifiableHttpResponseImpl)response).getWebResponseHeaders();
		return makeHttpResponse(webRPHeaders, contentType);
	}
	
	private ModifiableHttpResponse makeHttpResponse(HeaderWrapper fromHeaders, String contentType) {
		boolean withContent = (contentType != null);
		if (fromHeaders != null) {
			if (!withContent) {
				ContentHeadersRemover.removeContentHeaders(fromHeaders.getBackedHeader());
			}
		} else {
			fromHeaders = new HeaderWrapper(new HttpHeader());
			HttpHeader header = fromHeaders.getBackedHeader();
			header.setStatusLine("HTTP/1.1 200 OK");
			if (withContent) {
				header.setHeader ("Content-Type", contentType);
				// TODO skontrolovat ci toto neurobi pruser potom pri posielani (hint: chunking )
				header.setHeader ("Content-Length", "0");
			}
			header.setHeader("Date", HttpDateParser.getDateString(new Date()));
			header.setHeader("Via", con.getProxy().getProxyIdentity());
			HttpHeader filteredHeaders =  con.filterConstructedResponse(header);
			if (filteredHeaders != null)
				log.debug("If this was a normaly received response, it would have been blocked by header filters");
				// but now we don't care
		}
		ModifiableHttpResponseImpl retVal = new ModifiableHttpResponseImpl(fromHeaders,request);
		if (withContent)
			retVal.setData(new byte[0]);
		retVal.getServiceHandle().doServiceDiscovery();
		return retVal;
	}
	
	@Override
	public ModifiableHttpResponse constructHttpResponse(String contentType) {
		return makeHttpResponse(null, contentType);
	}
}
