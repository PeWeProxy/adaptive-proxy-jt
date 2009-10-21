package rabbit.io;

import java.util.Date;

/** Class that holds information about sockets that are
 *  currently not registered with anyone.
 */
public class HandleTimeoutErrors {
    /** The reason this socket is no longer active */
    private final String reason; 
    /** When the socket was last seen active */
    private final long when;
    
    public HandleTimeoutErrors (String reason) {
	this.reason = reason;
	this.when = System.currentTimeMillis ();
    }

    public String getReason () {
	return reason;
    }

    public long getWhen () {
	return when;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{reason: " + reason + 
	    ", when: " + new Date (when) + "}";
    }
}