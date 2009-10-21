package rabbit.httpio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import rabbit.io.BufferHandle;
import rabbit.io.BufferHandler;
import rabbit.io.CacheBufferHandle;

/** A resource that comes from a file.
 * 
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class FileResourceSource implements ResourceSource {    
    protected FileChannel fc;
    
    // used for block handling.
    private BlockListener listener;
    private TaskRunner tr;
    protected BufferHandle bufHandle;
    
    public FileResourceSource (String filename, TaskRunner tr, 
			       BufferHandler bufHandler) 
	throws IOException {
	this (new File (filename), tr, bufHandler);
    }

    public FileResourceSource (File f, TaskRunner tr, 
			       BufferHandler bufHandler) 
	throws IOException {
	if (!f.exists ())
	    throw new FileNotFoundException ("File: " + f.getName () + 
					     " not found");
	if (!f.isFile ())
	    throw new FileNotFoundException ("File: " + f.getName () + 
					     " is not a regular file");
	FileInputStream fis = new FileInputStream (f);
	fc = fis.getChannel ();
	this.tr = tr;
	this.bufHandle = new CacheBufferHandle (bufHandler);
    }    

    /** FileChannels can be used, will always return true.
     * @return true
     */
    public boolean supportsTransfer () {
	return true;
    }

    public long length () {
	try {
	    return fc.size ();
	} catch (IOException e) {
	    e.printStackTrace ();
	    return -1;
	}
    }

    public long transferTo (long position, long count, 
			    WritableByteChannel target)
	throws IOException {
	try {
	    return fc.transferTo (position, count, target);
	} catch (IOException e) {
	    if ("Resource temporarily unavailable".equals (e.getMessage ())) {
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
		// transferTo on linux throws IOException on full buffer.
		return 0;
	    }
	    throw e;
	}
    }

    /** Generally we do not come into this method, but it can happen..
     */
    public void addBlockListener (BlockListener listener) {
	this.listener = listener;
	// Get buffer on selector thread.
	bufHandle.getBuffer ();
	tr.runThreadTask (new ReadBlock ());
    }

    private class ReadBlock implements Runnable {
	public void run () {
	    try {
		ByteBuffer buffer = bufHandle.getBuffer ();
		int read = fc.read (buffer);
		if (read == -1) {
		    returnFinished ();
		} else {
		    buffer.flip ();
		    returnBlockRead ();
		}
	    } catch (IOException e) {
		returnWithFailure (e);
	    }
	}
    }

    private void returnWithFailure (final Exception e) {
	tr.runMainTask (new Runnable () {
		public void run () {
		    bufHandle.possiblyFlush ();
		    listener.failed (e);
		}
	    });
    }

    private void returnFinished () {
	tr.runMainTask (new Runnable () {
		public void run () {
		    bufHandle.possiblyFlush ();
		    listener.finishedRead ();
		}
	    });
    }

    private void returnBlockRead () {
	tr.runMainTask (new Runnable () {
		public void run () {
		    listener.bufferRead (bufHandle);
		}
	    });
    }

    public void release () {
	try {
	    if (fc != null)
		fc.close ();
	    listener = null;
	    tr = null;
	    bufHandle.possiblyFlush ();
	    bufHandle = null;
	} catch (IOException e) {
	    // TODO: handle...
	}
    }
}
