package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;
import java.util.Date;
import org.apache.log4j.Logger;
import rabbit.http.HttpDateParser;
import rabbit.http.HttpHeader;
import rabbit.proxy.Connection;
import sk.fiit.rabbit.adaptiveproxy.AdaptiveEngine;
import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeader;
import sk.fiit.rabbit.adaptiveproxy.utils.HeaderUtils;

public final class HttpMessageFactoryImpl implements HttpMessageFactory {
	private static final Logger log = Logger.getLogger(HttpMessageFactoryImpl.class.getName());
	
	private final AdaptiveEngine adaptiveEngine;
	private final Connection con;
	private final ModifiableHttpRequestImpl request;
	
	public HttpMessageFactoryImpl(AdaptiveEngine adaptiveEngine, Connection con, ModifiableHttpRequestImpl request) {
		this.adaptiveEngine = adaptiveEngine;
		this.con = con;
		this.request = request;
	}
	
	@Override
	public ModifiableHttpRequest constructHttpRequest(InetSocketAddress clientSocket,
			RequestHeader baseHeader, String contentType) {
		HeaderWrapper clientHeaders = null;
		if (baseHeader != null) {
			clientHeaders = new HeaderWrapper(((HeaderWrapper) baseHeader).getBackedHeader().clone());
		} else {
			clientHeaders = new HeaderWrapper(new HttpHeader());
		}
		ModifiableHttpRequestImpl retVal = new ModifiableHttpRequestImpl(adaptiveEngine.getModulesManager()
					,clientHeaders,(request != null) ? request.getClientSocketAddress() : null);
		retVal.setAllowedThread();
		if (contentType != null) {
			clientHeaders.setField ("Content-Type", contentType);
			// TODO skontrolovat ci toto neurobi pruser potom pri posielani (hint: chunking )
			clientHeaders.setField ("Content-Length", "0");
			retVal.setData(new byte[0]);
		}
		return retVal;
	}

	@Override
	public ModifiableHttpResponse constructHttpResponse(ResponseHeader baseHeader, String contentType) {
		boolean withContent = (contentType != null);
		HeaderWrapper fromHeaders = null;
		if (baseHeader != null)
			fromHeaders = new HeaderWrapper(((HeaderWrapper) baseHeader).getBackedHeader().clone());
		if (fromHeaders != null) {
			if (!withContent) {
				HeaderUtils.removeContentHeaders(fromHeaders.getBackedHeader());
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
		ModifiableHttpResponseImpl retVal = new ModifiableHttpResponseImpl(adaptiveEngine.getModulesManager()
				,fromHeaders,request);
		retVal.setAllowedThread();
		if (withContent)
			retVal.setData(new byte[0]);
		return retVal;
	}
}
