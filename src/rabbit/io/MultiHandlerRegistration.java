package rabbit.io;

import java.nio.channels.SelectionKey;

/** A multiplexing handler registration.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class MultiHandlerRegistration implements HandlerRegistration {
    private SocketHandler reader; 
    private SocketHandler writer;
    private long readWhen;
    private long writeWhen;

    public MultiHandlerRegistration (SocketHandler reader, long readWhen,
				     SocketHandler writer, long writeWhen) {
	this.reader = reader;
	this.readWhen = readWhen;
	this.writer = writer;
	this.writeWhen = writeWhen;
    }
    
    @Override public String toString () {
	return "MultiHandlerRegistration[" +
	    "reader: " + reader.getDescription () + 
	    ", writer: " + writer.getDescription () + "]";
    }

    public long getRegistrationTime () {
	return Math.min (readWhen, writeWhen);
    }

    public String getDescription () {
	return toString ();
    }

    public boolean isExpired (long now, long timeout) {
	return (now - readWhen > timeout) || (now - writeWhen > timeout);
    }
    
    public SocketHandler getHandler (SelectionKey sk) {
	int ops = sk.readyOps ();
	if ((ops & SelectionKey.OP_WRITE) > 0)
	    return writer;
	if ((ops & SelectionKey.OP_READ) > 0)
	    return reader;
	throw new IllegalStateException ("no handler for ops: " + 
					 Integer.toHexString (ops) +
					 " MHR: " + this);
    }

    public void register (int currentOps, int newOps, SelectionKey sk, 
			  SocketHandler sh, long when) {
	if (newOps == SelectionKey.OP_WRITE) {
	    writer = sh;
	    writeWhen = when;
	} else if (newOps == SelectionKey.OP_READ) {
	    reader = sh;
	    readWhen = when;
	} else {
	    String s = "do not know how to handle ops: " + 
		Integer.toHexString (newOps);
	    throw new IllegalArgumentException (s);
	}
    }

    public void unregister (SelectionKey sk, SocketHandler sh, String reason) {
	if (sh == reader) {
	    sk.interestOps (SelectionKey.OP_WRITE);
	    sk.attach (new SimpleHandlerRegistration (writer, writeWhen));
	} else if (sh == writer) {
	    sk.interestOps (SelectionKey.OP_READ);
	    sk.attach (new SimpleHandlerRegistration (reader, readWhen));
	} else {
	    String s = "sh: " + sh + " is not part of this mulithandler: " + 
		this;
	    throw new IllegalArgumentException (s);
	}
    }

    public void timeout () {
	// TODO: not sure if we only want to timeout one or both here.
	reader.timeout ();
	writer.timeout ();
    }
}
