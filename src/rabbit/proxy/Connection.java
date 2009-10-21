package rabbit.proxy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.List;
import rabbit.cache.Cache;
import rabbit.cache.CacheEntry;
import rabbit.handler.BaseHandler;
import rabbit.handler.Handler;
import rabbit.handler.MultiPartHandler;
import rabbit.http.HttpDateParser;
import rabbit.http.HttpHeader;
import rabbit.httpio.HttpHeaderListener;
import rabbit.httpio.HttpHeaderReader;
import rabbit.httpio.HttpHeaderSender;
import rabbit.httpio.HttpHeaderSentListener;
import rabbit.httpio.RequestLineTooLongException;
import rabbit.httpio.request.ChunkSeparator;
import rabbit.httpio.request.ClientResourceHandler;
import rabbit.httpio.request.ContentSeparator;
import rabbit.httpio.request.FixedLengthSeparator;
import rabbit.httpio.request.MultiPipeSeparator;
import rabbit.io.BufferHandle;
import rabbit.io.BufferHandler;
import rabbit.io.CacheBufferHandle;
import rabbit.util.Counter;
import rabbit.util.Logger;

/** The base connection class for rabbit.
 *
 *  This is the class that handle the http protocoll for proxies.
 *
 *  For the technical overview of how connections and threads works
 *  see the file htdocs/technical_documentation/thread_handling_overview.txt
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class Connection {
    /** The id of this connection. */
    private ConnectionId id;

    /** The client channel */
    private SocketChannel channel;

    /** The client's request headers */
    private HttpHeader clientRequest;
    
    /** The proxy's web request headers */
    private HttpHeader proxyRequest;

    /** The current request buffer handle */
    private BufferHandle requestHandle;

    /** The buffer handler. */
    private BufferHandler bufHandler;

    /** The selector to use */
    private Selector selector;

    /** The proxy we are serving */
    private HttpProxy proxy;

    /** The current status of this connection. */
    private String status;

    /** The time this connection was started. */
    private long started;

    private boolean  keepalive      = true;
    private boolean  meta           = false;
    private boolean  chunk          = true;
    private boolean  mayUseCache    = true;
    private boolean  mayCache       = true;
    private boolean  mayFilter      = true;
    private boolean  mustRevalidate = false;
    private boolean  addedINM       = false;
    private boolean  addedIMS       = false;

    /** If the user has authenticated himself */
    private String userName = null;
    private String password = null;

    /* Current status information */
    private String requestVersion = null;
    private String requestLine   = null;
    private String statusCode    = null;
    private String extraInfo     = null;
    private String contentLength = null;

    private ClientResourceHandler clientResourceHandler;

    private StandardResponseHeaders responseHandler;

    private TrafficLoggerHandler tlh = new TrafficLoggerHandler ();

    public Connection (ConnectionId id,	SocketChannel channel,
		       Selector selector, HttpProxy proxy,
		       BufferHandler bufHandler) {
	this.id = id;
	this.channel = channel;
	this.selector = selector;
	this.proxy = proxy;
	this.requestHandle = new CacheBufferHandle (bufHandler);
	this.bufHandler = bufHandler;
	proxy.addCurrentConnection (this);
	responseHandler =
	    new StandardResponseHeaders (proxy.getServerIdentity (), this);
    }

    // For logging and status
    public ConnectionId getId () {
	return id;
    }

    /** Read a request.
     */
    public void readRequest () {
	clearStatuses ();
	proxy.getAdaptiveEngine().newRequestAttempt(this);
	try {
	    channel.socket ().setTcpNoDelay (true);
	    HttpHeaderListener clientListener = new RequestListener ();
	    new HttpHeaderReader (channel, requestHandle, selector,
				  getLogger (), tlh.getClient (), true,
				  proxy.getStrictHttp (), clientListener);
	} catch (Throwable ex) {
	    handleFailedRequestRead (ex);
	}
    }

    private void handleFailedRequestRead (Throwable t) {
	if (t instanceof RequestLineTooLongException) {
	    HttpHeader err = getHttpGenerator ().get414 ();
	    // Send response and close
	    sendAndClose (err);
	} else {
	    getLogger ().logInfo ("Exception when reading request: " +
				  getStackTrace (t));
	    closeDown ();
	}
    }

    private class RequestListener implements HttpHeaderListener {
	public void httpHeaderRead (HttpHeader header, BufferHandle bh,
				    boolean keepalive, boolean isChunked,
				    long dataSize) {
	    setKeepalive (keepalive);
	    requestRead (header, bh, isChunked, dataSize);
	}

	public void closed () {
		proxy.getAdaptiveEngine().getEventsHandler().logClientClosedCon(Connection.this);
	    closeDown ();
	}

	public void timeout () {
	    readTimeout();
	}

	public void failed (Exception e) {
	    readFailed(e);
	}
    }
    
    public void readTimeout() {
    	proxy.getAdaptiveEngine().getEventsHandler().logRequestReadTimeout(Connection.this);
    	getLogger ().logInfo ("Timeout when reading client request");
 	    closeDown ();
    }
    
    public void readFailed(Exception e) {
    	proxy.getAdaptiveEngine().getEventsHandler().logRequestReadFailed(this);
    	handleFailedRequestRead (e);
    }

    private String getStackTrace (Throwable t) {
	StringWriter sw = new StringWriter ();
	PrintWriter ps = new PrintWriter (sw);
	t.printStackTrace (ps);
	return sw.toString ();
    }

    private void handleInternalError (Throwable t) {
	extraInfo =
	    extraInfo != null ? extraInfo + t.toString () : t.toString ();
	String message = getStackTrace (t);
	getLogger ().logError ("Internal Error: " + message);
	HttpHeader internalError = getHttpGenerator ().get500 (t);
	// Send response and close
	sendAndClose (internalError);
    }

    private void requestRead (HttpHeader request, BufferHandle bh,
			      boolean isChunked, long dataSize) {
	if (request == null) {
	    getLogger ().logError ("Got a null request");
	    closeDown ();
	    return;
	}
	status = "Request read, processing";
	this.clientRequest = request;
	proxyRequest = request.clone();
	this.requestHandle = bh;
	requestVersion = request.getHTTPVersion ();
	proxy.getAdaptiveEngine().newRequest(this, isChunked);
	if (requestVersion == null) {
	    // TODO: fix http/0.9 handling.
	    getLogger ().logInfo ("bad header read: " + request);
	    closeDown ();
	    return;
	}
	requestVersion = requestVersion.toUpperCase ();
	String proxyChain = proxyRequest.getHeader("Via");
	if (proxyChain == null)
		proxyChain = "";
	proxyRequest.setHeader ("Via", proxyChain + " " + proxy.getProxyIdentity());

	requestLine = request.getRequestLine ();
	getCounter ().inc ("Requests");

	try {
	    // SSL requests are special in a way...
	    // Don't depend upon being able to build URLs from the header...
	    if (request.isSSLRequest ()) {
		checkAndHandleSSL (bh);
		return;
	    }

	    // Now set up handler of any posted data.
	    ContentSeparator separator = null;
	    // is the request resource chunked?
	    if (isChunked) {
			separator = setupChunkedContent ();
	    } else {
		// no? then try regular data
		String ct = null;
		ct = request.getHeader ("Content-Type");
		if (hasRegularContent (request, ct, dataSize))
		    separator = setupClientResourceHandler (dataSize);
		else
		    // still no? then try multipart
		    if (ct != null)
		    	separator = readMultiPart (ct);
	    }
	    filterAndHandleRequest(separator,dataSize,isChunked);
	} catch (Throwable t) {
	    handleInternalError (t);
	}
    }

    private boolean hasRegularContent (HttpHeader request, String ct,
				       long dataSize) {
	if (request.getContent () != null)
	    return true;
	if (ct != null && ct.startsWith ("multipart/byteranges"))
	    return false;
	return dataSize > -1;
    }

    /** Filter the request and handle it.
     * @param header the request
     */
    // TODO: filtering here may block! be prepared to run filters in a
    // TODO: separate thread.
    private void filterAndHandleRequest (ContentSeparator separator, long dataSize, boolean isChunked) {
	// Filter the request based on the header.
	// A response means that the request is blocked.
	// For ad blocking, bad header configuration (http/1.1 correctness) ...
	HttpHeaderFilterer filterer = proxy.getHttpHeaderFilterer ();
	HttpHeader badresponse = filterer.filterHttpIn (this, channel, proxyRequest);
	if (badresponse != null) {
	    statusCode = badresponse.getStatusCode ();
	    // Send response and close
	    sendAndClose (badresponse);
	} else {
	    if (getMeta ())
		handleMeta ();
	    else
	    proxy.getAdaptiveEngine().cacheRequestIfNeeded(this, separator, dataSize);
	}
    }

    /** Handle a meta page.
     */
    private void handleMeta () {
	status = "Handling meta page";
	MetaHandlerHandler mhh = new MetaHandlerHandler ();
	try {
	    mhh.handleMeta (this, proxyRequest, tlh.getProxy (), tlh.getClient ());
	} catch (IOException ex) {
	    logAndClose (null);
	}
    }

    private void checkNoStore (CacheEntry<HttpHeader, HttpHeader> entry) {
	if (entry == null)
	    return;
	List<String> ccs = proxyRequest.getHeaders ("Cache-Control");
	int ccl = ccs.size ();
	for (int i = 0; i < ccl; i++)
	    if (ccs.get (i).equals ("no-store"))
		proxy.getCache ().remove (entry.getKey ());
    }

    private boolean checkMaxAge (RequestHandler rh) {
	return rh.getCond ().checkMaxAge (this, rh.getDataHook (), rh);
    }
    
    public void processRequest(ClientResourceHandler resourceHandler) {
    	this.clientResourceHandler = resourceHandler;
    	handleRequest();
    }

    /** Handle a request by getting the datastream (from the cache or the web).
     *  After getting the handler for the mimetype, send it.
     */
    private void handleRequest () {
	status = "Handling request";
	final RequestHandler rh = new RequestHandler (this);
	if (proxy.getCache ().getMaxSize () > 0) {
	    // memory consistency is guarded by the underlying SynchronousQueue
	    getProxy ().getTaskRunner ().runThreadTask (new Runnable () {
		    public void run () {
			fillInCacheEntries (rh);
		    }
		});
	} else {
	    handleRequestBottom (rh);
	}
    }

    //TODO
    private void fillInCacheEntries (final RequestHandler rh) {
	status = "Handling request - checking cache";
	Cache<HttpHeader, HttpHeader> cache = proxy.getCache ();
	String method = proxyRequest.getMethod ();
	if (!method.equals ("GET") && !method.equals ("HEAD"))
	    cache.remove (proxyRequest);

	rh.setEntry (cache.getEntry (proxyRequest));
	if (rh.getEntry () != null)
	    rh.setDataHook (rh.getEntry ().getDataHook (proxy.getCache ()));

	checkNoStore (rh.getEntry ());
	// Check if cached item is too old
	if (!rh.getCond ().checkMaxStale (proxyRequest, rh) && checkMaxAge (rh))
	    setMayUseCache (false);

	// Add headers to send If-None-Match, or If-Modified-Since
	rh.setConditional (rh.getCond ().checkConditional (this, proxyRequest, rh,
							   mustRevalidate));
	if (partialContent (rh))
	    fillupContent ();
	checkIfRange (rh);

	boolean mc = getMayCache ();
	// in cache?
	if (getMayUseCache () && rh.getEntry () != null) {
	    CacheChecker cc = new CacheChecker ();
	    if (cc.checkCachedEntry (this, proxyRequest, rh))
		return;
	}
	if (rh.getContent () == null) {
	    // Ok cache did not have a usable resource,
	    // reset value to one before we thought we could use cache...
	    mayCache = mc;
	}

	// memory consistency is guarded by the underlying returnedTasksLock
	getProxy ().runMainTask (new Runnable () {
		public void run () {
		    handleRequestBottom (rh);
		}
	    });
    }

    private void handleRequestBottom (final RequestHandler rh) {
	if (rh.getContent () == null) {
	    status = "Handling request - setting up web connection";
	    // no usable cache entry so get the resource from the net.
	    SWC swc = new SWC (this, proxyRequest, tlh, clientResourceHandler, rh);
	    swc.establish ();
	} else {
	    resourceEstablished (rh);
	}
    }

    void webConnectionSetupFailed (RequestHandler rh, Exception cause) {
	if (cause instanceof UnknownHostException)
	    // do we really want this in the log?
	    getLogger ().logWarn (cause.toString ());
	else
	    getLogger ().logWarn ("strange error setting up web connection: " +
				  cause.toString ());
	tryStaleEntry (rh, cause);
    }

    private void setMayCacheFromCC (RequestHandler rh) {
	HttpHeader resp = rh.getWebHeader ();
	for (String val : resp.getHeaders ("Cache-Control")) {
	    if ("public".equals (val)
		|| "must-revalidate".equals (val)
		|| val.startsWith ("s-maxage=")) {
		String auth = proxyRequest.getHeader ("Authorization");
		if (auth != null) {
		    // TODO this ignores no-store and a few other things...
		    mayCache = true;
		    break;
		}
	    }
	}
    }

    /** Check if we must tunnel a request.
     *  Currently will only check if the Authorization starts with NTLM or Negotiate.
     * @param rh the request handler.
     */
    protected boolean mustTunnel (RequestHandler rh) {
	String auth = clientRequest.getHeader ("Authorization");
	return auth != null &&
	    (auth.startsWith ("NTLM") || auth.startsWith ("Negotiate"));
    }

    void webConnectionEstablished (RequestHandler rh) {
	getProxy ().markForPipelining (rh.getWebConnection ());
	setMayCacheFromCC (rh);
	resourceEstablished (rh);
    }

    private void tunnel (RequestHandler rh) {
	status = "Handling request - tunneling";
	try {
	    TunnelDoneListener tdl = new TDL (rh);
	    new Tunnel (selector, getLogger (),
			channel, requestHandle, tlh.getClient (),
			rh.getWebConnection ().getChannel (),
			rh.getWebHandle (), tlh.getNetwork (),
			tdl);
	} catch (IOException ex) {
	    logAndClose (rh);
	}
    }

    private void resourceEstablished (RequestHandler rh) {
	status = "Handling request - got resource";
	try {
		proxy.getAdaptiveEngine().newResponse(this, rh.getWebHeader());
	    // and now we filter the response header if any.
	    if (!clientRequest.isDot9Request ()) {
		if (mustTunnel (rh)) {
		    tunnel (rh);
		    return;
		}

		String status = rh.getWebHeader ().getStatusCode ().trim ();

		// Check if the cached Date header is newer,
		// indicating that we should not cache.
		if (!rh.getCond ().checkStaleCache (proxyRequest, this, rh))
		    setMayCache (false);

		CacheChecker cc = new CacheChecker ();
		cc.removeOtherStaleCaches (proxyRequest, rh.getWebHeader (),
					   proxy.getCache (), getLogger ());
		if (status.equals ("304")) {
		    NotModifiedHandler nmh = new NotModifiedHandler ();
		    nmh.updateHeader (rh, getLogger ());
		    if (rh.getEntry () != null) {
			proxy.getCache ().entryChanged (rh.getEntry (),
					proxyRequest, rh.getDataHook ());
		    }
		}

		// Check that the cache entry has expected header
		// returns null for a good cache entry
		HttpHeader bad =
		    cc.checkExpectations (this, proxyRequest, rh.getWebHeader ());
		if (bad == null) {
		    HttpHeaderFilterer filterer =
			proxy.getHttpHeaderFilterer ();
		    // Run output filters on the header
		    bad = filterer.filterHttpOut (this, channel, rh.getWebHeader ());
		}
		if (bad != null) {
		    // Bad cache entry or this request is blocked
		    rh.getContent ().release ();
		    // Send error response and close
		    sendAndClose (bad);
		    return;
		}

		if (rh.isConditional () && rh.getEntry () != null
		    && status.equals ("304")) {
		    // Try to setup a resource from the cache
		    if (handleConditional (rh)) {
			return;
		    }
		} else if (status.length () > 0) {
		    if (status.equals ("304") || status.equals ("204")
			|| status.charAt (0) == '1') {
			rh.getContent ().release ();
			// Send success response and close
			sendAndClose (rh.getWebHeader ());
			return;
		    }
		}
	    }

	    setHandlerFactory (rh);
	    status = "Handling request - " +
		rh.getHandlerFactory ().getClass ().getName ();
	    Handler handler =
		rh.getHandlerFactory ().getNewInstance (this, tlh,
							proxyRequest, requestHandle,
							rh.getWebHeader (),
							rh.getContent (),
							getMayCache (),
							getMayFilter (),
							rh.getSize ());
	    if (handler == null) {
		doError (500, "Something fishy with that handler....");
	    } else {
		finalFixesOnWebHeader (rh, handler);
		// HTTP/0.9 does not support HEAD, so webheader should be valid.
		if (clientRequest.isHeadOnlyRequest ()) {
		    rh.getContent ().release ();
		    sendAndRestart (rh.getWebHeader ());
		} else {
			final Handler hndlr = handler;
			proxy.getAdaptiveEngine().processResponse(this,new Runnable() {
				@Override
				public void run() {
					try {
						hndlr.handle ();
			    	 } catch (Throwable e) {
			    		 handleInternalError(e);
					}
				}
			});
		}
	    }
	} catch (Throwable t) {
	    handleInternalError (t);
	}
    }
    
    private void finalFixesOnWebHeader (RequestHandler rh, Handler handler) {
	if (chunk) {
	    if (rh.getSize () < 0 || handler.changesContentSize ()) {
		rh.getWebHeader ().removeHeader ("Content-Length");
		rh.getWebHeader ().setHeader ("Transfer-Encoding", "chunked");
	    } else {
		setChunking (false);
	    }
	} else {
	    if (getKeepalive ()) {
		rh.getWebHeader ().setHeader ("Proxy-Connection", "Keep-Alive");
		rh.getWebHeader ().setHeader ("Connection", "Keep-Alive");
	    } else {
		rh.getWebHeader ().setHeader ("Proxy-Connection", "close");
		rh.getWebHeader ().setHeader ("Connection", "close");
	    }
	}
    }

    private void setHandlerFactory (RequestHandler rh) {
	if (rh.getHandlerFactory () == null) {
	    String ct = null;
	    if (rh.getWebHeader () != null) {
		ct = rh.getWebHeader ().getHeader ("Content-Type");
		if (ct != null) {
		    ct = ct.toLowerCase ();
		    // remove some white spaces for easier configuration.
		    // "text/html; charset=iso-8859-1"
		    // "text/html;charset=iso-8859-1"
		    ct = ct.replace ("; ", ";");
		    if (getMayFilter ())
			rh.setHandlerFactory (proxy.getHandlerFactory (ct));
		    if (rh.getHandlerFactory () == null
			&& ct.startsWith ("multipart/byteranges"))
			rh.setHandlerFactory (new MultiPartHandler ());
		}
	    }
	    if (rh.getHandlerFactory () == null) {              // still null
		getLogger ().logInfo ("Using BaseHandler for " + ct);
		rh.setHandlerFactory (new BaseHandler ());   // fallback...
	    }
	}
    }

    private boolean handleConditional (RequestHandler rh) throws IOException {
	HttpHeader cachedHeader = rh.getDataHook ();
	proxy.releaseWebConnection (rh.getWebConnection ());
	if (addedINM)
		proxyRequest.removeHeader ("If-None-Match");
	if (addedIMS)
		proxyRequest.removeHeader ("If-Modified-Since");

	if (checkWeakEtag (cachedHeader, rh.getWebHeader ())) {
	    NotModifiedHandler nmh = new NotModifiedHandler ();
	    nmh.updateHeader (rh, getLogger ());
	    setMayCache (false);
	    try {
		HttpHeader res304 = nmh.is304 (proxyRequest, this, rh);
		if (res304 != null) {
		    sendAndClose (res304);
		    return true;
		}
		if (rh.getContent () != null)
		    rh.getContent ().release ();
		// Try to setup a resource from the cache
		setupCachedEntry (rh);
	    } catch (IOException e) {
		getLogger ().logWarn ("Conditional request: IOException (" +
				proxyRequest.getRequestURI () + ",: " + e);
	    }
	} else {
	    // retry...
	    proxyRequest.removeHeader ("If-None-Match");
	    proxy.getCache ().remove (proxyRequest);
	    handleRequest ();
	    return true;
	}

	// send the cached entry.
	return false;
    }

    private class TDL implements TunnelDoneListener {
	private RequestHandler rh;

	public TDL (RequestHandler rh) {
	    this.rh = rh;
	}

	public void tunnelClosed () {
	    logAndClose (rh);
	}
    }

    private void tryStaleEntry (RequestHandler rh, Exception e) {
	// do we have a stale entry?
	if (rh.getEntry () != null && rh.isConditional () && !mustRevalidate)
	    handleStaleEntry (rh);
	else
	    doError (504, e);
    }

    private void handleStaleEntry (RequestHandler rh) {
	setMayCache (false);
	try {
	    setupCachedEntry (rh);
	    rh.getWebHeader ().addHeader ("Warning",
				    "110 RabbIT \"Response is stale\"");
	    resourceEstablished (rh);
	} catch (IOException ex) {
	    doError (504, ex);
	    return;
	}
    }

    // Setup a resource from the cache
    HttpHeader setupCachedEntry (RequestHandler rh) throws IOException {
	SCC swc = new SCC (this, proxyRequest, rh);
	HttpHeader ret = swc.establish ();
	return ret;
    }

    private ContentSeparator setupChunkedContent () throws IOException {
	status = "Request read, reading chunked data";
	setMayUseCache (false);
	setMayCache (false);
	return new ChunkSeparator(getProxy().getStrictHttp());
    }

    private ContentSeparator setupClientResourceHandler (long dataSize) {
	status = "Request read, reading client resource data";
	setMayUseCache (false);
	setMayCache (false);
	return new FixedLengthSeparator(dataSize);
    }

    private ContentSeparator readMultiPart (String ct) {
	status = "Request read, reading multipart data";
	// Content-Type: multipart/byteranges; boundary=B-qpuvxclkeavxeywbqupw
	if (ct.startsWith ("multipart/byteranges")) {
	    setMayUseCache (false);
	    setMayCache (false);

	    return new MultiPipeSeparator(ct);
	}
	return null;
    }
    
    public void setClientResourceHandler(ClientResourceHandler handler) {
		
	}

    private boolean partialContent (RequestHandler rh) {
	if (rh.getEntry () == null)
	    return false;
	String method = proxyRequest.getMethod ();
	if (!method.equals ("GET"))
	    return false;
	HttpHeader resp = rh.getDataHook ();
	String realLength = resp.getHeader ("RabbIT-Partial");
	return (realLength != null);
    }

    private void fillupContent () {
	setMayUseCache (false);
	setMayCache (true);
	// TODO: if the need arise, think about implementing smart partial updates.
    }

    private void checkIfRange (RequestHandler rh) {
	if (rh.getEntry () == null)
	    return;
	String ifRange = proxyRequest.getHeader ("If-Range");
	if (ifRange == null)
	    return;
	String range = proxyRequest.getHeader ("Range");
	if (range == null)
	    return;
	Date d = HttpDateParser.getDate (ifRange);
	HttpHeader oldresp = rh.getDataHook ();
	if (d == null) {
	    // we have an etag...
	    String etag = oldresp.getHeader ("Etag");
	    if (etag == null || !checkWeakEtag (etag, ifRange))
		setMayUseCache (false);
	}
    }

    /** Send an error (400 Bad Request) to the client.
     * @param status the status code of the error.
     * @param message the error message to tell the client.
     */
    void doError (int status, String message) {
	this.statusCode = Integer.toString (status);
	HttpHeader header = responseHandler.getHeader ("HTTP/1.0 400 Bad Request");
	StringBuilder error =
	    new StringBuilder (HtmlPage.getPageHeader (this, "400 Bad Request") +
			       "Unable to handle request:<br><b>" +
			       message +
			       "</b></body></html>\n");
	header.setContent (error.toString ());
	sendAndClose (header);
    }

    /** Send an error (400 Bad Request or 504) to the client.
     * @param statuscode the status code of the error.
     * @param e the exception to tell the client.
     */
    private void doError (int statuscode, Exception e) {
	String message = "";
	boolean dnsError = (e instanceof UnknownHostException);
	this.statusCode = Integer.toString (statuscode);
	extraInfo = (extraInfo != null ?
		     extraInfo + e.toString () :
		     e.toString ());
	HttpHeader header = null;
	if (!dnsError) {
	    StringWriter sw = new StringWriter ();
	    PrintWriter ps = new PrintWriter (sw);
	    e.printStackTrace (ps);
	    message = sw.toString ();
	}
	if (statuscode == 504)
	    header = getHttpGenerator ().get504 (e, requestLine);
	else
	    header = getHttpGenerator ().getHeader ("HTTP/1.0 400 Bad Request");

	StringBuilder sb = new StringBuilder ();
	sb.append (HtmlPage.getPageHeader (this, statuscode + " " +
					   header.getReasonPhrase ()));
	if (dnsError)
	    sb.append ("Server not found");
	else
	    sb.append ("Unable to handle request");
	sb.append (":<br><b>" + e.getMessage () +
		   (header.getContent () != null ?
		    "<br>" + header.getContent () :
		    "") +
		   "</b><br><xmp>" + message + "</xmp></body></html>\n");
	header.setContent (sb.toString ());
	sendAndClose (header);
    }

    private void checkAndHandleSSL (BufferHandle bh) {
	status = "Handling ssl request";
	SSLHandler sslh = new SSLHandler (proxy, this, proxyRequest, tlh);
	if (sslh.isAllowed ()) {
	    sslh.handle (channel, selector, bh);
	} else {
	    HttpHeader badresponse = responseHandler.get403 ();
	    sendAndClose (badresponse);
	}
    }

    public SocketChannel getChannel () {
	return channel;
    }

    public Selector getSelector () {
	return selector;
    }

    public HttpProxy getProxy () {
	return proxy;
    }

    public BufferHandler getBufferHandler () {
	return bufHandler;
    }

    private void closeDown () {
    proxy.getAdaptiveEngine().getEventsHandler().logProxyClosedCon(this);
    proxy.getAdaptiveEngine().connectionClosed(this);
	try {
	    channel.close ();
	} catch (IOException e) {
	    getLogger ().logWarn ("Failed to close down connection: " + e);
	}
	if (!requestHandle.isEmpty ()) {
	    // empty the buffer...
	    ByteBuffer buf = requestHandle.getBuffer ();
	    buf.position (buf.limit ());
	}
	requestHandle.possiblyFlush ();
	proxy.removeCurrentConnection (this);
    }

    public Logger getLogger () {
	return proxy.getLogger ();
    }

    private ConnectionLogger getConnectionLogger () {
	return proxy.getConnectionLogger ();
    }

    Counter getCounter () {
	return proxy.getCounter ();
    }

    /** Resets the statuses for this connection.
     */
    private void clearStatuses () {
	status         = "Reading request";
	started        = System.currentTimeMillis ();
	clientRequest        = null;
	keepalive      = true;
	meta           = false;
	chunk          = true;
	mayUseCache    = true;
	mayCache       = true;
	mayFilter      = true;
	mustRevalidate = false;
	addedINM       = false;
	addedIMS       = false;
	userName       = null;
	password       = null;
	requestLine    = "?";
	statusCode     = "200";
	extraInfo      = null;
	contentLength  = "-";
	clientResourceHandler = null;
    }

    /** Set keepalive to a new value. Note that keepalive can only be
     *	promoted down.
     * @param keepalive the new keepalive value.
     */
    public void setKeepalive (boolean keepalive) {
	this.keepalive = (this.keepalive && keepalive);
    }

    /** Get the keepalive value.
     * @return true if keepalive should be done, false otherwise.
     */
    private boolean getKeepalive () {
	return keepalive;
    }

    public String getUserName () {
	return userName;
    }

    public void setUserName (String userName) {
	this.userName = userName;
    }

    public String getPassword () {
	return password;
    }

    public void setPassword (String password) {
	this.password = password;
    }

    // For logging and status
    public String getRequestLine () {
	return requestLine;
    }

    /** Get the current request uri.
     *  This will get the uri from the request header.
     */
    public String getRequestURI () {
	return clientRequest.getRequestURI();
    }

    // Get debug info for use in 500 error response
    String getDebugInfo () {
	return
	    "status: " + getStatus ()  + "\n" +
	    "started: " + new Date (getStarted ()) + "\n" +
	    "keepalive: " + getKeepalive () + "\n" +
	    "meta: " + getMeta () + "\n" +
	    "mayusecache: " + getMayUseCache () + "\n" +
	    "maycache: " + getMayCache () + "\n" +
	    "mayfilter: " + getMayFilter () + "\n"+
	    "requestline: " + getRequestLine () + "\n" +
	    "statuscode: " + getStatusCode () + "\n" +
	    "extrainfo: " + getExtraInfo () + "\n" +
	    "contentlength: " + getContentLength () + "\n";
    }

    /** Get the http version that the client used.
     *  We modify the request header to hold HTTP/1.1 since that is
     *  what rabbit uses, but the real client may have sent a 1.0 header.
     */
    String getRequestVersion () {
	return requestVersion;
    }

    // For logging and status
    public String getStatus () {
	return status;
    }

    // For logging
    String getStatusCode () {
	return statusCode;
    }

    public String getContentLength () {
	return contentLength;
    }

    public String getExtraInfo () {
	return extraInfo;
    }

    /** Set the extra info.
     * @param info the new info.
     */
    public void setExtraInfo (String info) {
	this.extraInfo = info;
    }

    /** Get the time this connection was started. */
    public long getStarted () {
	return started;
    }

    /** Set the chunking option.
     * @param b if true this connection should use chunking.
     */
    public void setChunking (boolean b) {
	chunk = b;
    }

    /** Get the chunking option.
     * @return if this connection is using chunking.
     */
    public boolean getChunking () {
	return chunk;
    }

    /** Get the state of this request.
     * @return true if this is a metapage request, false otherwise.
     */
    public boolean getMeta () {
	return meta;
    }

    /** Set the state of this request.
     * @param meta true if this request is a metapage request, false otherwise.
     */
    public void setMeta (boolean meta) {
	this.meta = meta;
    }

    /** Set the state of this request. This can only be promoted down..
     * @param useCache true if we may use the cache for this request,
     *        false otherwise.
     */
    public void setMayUseCache (boolean useCache) {
	mayUseCache = mayUseCache && useCache;
    }

    /** Get the state of this request.
     * @return true if we may use the cache for this request, false otherwise.
     */
    private boolean getMayUseCache () {
	return mayUseCache;
    }

    /** Set the state of this request. This can only be promoted down.
     * @param cacheAllowed true if we may cache the response, false otherwise.
     */
    public void setMayCache (boolean cacheAllowed) {
	mayCache = cacheAllowed && mayCache;
    }

    /** Get the state of this request.
     * @return true if we may cache the response, false otherwise.
     */
    public boolean getMayCache () {
	return mayCache;
    }

    /** Get the state of this request. This can only be promoted down.
     * @param filterAllowed true if we may filter the response, false otherwise.
     */
    public void setMayFilter (boolean filterAllowed) {
	mayFilter = filterAllowed && mayFilter;
    }

    /** Get the state of the request.
     * @return true if we may filter the response, false otherwise.
     */
    public boolean getMayFilter () {
	return mayFilter;
    }

    void setAddedINM (boolean b) {
	addedINM = b;
    }

    void setAddedIMS (boolean b) {
	addedIMS = b;
    }

    public void setMustRevalidate (boolean b) {
	mustRevalidate = b;
    }

    /** Set the content length of the response.
     * @param contentLength the new content length.
     */
    public void setContentLength (String contentLength) {
	this.contentLength = contentLength;
    }

    public void setStatusCode (String statusCode) {
	this.statusCode = statusCode;
    }

    // Set status and content length
    private void setStatusesFromHeader (HttpHeader header) {
	statusCode = header.getStatusCode ();
	String cl = header.getHeader ("Content-Length");
	if (cl != null)
	    contentLength  = cl;
    }

    void sendAndRestart (final HttpHeader header) {
	status = "Sending response.";
	setStatusesFromHeader (header);
	if (!keepalive) {
	    sendAndClose (header);
	} else {
		proxy.getAdaptiveEngine().newProxyResponse(this, header, new Runnable() {
			@Override
			public void run() {
				HttpHeaderSentListener sar = new SendAndRestartListener ();
			    try {
				new HttpHeaderSender (channel, selector, getLogger (),
						      tlh.getClient (), header, false, sar);
			    } catch (IOException e) {
				getLogger ().logWarn ("IOException when sending header: " + e);
				closeDown ();
			    }
			}
		});
	}
    }

    boolean useFullURI () {
	return proxy.isProxyConnected ();
    }

    private class SendAndRestartListener implements HttpHeaderSentListener {
	public void httpHeaderSent () {
	    logConnection ();
	    readRequest ();
	}

	public void timeout () {
	    getLogger ().logInfo ("Timeout when sending http header");
	    logAndClose (null);
	}

	public void failed (Exception e) {
	    getLogger ().logInfo ("Exception when sending http header: " + e);
	    logAndClose (null);
	}
    }

    // Send response and close
    void sendAndClose (final HttpHeader header) {
	status = "Sending response and closing.";
	// Set status and content length
	setStatusesFromHeader (header);
	keepalive = false;
	final HttpHeaderSentListener scl = new SendAndCloseListener ();
	proxy.getAdaptiveEngine().newProxyResponse(this, header, new Runnable() {
		@Override
		public void run() {
			try {
			    new HttpHeaderSender (channel, selector, getLogger (),
						  tlh.getClient (), header, false, scl);
			} catch (IOException e) {
			    getLogger ().logWarn ("IOException when sending header: " + e);
			    closeDown ();
			}
		}
	});
    }

    public void logAndClose (RequestHandler rh) {
	if (rh != null && rh.getWebConnection () != null) {
	    proxy.releaseWebConnection (rh.getWebConnection ());
	}
	logConnection ();
	closeDown ();
    }

    public void logAndRestart () {
	logConnection ();
	if (getKeepalive ())
	    readRequest ();
	else
	    closeDown ();
    }

    private class SendAndCloseListener implements HttpHeaderSentListener {
	public void httpHeaderSent () {
	    logAndClose (null);
	}

	public void timeout () {
	    getLogger ().logInfo ("Timeout when sending http header");
	    logAndClose (null);
	}

	public void failed (Exception e) {
	    getLogger ().logInfo ("Exception when sending http header: " + e);
	    logAndClose (null);
	}
    }

    protected boolean isWeak (String t) {
	return t.startsWith ("W/");
    }

    protected boolean checkStrongEtag (String et, String im) {
	return !isWeak (im) && im.equals (et);
    }

    /* Remove any W/ prefix then check if etags are equal.
     * Inputs can be in any order.
     * @return true if the etags match or at least one of the etag
     *              headers do not exist.
     */
    private boolean checkWeakEtag (HttpHeader h1, HttpHeader h2) {
	String et1 = h1.getHeader ("Etag");
	String et2 = h2.getHeader ("Etag");
	if (et1 == null || et2 == null)
	    return true;
	return checkWeakEtag (et1, et2);
    }

    /* Remove any W/ prefix from the inputs then check if they are equal.
     * Inputs can be in any order.
     * Returns true if equal.
     */
    protected boolean checkWeakEtag (String et, String im) {
	if (et == null || im == null)
	    return false;
	if (isWeak (et))
	    et = et.substring (2);
	if (isWeak (im))
	    im = im.substring (2);
	return im.equals (et);
    }

    public HttpGenerator getHttpGenerator () {
	return responseHandler;
    }

    private void logConnection () {
	getConnectionLogger ().logConnection (Connection.this);
	proxy.updateTrafficLog (tlh);
	tlh.clear ();
    }
    
    public HttpHeader getClientRHeader() {
    	return clientRequest;
    }
    
    public void setProxyRHeader(HttpHeader proxyRQHeaders) {
    	proxyRequest = proxyRQHeaders;
    }
    
    public BufferHandle getRequestBufferHandle() {
    	return requestHandle;
    }
    
    public TrafficLoggerHandler getTrafficLoggerHandler() {
    	return tlh;
    }
    
    public HttpHeader filterConstructedResponse(HttpHeader header) {
    	return proxy.getHttpHeaderFilterer().filterHttpOut(this, channel, header);
    }
}
