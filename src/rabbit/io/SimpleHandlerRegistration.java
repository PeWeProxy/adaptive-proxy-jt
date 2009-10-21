package rabbit.io;

import java.nio.channels.SelectionKey;

/** A class to hold information about when an operation 
 *  was attached to the selector. Needed for timeout options.
 */
public class SimpleHandlerRegistration implements HandlerRegistration {
    private SocketHandler handler;
    private long when;

    public SimpleHandlerRegistration (SocketHandler handler, long when) {
	this.when = when;
	this.handler = handler;
    }

    @Override public String toString () {
	return "HandlerRegistration[when: " + when + 
	    ", handler: " + handler.getDescription () + "]";
    }
    
    public long getRegistrationTime () {
	return when;
    }

    public boolean isExpired (long now, long timeout) {
	return now - when > timeout;
    }

    public SocketHandler getHandler (SelectionKey sk) {
	return handler;
    }

    public void register (int currentOps, int newOps, SelectionKey sk, 
			  SocketHandler sh, long when) {
	if (currentOps == newOps) {
	    handler = sh;
	    this.when = when;
	} else if (currentOps == SelectionKey.OP_READ && newOps == SelectionKey.OP_WRITE) {
	    sk.attach (new MultiHandlerRegistration (handler, this.when, sh, when));
	    sk.interestOps (newOps & currentOps);
	} else if (currentOps == SelectionKey.OP_WRITE && newOps == SelectionKey.OP_READ) {
	    sk.attach (new MultiHandlerRegistration (sh, when, handler, this.when));	    
	    sk.interestOps (newOps & currentOps);
	} else {
	    String s = "Do not know how to handle " + 
		Integer.toHexString (currentOps) + ", " + handler +
		" and " + 
		Integer.toHexString (newOps) + ", " + sh;
	    throw new IllegalArgumentException (s);
	}
    }

    public void unregister (SelectionKey sk, SocketHandler sh, String reason) {
	if (sh != handler)
	    throw new IllegalArgumentException ("sh: " + sh + 
						" is not the current handler");
	sk.interestOps (0);
	sk.attach (new HandleTimeoutErrors (reason));
    }

    public void timeout () {
	handler.timeout ();
    }

    public String getDescription () {
	return handler.getDescription ();
    }
}
    
