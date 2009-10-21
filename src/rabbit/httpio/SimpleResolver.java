package rabbit.httpio;

import java.net.URL;
import rabbit.dns.DNSHandler;
import rabbit.dns.DNSJavaHandler;
import rabbit.io.InetAddressListener;
import rabbit.io.Resolver;
import rabbit.util.Logger;

/** A simple resolver that uses the dnsjava resolver. 
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class SimpleResolver implements Resolver {
    private final DNSHandler dnsHandler;
    private final TaskRunner tr;

    public SimpleResolver (Logger logger, TaskRunner tr) {
	DNSJavaHandler jh = new DNSJavaHandler ();
	jh.setup (null, logger);
	dnsHandler = jh;
	this.tr = tr;
    }

    public void getInetAddress (URL url, InetAddressListener listener) {
	tr.runThreadTask (new ResolvRunner (tr, dnsHandler, url, listener));
    }

    public int getConnectPort (int port) {
	return port;
    } 

    public boolean isProxyConnected () {
	return false;
    }
    public String getProxyAuthString () {
	return null;
    }
}
