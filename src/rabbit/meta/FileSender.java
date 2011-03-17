package rabbit.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.khelekore.rnio.impl.Closer;
import rabbit.http.HttpDateParser;
import rabbit.http.HttpHeader;
import rabbit.httpio.HttpHeaderSender;
import rabbit.httpio.HttpHeaderSentListener;
import rabbit.httpio.TransferHandler;
import rabbit.httpio.TransferListener;
import rabbit.httpio.Transferable;
import rabbit.proxy.Connection;
import rabbit.util.HeaderUtils;
import rabbit.util.MimeTypeMapper;
import rabbit.util.SProperties;
import rabbit.util.TrafficLogger;
import rabbit.zip.GZipPackListener;
import rabbit.zip.GZipPacker;

/** A file resource handler.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class FileSender implements MetaHandler, HttpHeaderSentListener {
	private static final String PREFIX_GZIP = "gzip_";
	private static final String POSTFIX_GZIP = ".gzip";
    private Connection con;
    private TrafficLogger tlClient;
    private TrafficLogger tlProxy;
    private FileInputStream fis;
    private FileChannel fc;
    private long length;
    private final Logger logger = Logger.getLogger (getClass ().getName ());
    private GZipPacker packer = null;

    public void handle (HttpHeader request,
			SProperties htab,
			Connection con,
			TrafficLogger tlProxy,
			TrafficLogger tlClient) throws IOException{
	this.con = con;
	this.tlProxy = tlProxy;
	this.tlClient = tlClient;
	
	Date ifModifiedSince = HttpDateParser.getDate(request.getHeader("If-Modified-Since"));

	String file = htab.getProperty ("argstring");
	if (file == null)
	    throw (new IllegalArgumentException ("no file given."));
	if (file.indexOf ("..") >= 0)    // file is un-url-escaped
	    throw (new IllegalArgumentException ("Bad filename given"));

	String filePath = con.getProxy().getHtdocsDir()+"/" + file;
	if (filePath.endsWith ("/"))
	    filePath = filePath + "index.html";
	filePath = filePath.replace ('/', File.separatorChar);

	File fle = new File (filePath);
	boolean gzip = false;
	if (!fle.exists ()) {
		boolean success = false;
		int indexOfSlash = filePath.lastIndexOf(File.separatorChar);
		String fileName = filePath.substring(indexOfSlash+1);
		if (!fileName.isEmpty()) { // should never happen
			if (indexOfSlash == -1)
				indexOfSlash = 0;
			fle = new File(filePath.substring(0, indexOfSlash+1)+PREFIX_GZIP+fileName);
			if (fle.exists()) {
				success = true;
				if (HeaderUtils.doesClientAcceptGzip(request))
					gzip = true;
			}
		}
		if (!success) {
			// remove "htdocs/"
		    do404 (filePath.substring (con.getProxy().getHtdocsDir().length()+1));
		    return;
		}
	}
	if (gzip) {
		try {
			prepareGzipFile(filePath, fle, ifModifiedSince);
		} catch (IOException e) {
			failed(e);
		}
	} else
		sendHeader(filePath, fle, ifModifiedSince, false);
    }
    
    private void sendHeader(String urlPath, File sourceFile, Date ifModifiedSince, boolean gzip) throws IOException {
    Date lm = getLastModified(sourceFile);
    
    HttpHeader response = null;
    if (ifModifiedSince != null && lm.equals(ifModifiedSince)) {
    	response = con.getHttpGenerator().get304(new HttpHeader());
    	sourceFile = null;
    } else {
    	response = con.getHttpGenerator ().getHeader ();
    }
    HeaderUtils.removeCacheHeaders(response);
	setMime (urlPath, response);
	response.setHeader ("Last-Modified", HttpDateParser.getDateString (lm));
	if (sourceFile != null) {
		length = sourceFile.length ();
		response.setHeader ("Content-Length", Long.toString (length));
		con.setContentLength (response.getHeader ("Content-Length"));
		if (gzip)
			response.setHeader ("Content-Encoding","gzip");
		try {
		    fis = new FileInputStream (sourceFile);
		} catch (IOException e) {
		    throw (new IllegalArgumentException ("Could not open file: '" +
							 sourceFile.getAbsolutePath() + "'."));
		}
	}
	sendHeader (response);
    }
    
    private Date getLastModified(File file) {
    	return new Date (file.lastModified () - con.getProxy ().getOffset ());
    }
    
    private void prepareGzipFile(final String urlPath, final File sourceFile, final Date ifModifiedSince) throws IOException {
    	// example: "htdocs/some_paths/gzip_some_script.js"
    	String fileName = sourceFile.getName();
    	final File gzipSource= new File(sourceFile.getParentFile(),fileName.concat(POSTFIX_GZIP));
    	if (!gzipSource.exists() || sourceFile.lastModified() != gzipSource.lastModified()) {
    		final byte[] buffer = new byte[4096];
    		final FileInputStream inStream = new FileInputStream(sourceFile);
    		final FileOutputStream outStream = new FileOutputStream(gzipSource);
    		packer = new GZipPacker(new GZipPackListener() {
    			@Override
    			public byte[] getBuffer () {
    			    return buffer;
    			}
				
				@Override
				public void failed(Exception e) {
					closeStreams();
					FileSender.this.failed(e);
				}
				
				@Override
				public void packed(byte[] buf, int off, int len) {
					try {
						if (len > 0) {
							outStream.write(buf, off, len);
							getNextData(buffer, inStream);
					    } else {
					    	getNextData(buffer, inStream);
					    }
					} catch (IOException e) {
						failed(e);
					}
				}
				
				@Override
				public void finished() {
					closeStreams();
					gzipSource.setLastModified(sourceFile.lastModified());
					try {
						sendHeader(urlPath, gzipSource, ifModifiedSince, true);
					} catch (IOException e) {
						failed(e);
					}
				}
				
				@Override
				public void dataPacked() {}
				
				void closeStreams() {
					Closer.close(inStream, logger);
					Closer.close(outStream, logger);
				}
			});
    		if (!packer.needsInput())
    			packer.handleCurrentData();
    		else
    			getNextData(buffer, inStream);
    	} else
    		sendHeader(urlPath, gzipSource, ifModifiedSince, true);
    }
    
    private void getNextData(byte[] buf, InputStream inStream) throws IOException {
    	while (packer.needsInput()) {
    		int read = inStream.read(buf, 0, buf.length);
    		if (read == -1) {
    			packer.finish();
    			break;
    		} else
    			packer.setInput(buf, 0, read);
    	}
    	if (!packer.finished())
    		packer.handleCurrentData();
    	
    }

    private void setMime (String filename, HttpHeader response) {
	// TODO: better filename mapping.
	String type = MimeTypeMapper.getMimeType (filename);
	if (type != null)
	    response.setHeader ("Content-type", type);
    }

    private void do404 (String filename)
	throws IOException {
	HttpHeader response = con.getHttpGenerator ().get404 (filename);
	sendHeader (response);
    }

    private void sendHeader (HttpHeader header)
	throws IOException {
	HttpHeaderSender hhs =
	    new HttpHeaderSender (con.getChannel (), con.getNioHandler (),
				  tlClient, header, true, this);
	hhs.sendHeader ();
    }

    /** Write the header and the file to the output.
     */
    private void channelTransfer (long length) {
	TransferListener ftl = new FileTransferListener ();
	TransferHandler th =
	    new TransferHandler (con.getNioHandler (),
				 new FCTransferable (length),
				 con.getChannel (), tlProxy, tlClient, ftl);
	th.transfer ();
    }

    private class FCTransferable implements Transferable {
	private final long length;

	public FCTransferable (long length) {
	    this.length = length;
	}

	public long transferTo (long position, long count,
				WritableByteChannel target)
	    throws IOException {
	    return fc.transferTo (position, count, target);
	}

	public long length () {
	    return length;
	}
    }

    private class FileTransferListener implements TransferListener {
	public void transferOk () {
	    closeFile ();
	    con.logAndRestart ();
	}

	public void failed (Exception cause) {
	    closeFile ();
	    FileSender.this.failed (cause);
	}
    }

    private void closeFile () {
	Closer.close (fc, logger);
	Closer.close (fis, logger);
    }

    public void httpHeaderSent () {
	if (fis != null) {
	    fc = fis.getChannel ();
	    channelTransfer (length);
	} else {
	    con.logAndRestart ();
	}
    }

    public void failed (Exception e) {
	closeFile ();
	logger.log (Level.WARNING, "Exception when handling meta", e);
	con.logAndClose (null);
    }

    public void timeout () {
	closeFile ();
	logger.warning ("Timeout when handling meta.");
	con.logAndClose (null);
    }
}
