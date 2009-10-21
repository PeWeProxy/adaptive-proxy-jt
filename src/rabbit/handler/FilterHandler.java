package rabbit.handler;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import rabbit.filter.HtmlFilter;
import rabbit.filter.HtmlFilterFactory;
import rabbit.html.HtmlBlock;
import rabbit.html.HtmlParseException;
import rabbit.html.HtmlParser;
import rabbit.http.HttpHeader;
import rabbit.httpio.ResourceSource;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;
import rabbit.util.CharsetDetector;
import rabbit.util.Logger;
import rabbit.util.SProperties;
import rabbit.zip.GZipUnpackListener;
import rabbit.zip.GZipUnpacker;

/** This handler filters out unwanted html features.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class FilterHandler extends GZipHandler {
	protected List<HtmlFilterFactory> filterClasses = 
    new ArrayList<HtmlFilterFactory> ();
    protected boolean repack = false;
    private String defaultCharSet = null;
    private String overrideCharSet = null;

    private List<HtmlFilter> filters; 
    private HtmlParser parser;
    private byte[] restBlock = null;
    private boolean sendingRest = false;
    protected Iterator<ByteBuffer> sendBlocks = null;

    protected GZipUnpacker gzu = null;
    private GZListener gzListener = null;
    
    // For creating the factory.
    public FilterHandler () {	
    }

    /** Create a new FilterHandler for the given request.
     * @param con the Connection handling the request.
     * @param request the actual request made.
     * @param clientHandle the client side buffer handle.
     * @param response the actual response.
     * @param content the resource.
     * @param mayCache May we cache this request? 
     * @param mayFilter May we filter this request?
     * @param size the size of the data beeing handled.
     * @param compress if we want this handler to compress or not.
     */
    public FilterHandler (Connection con, TrafficLoggerHandler tlh, 
			  HttpHeader request, BufferHandle clientHandle,
			  HttpHeader response, ResourceSource content, 
			  boolean mayCache, boolean mayFilter, long size, 
			  boolean compress, boolean repack,
			  List<HtmlFilterFactory> filterClasses) {
	super (con, tlh, request, clientHandle, response, content, 
	       mayCache, mayFilter, size, compress);
	this.repack = repack;
	this.filterClasses = filterClasses;
    }

    @Override 
    protected void setupHandler () {
	String ce = response.getHeader ("Content-Encoding");
	if (repack && ce != null) {
	    ce = ce.toLowerCase ();
	    if (ce.equals ("gzip")) {
		gzListener = new GZListener ();
		gzu = new GZipUnpacker (gzListener, false);		
	    } else if( ce.equals("deflate")) {
		gzListener = new GZListener ();
		gzu = new GZipUnpacker (gzListener, true);
	    } else {
		getLogger ().logWarn ("Encloding: " + ce);
	    }
	}

	super.setupHandler ();
	if (mayFilter) {
	    response.removeHeader ("Content-Length");

	    String cs = null;
	    if (overrideCharSet != null) {
	    	cs = overrideCharSet;
	    } else {
	    	cs = CharsetDetector.detectCharsetString(response); 
	    	if (cs == null)
	    		cs = defaultCharSet;
	    }
	    // There are lots of other charsets, and it could be specified by a 
	    // HTML Meta tag.  
	    // And it might be specified incorrectly for the actual page.
	    // http://www.w3.org/International/O-HTTP-charset
	    
 	    // default fron conf file 
 	    //	    then look for HTTP charset
 	    //	    then look for HTML Meta charset, maybe re-decode
	    // <META content="text/html; charset=gb2312" http-equiv=Content-Type>
	    // <meta http-equiv="content-type" content="text/html;charset=Shift_JIS" />
	    Charset charSet = null;
	    if (cs != null) {
	    	try {
	    		charSet = Charset.forName (cs);
	    	    } catch (UnsupportedCharsetException e) {
	    		getLogger ().logInfo ("Bad CharSet: " + cs);
	    	    }
		}
	    if (charSet == null)
			charSet = Charset.forName ("ISO-8859-1");
	    parser = new HtmlParser (charSet);
	    filters = initFilters ();
	}
    }

    @Override 
    protected boolean willCompress () {
	return gzu != null || super.willCompress ();
    }
    
    private class GZListener implements GZipUnpackListener {
	private byte[] buffer;
	public void unpacked (byte[] buf, int off, int len) {
	    handleArray (buf, off, len);
	}
	
	public void finished () {
	    gzu = null;
	    finishData ();
	}

	public byte[] getBuffer () {
	    if (buffer == null)
		buffer = new byte[4096];
	    return buffer;
	}

	public void failed (Exception e) {
	    FilterHandler.this.failed (e);
	}
    }
    
    @Override
    public Handler getNewInstance (Connection con, TrafficLoggerHandler tlh,
				   HttpHeader header, BufferHandle bufHandle, 
				   HttpHeader webHeader, 
				   ResourceSource content, boolean mayCache, 
				   boolean mayFilter, long size) {
	FilterHandler h = 
	    new FilterHandler (con, tlh, header, bufHandle, webHeader, 
			       content, mayCache, mayFilter, size, 
			       compress, repack, filterClasses);
	h.defaultCharSet = defaultCharSet;
	h.overrideCharSet = overrideCharSet;
	h.setupHandler ();
	return h;
    }

    @Override
    protected void writeDataToGZipper (byte[] arr) {
	forwardArrayToHandler (arr, 0, arr.length);
    }

    @Override
    protected void modifyBuffer (BufferHandle bufHandle) {
	if (!mayFilter) {
	    super.modifyBuffer (bufHandle);
	    return;
	}
	ByteBuffer buf = bufHandle.getBuffer ();
	byte[] arr; 
	if (buf.isDirect ()) {
	    arr = new byte[buf.remaining ()];
	    buf.get (arr);
	} else {
	    arr = buf.array ();
	}
	bufHandle.possiblyFlush ();
	//** REDEEMER ** Are we sure with offset = 0 ?! buffer.position() can be gr8ter
	// and buf.array () returns >whole< backed buffer, not just a part marked as buffer's
	// remaining section
	forwardArrayToHandler (arr, 0, arr.length);
    }

    private void forwardArrayToHandler (byte[] arr, int off, int len) {
	if (gzu != null) {
	    gzu.setInput (arr, off, len);
	    if ((sendBlocks == null || !sendBlocks.hasNext ()) && 
		(gzu != null && gzu.needsInput ()))
		waitForData ();
	} else {
	    handleArray (arr, off, len);
	}
    }

    protected void handleArray (byte[] arr, int off, int len) {
	if (restBlock != null) {
	    int rs = restBlock.length;
	    int newLen = len + rs;
	    byte[] buf = new byte[newLen];
	    System.arraycopy (restBlock, 0, buf, 0, rs);
	    System.arraycopy (arr, off, buf, rs, len);
	    arr = buf;
	    off = 0;
	    len = newLen;
	    restBlock = null;
	}
	parser.setText (arr, off, len);
	HtmlBlock currentBlock = null;
	try {
	    currentBlock = parser.parse ();
	    for (HtmlFilter hf : filters) {
		hf.filterHtml (currentBlock);
		if (!hf.isCacheable ()) {
		    mayCache = false;
		    removeCache ();
		}
	    }
	    
	    List<ByteBuffer> ls = currentBlock.getBlocks (); 
	    if (currentBlock.hasRests ()) {
		// since the unpacking buffer is re used we need to store the 
		// rest in a separate buffer.
		restBlock = currentBlock.getRestBlock ();
	    }
	    sendBlocks = ls.iterator ();
	} catch (HtmlParseException e) {
	    getLogger ().logInfo ("Bad HTML: " + e.toString ());
	    // out.write (arr);
	    currentBlock = null;
	    ByteBuffer buf = ByteBuffer.wrap (arr, off, len);
	    sendBlocks = Arrays.asList (buf).iterator ();
	}
	if (sendBlocks.hasNext ()) {
	    sendBlockBuffers ();
	} else {
	    // no more blocks so wait for more data, either from 
	    // gzip or the net
	    blockSent ();
	}
    }

    @Override public void blockSent () {
	if (sendingRest) {
	    super.finishData ();
	} else if (sendBlocks != null && sendBlocks.hasNext ()) {
	    sendBlockBuffers (); 
	} else if (gzu != null && !gzu.needsInput ()) {
	    gzu.handleCurrentData ();
	} else {
	    super.blockSent ();
	}
    }

    protected void sendBlockBuffers () {
	ByteBuffer buf = sendBlocks.next ();
	SimpleBufferHandle bh = new SimpleBufferHandle (buf);
	send (bh);
    }

    @Override
    protected void finishData ()  {
	if (restBlock != null && restBlock.length > 0) {
	    ByteBuffer buf = ByteBuffer.wrap (restBlock);
	    SimpleBufferHandle bh = new SimpleBufferHandle (buf);
	    restBlock = null;
	    sendingRest = true;
	    send (bh);
	} else {
	    super.finishData ();
	}
    }
    
    /** Initialize the filter we are using.
     * @return a List of HtmlFilters.
     */
    private List<HtmlFilter> initFilters () {
	int fsize = filterClasses.size ();
	List<HtmlFilter> fl = new ArrayList<HtmlFilter> (fsize);	

	for (int i = 0; i < fsize; i++) {
	    HtmlFilterFactory hff = filterClasses.get (i);
	    fl.add (hff.newFilter (con, request, response));
	}
	return fl;
    }

    /** Setup this class.
     * @param prop the properties of this class.
     */
    @Override public void setup (Logger logger, SProperties prop) {
	super.setup (logger, prop);
	defaultCharSet = prop.getProperty ("defaultCharSet", "ISO-8859-1");
	overrideCharSet = prop.getProperty ("overrideCharSet");
	String rp = prop.getProperty ("repack", "false");
	repack = Boolean.parseBoolean (rp);
	String fs = prop.getProperty ("filters", "");
	if ("".equals (fs))
	    return;
	String[] names = fs.split (",");
	for (String classname : names) {
	    try {
		Class<? extends HtmlFilterFactory> cls = 
		    Class.forName (classname).
		    asSubclass (HtmlFilterFactory.class);
		filterClasses.add (cls.newInstance ());
	    } catch (ClassNotFoundException e) {
		logger.logWarn ("Could not find filter: '" + classname + "'");
	    } catch (InstantiationException e) {
		logger.logWarn ("Could not instanciate class: '" + 
				classname + "' " + e);
	    } catch (IllegalAccessException e) {
		logger.logWarn ("Could not get constructor for: '" + 
				classname + "' " + e);
	    }
	}
    }
}
