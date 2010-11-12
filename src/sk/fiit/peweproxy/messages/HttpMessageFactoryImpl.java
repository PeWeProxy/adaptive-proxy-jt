package sk.fiit.peweproxy.messages;

import java.util.Date;
import org.apache.log4j.Logger;
import rabbit.http.HttpDateParser;
import rabbit.http.HttpHeader;
import rabbit.httpio.ConnectionSetupResolver;
import rabbit.proxy.Connection;
import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.utils.HeaderUtils;

public final class HttpMessageFactoryImpl implements HttpMessageFactory {
	private static final Logger log = Logger.getLogger(HttpMessageFactoryImpl.class.getName());
	
	private final AdaptiveEngine adaptiveEngine;
	private final Connection con;
	private final HttpRequestImpl request;
	private HttpResponseImpl response;
	
	public HttpMessageFactoryImpl(AdaptiveEngine adaptiveEngine, Connection con, HttpRequestImpl request) {
		this.adaptiveEngine = adaptiveEngine;
		this.con = con;
		this.request = request;
	}
	
	private void setContent(HttpMessageImpl<?> message, String contentType) {
		HttpHeader proxyHeader = message.getHeader().getBackedHeader();
		if (contentType != null) {
			proxyHeader.setHeader ("Content-Type", contentType);
			proxyHeader.setHeader ("Content-Length", "0");
			con.fixResponseHeader(proxyHeader, 0, true);
			message.setData(new byte[0]);
		} else {
			message.setData(null);
			HeaderUtils.removeContentHeaders(proxyHeader);
		}
	}
	
	@Override
	public ModifiableHttpRequest constructHttpRequest(ModifiableHttpRequest baseRequest, String contentType) {
		ModifiableHttpRequestImpl retVal = null;
		if (baseRequest != null) {
			retVal = (ModifiableHttpRequestImpl) ((ModifiableHttpRequestImpl)baseRequest).clone();
		} else {
			HttpHeader newHeader = new HttpHeader();
			retVal = new ModifiableHttpRequestImpl(adaptiveEngine.getModulesManager(),new HeaderWrapper(newHeader)
						, request);
			//ConnectionSetupResolver conSetup = new ConnectionSetupResolver(newHeader); 
		}
		retVal.setAllowedThread();
		setContent(retVal, contentType);
		return retVal;
	}

	@Override
	public ModifiableHttpResponse constructHttpResponse(ModifiableHttpResponse baseResponse, String contentType) {
		ModifiableHttpResponseImpl retVal = null;
		if (baseResponse != null)
			retVal = (ModifiableHttpResponseImpl) ((ModifiableHttpResponseImpl)baseResponse).clone();
		else {
			if (response == null) // when creating response during request processing
				retVal = new ModifiableHttpResponseImpl(adaptiveEngine.getModulesManager()
						,new HeaderWrapper(new HttpHeader()),request);
			else
				retVal = new ModifiableHttpResponseImpl(adaptiveEngine.getModulesManager()
						,new HeaderWrapper(new HttpHeader()),response);
			HttpHeader header = retVal.getHeader().getBackedHeader();
			header.setStatusLine("HTTP/1.1 200 OK");
			header.setHeader("Date", HttpDateParser.getDateString(new Date()));
			header.setHeader("Via", con.getProxy().getProxyIdentity());
			HttpHeader filteredHeaders =  con.filterConstructedResponse(header);
			if (filteredHeaders != null)
				log.debug("If this was a normaly received response, it would have been blocked by header filters");
				// but now we don't care
		}
		retVal.setAllowedThread();
		setContent(retVal, contentType);
		return retVal;
	}
	
	public void setResponse(HttpResponseImpl response) {
		this.response = response;
	}
}
