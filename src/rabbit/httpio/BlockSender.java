package rabbit.httpio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import rabbit.io.BufferHandle;
import rabbit.util.Logger;
import rabbit.util.TrafficLogger;

/** A handler that writes data blocks.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class BlockSender extends BaseSocketHandler {
    private ByteBuffer chunkBuffer;
    private ByteBuffer end;
    private ByteBuffer[] buffers;
    private final TrafficLogger tl;
    private final BlockSentListener sender;
    private int ops = 0;
    
    public BlockSender (SocketChannel channel, Selector selector, 
			Logger logger, TrafficLogger tl, 
			BufferHandle bufHandle, boolean chunking, 
			BlockSentListener sender) 
	throws IOException {
	super (channel, bufHandle, selector, logger);
	this.tl = tl;
	ByteBuffer buffer = bufHandle.getBuffer ();
	if (chunking) {
	    int len = buffer.remaining ();
	    String s = Long.toHexString (len) + "\r\n";
	    try {
		chunkBuffer = ByteBuffer.wrap (s.getBytes ("ASCII"));
	    } catch (UnsupportedEncodingException e) {
		logger.logError ("BlockSender: ASCII not found!");
	    }
	    end = ByteBuffer.wrap (new byte[] {'\r', '\n'});
	    buffers = new ByteBuffer[]{chunkBuffer, buffer, end};
	} else {
	    buffers = new ByteBuffer[]{buffer};
	    end = buffer;
	}
	this.sender = sender;
	writeBuffer ();
    }

    public String getDescription () {
	StringBuilder sb = new StringBuilder ("BlockSender: buffers: " + buffers.length);
	for (int i = 0; i < buffers.length; i++) {
	    if (i > 0)
		sb.append (", ");
	    sb.append ("i: ").append (buffers[i].remaining ());
	}
	return sb.toString ();
    }

    @Override protected int getSocketOperations () {
	return ops;
    }

    public void timeout () {
	releaseBuffer ();
	sender.timeout ();
    }

    public void run () {
	try {
	    writeBuffer ();
	} catch (IOException e) {
	    unregister ();
	    releaseBuffer ();
	    sender.failed (e);
	}
    }
    
    private void writeBuffer () throws IOException {
	long written;
	do {
	    written = channel.write (buffers);
	    tl.write (written);
	} while (written > 0 && end.remaining () > 0);

	if (end.remaining () == 0) {
	    unregister ();
	    releaseBuffer ();
	    sender.blockSent ();
	} else {
	    ops = SelectionKey.OP_WRITE;
	    register ();
	}
    }
}    
