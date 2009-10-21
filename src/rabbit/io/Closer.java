package rabbit.io;

import java.io.Closeable;
import java.io.IOException;
import rabbit.util.Logger;

/** A helper class that can close resources without throwing exceptions.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class Closer {
    public static void close (Closeable c, Logger l) {
	if (c == null)
	    return;
	try {
	    c.close ();
	} catch (IOException e) {
	    l.logError ("Failed to close connection: " + c);
	}
    }
}
