package rabbit.handler;

import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Locale;

import rabbit.http.HttpHeader;
import rabbit.httpio.ResourceSource;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;
import rabbit.util.SProperties;
import rabbit.zip.GZipPackListener;
import rabbit.zip.GZipPacker;

/** This handler compresses the data passing through it.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class GZipHandler extends BaseHandler {
    protected boolean compress = true;
    protected boolean isCompressing = false;
    private boolean compressionFinished = false;
    private boolean compressedDataFinished = false;
    private GZipPacker packer = null;
    
    private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

    /** For creating the factory.
     */
    public GZipHandler () {
	// empty
    }

    /** Create a new GZipHandler for the given request.
     * @param con the Connection handling the request.
     * @param tlh the TrafficLoggerHandler to update with traffic information
     * @param request the actual request made.
     * @param response the actual response.
     * @param content the resource.
     * @param mayCache May we cache this request?
     * @param mayFilter May we filter this request?
     * @param size the size of the data beeing handled.
     * @param compress if we want this handler to compress or not.
     */
    public GZipHandler (Connection con, TrafficLoggerHandler tlh,
			HttpHeader request, HttpHeader response,
			ResourceSource content, boolean mayCache,
			boolean mayFilter, long size, boolean compress) {
	super (con, tlh, request, response, content,
	       mayCache, mayFilter, size);
	this.compress = compress;
    }

    protected void setupHandler () {
	if (compress) {
		boolean seeUnpacked = seeUnpackedData ();
	    isCompressing = doesClientAcceptGzip() && seeUnpacked;
	    if (isCompressing) {
		response.removeHeader ("Content-Length");
		response.setHeader ("Content-Encoding", "gzip");
		if (!con.getChunking ())
		    con.setKeepalive (false);
	    } else {
	    // TODO podla vsetkeho by odstranenie tohto riadku malo
	    // umoznit filtrovat data bez vedlajsich ucinkov
		    if (!seeUnpacked)
		    	mayFilter = false;
	    }
	}
    }
    
    private boolean doesClientAcceptGzip() {
    	/*
    	 * Accept-Encoding: compress, gzip
    	 * Accept-Encoding:
    	 * Accept-Encoding: *
    	 * Accept-Encoding: compress;q=0.5, gzip;q=1.0
    	 * Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
    	 */
    	boolean undirectAccept = false;
    	Iterator<String> iter = con.getClientRHeader().getHeaders("Accept-Encoding").iterator();
    	if (!iter.hasNext()) {
    		/* RFC 2616:
    		 * "If no Accept-Encoding field is present in a request, the server MAY
    		 * assume that the client will accept any content coding."
    		 */
			//return true;
    		
    		/* FireFox corrupts responses when it's Accept-Encoding config value is cleared
    		 * and it does not include any Accept-Encoding field in request
    		 */
    		return false;
		}
    	while(iter.hasNext()) {
    		String prefs = iter.next();
    		Boolean gzipAccept = isEncAccepted(prefs, "gzip");
    		if (gzipAccept != null)
    			return gzipAccept.booleanValue();
    		Boolean otherAccept = isEncAccepted(prefs, "*");
    		if (otherAccept != null) {
    			undirectAccept = !otherAccept;
    		}
    	}
    	return undirectAccept;
    }
    
    private Boolean isEncAccepted(String prefs, String enc) {
    	int index = -1;
    	if ((index = prefs.indexOf(enc)) >= 0) {
			index += enc.length();
			String following = prefs.substring(index).trim();
			if (following.length() == 0)
				return true;
			char nextChar = following.charAt(0);
	    	if (nextChar == ',')
	    		return true;
	    	if (nextChar == ';') {
	    		// cut ;q=
	    		following = following.substring(3);
	    		int end = following.indexOf(',');
	    		if (end < 0)
	    			end = following.length();
	    		following = following.substring(0, end);
	    		try {
	    			return nf.parse(following).doubleValue() > 0;
				} catch (ParseException ignored) {}
			}
    	}
    	return null;
    }
    
    protected boolean seeUnpackedData () {
	String ce = response.getHeader ("Content-Encoding");
	if (ce == null)
	    return true;
	ce = ce.toLowerCase ();
	return !(ce.equals ("gzip") || ce.equals ("deflate"));
    }

    @Override
    public Handler getNewInstance (Connection con, TrafficLoggerHandler tlh,
				   HttpHeader header, HttpHeader webHeader,
				   ResourceSource content, boolean mayCache,
				   boolean mayFilter, long size) {
	GZipHandler h =
	    new GZipHandler (con, tlh, header, webHeader,
			     content, mayCache, mayFilter, size,
			     compress && mayFilter);
	h.setupHandler ();
	return h;
    }

    /**
     * @return true this handler modifies the content.
     */
    @Override public boolean changesContentSize () {
	return true;
    }

    @Override
    protected void prepare () {
	if (isCompressing) {
	    GZipPackListener pl = new PListener ();
	    packer = new GZipPacker (pl);
	    if (!packer.needsInput ())
		packer.handleCurrentData ();
	    else
		super.prepare ();
	} else {
	    super.prepare ();
	}
    }

    private class PListener implements GZipPackListener {
	private byte[] buffer;

	public byte[] getBuffer () {
	    if (buffer == null)
		buffer = new byte[4096];
	    return buffer;
	}

	public void packed (byte[] buf, int off, int len) {
	    if (len > 0) {
		ByteBuffer bb = ByteBuffer.wrap (buf, off, len);
		BufferHandle bufHandle = new SimpleBufferHandle (bb);
		GZipHandler.super.bufferRead (bufHandle);
	    } else {
		blockSent ();
	    }
	}

	public void dataPacked () {
	    // do not really care...
	}

	public void finished () {
	    compressedDataFinished = true;
	}

	public void failed (Exception e) {
	    GZipHandler.this.failed (e);
	}
    }

    @Override
    protected void finishData () {
	if (isCompressing) {
	    packer.finish ();
	    compressionFinished = true;
	    sendEndBuffers ();
	} else {
	    super.finishData ();
	}
    }

    private void sendEndBuffers () {
	if (packer.finished ()) {
	    super.finishData ();
	} else {
	    packer.handleCurrentData ();
	}
    }

    /** Check if this handler supports direct transfers.
     * @return this handler always return false.
     */
    @Override
    protected boolean mayTransfer () {
	return false;
    }

    @Override
    public void blockSent () {
	if (packer == null)
	    super.blockSent ();
	else if (compressedDataFinished)
	    super.finishData ();
	else if (compressionFinished)
	    sendEndBuffers ();
	else if (packer.needsInput ())
	    waitForData ();
	else
	    packer.handleCurrentData ();
    }

    protected void waitForData () {
	requestMoreData();
    }

    /** Write the current block of data to the gzipper.
     *  If you override this method you probably want to override
     *  the modifyBuffer(ByteBuffer) as well.
     * @param arr the data to write to the gzip stream.
     */
    protected void writeDataToGZipper (byte[] arr, int offset, int length) {
	packer.setInput (arr, offset, length);
	if (packer.needsInput ())
	    waitForData ();
	else
	    packer.handleCurrentData ();
    }

    /** This method is used when we are not compressing data.
     *  This method will just call "super.bufferRead (buf);"
     * @param bufHandle the handle to the buffer that just was read.
     */
    protected void modifyBuffer (BufferHandle bufHandle) {
	super.bufferRead (bufHandle);
    }

    protected void send (BufferHandle bufHandle) {
	if (isCompressing && !bufHandle.isEmpty()) {
	    ByteBuffer buf = bufHandle.getBuffer ();
	    byte[] arr = buf.array ();
	    int pos = buf.position ();
	    int len = buf.remaining ();
	    packer.setInput (arr, pos, len);
	    if (!packer.needsInput ())
		packer.handleCurrentData ();
	    else
		blockSent ();
	} else {
	    super.bufferRead (bufHandle);
	}
    }

    @Override
    public void bufferRead (BufferHandle bufHandle) {
	if (con == null) {
	    // not sure why this can happen, client has closed connection?
	    return;
	}
	if (isCompressing) {
	    // we normally have direct buffers and we can not use
	    // array() on them. Create a new byte[] and copy data into it.
	    byte[] arr;
	    ByteBuffer buf = bufHandle.getBuffer ();
	    int length = buf.remaining();
	    totalRead += length;
	    int offset = 0;
	    if (buf.isDirect ()) {
		arr = new byte[buf.remaining ()];
		buf.get (arr);
	    } else {
	    arr = buf.array ();
	    offset = buf.arrayOffset();
		buf.position (buf.limit ());
	    }
	    bufHandle.possiblyFlush ();
	    writeDataToGZipper (arr, offset, length);
	} else {
	    modifyBuffer (bufHandle);
	}
    }

    @Override
    public void setup (SProperties prop) {
	super.setup (prop);
	if (prop != null) {
	    String comp = prop.getProperty ("compress", "true");
	    compress = !comp.equalsIgnoreCase ("false");
	}
    }
}
