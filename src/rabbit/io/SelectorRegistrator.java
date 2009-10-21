package rabbit.io;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import rabbit.util.Logger;

/** A class to handle selector registrations.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class SelectorRegistrator {
    public static SelectionKey register (Logger logger, 
					 SelectableChannel channel, 
					 Selector selector,
					 int ops, 
					 SocketHandler sh) 
	throws ClosedChannelException {
	return register (logger, channel, selector, ops, sh, 
			 System.currentTimeMillis ());
    }

    public static SelectionKey register (Logger logger,
					 SelectableChannel channel, 
					 Selector selector, 
					 int ops, 
					 SocketHandler sh, 
					 long when) 
	throws ClosedChannelException {
	SelectionKey existingKey = channel.keyFor (selector);
	if (existingKey != null)
	    selector = existingKey.selector ();
	if (existingKey == null || !existingKey.isValid ())
	    existingKey = channel.register (selector, 0);
	int eops = existingKey.interestOps ();
	if ((eops & ops) != 0) {
	    logger.logWarn ("trying to register conflicting ops: 0x" +
			    Integer.toHexString (ops) + ", eops: 0x" +
			    Integer.toHexString (eops) + 
			    ", channel: " + channel + 
			    ", current handler: " + 
			    existingKey.attachment () + 
			    ", new handler: " + sh);
	}
	if (eops > 0) {
	    Object attached = existingKey.attachment ();
	    if (attached instanceof HandlerRegistration) { 
		HandlerRegistration hr = (HandlerRegistration)attached;
		hr.register (eops, ops, existingKey, sh, when);
		return existingKey;
	    }
	    String s = "do not know how to handle: " + attached + 
		"will overwrite with a simple handler";
	    logger.logWarn (s);
	}
	HandlerRegistration hr = new SimpleHandlerRegistration (sh, when);
	existingKey.interestOps (ops);
	existingKey.attach (hr);
	return existingKey;
    }

    public static void unregister (Selector selector, SelectionKey sk, 
				   SocketHandler sh, String reason) {
	if (sk != null && sk.isValid ()) {
	    Object a = sk.attachment ();
	    if (a != null && (a instanceof HandlerRegistration)) {
		HandlerRegistration hr = (HandlerRegistration)a;
		hr.unregister (sk, sh, reason);
	    } else {
		String s = "sh is not a registered handler: " + sh;
		throw new IllegalArgumentException (s);
	    }
	}
    }
}
