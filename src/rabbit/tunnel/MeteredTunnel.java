package rabbit.tunnel;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/** A tunnel to simulate a slow connection. 
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class MeteredTunnel {
    private static final int DEFAULT_LISTEN_PORT = 9665;
    private static final int DEFAULT_FORWARD_PORT = 9666;
    
    private int port;
    private int forwardPort;
    // speed in kB / second / connection
    private int speed;
    
    public static void main (String[] args) {
	int i = 0;
	int listenPort = DEFAULT_LISTEN_PORT;
	int forwardPort = DEFAULT_FORWARD_PORT;
	int speed = 3 * 1024;

	while (i < args.length) {
	    if ("-p".equals (args[i]) && args.length < (i + 1)) {
		listenPort = Integer.parseInt (args[++i]);
		continue;
	    }
	    if ("-f".equals (args[i]) && args.length < (i + 1)) {
		forwardPort = Integer.parseInt (args[++i]);
		continue;
	    }
	    
	    if ("-s".equals (args[++i]) && args.length < (i + 1)) {
		speed = Integer.parseInt (args[++i]) * 1024;   
		continue;
	    }
	    System.err.println ("unsupported argument: '" + args[i] + "'");
	}

	MeteredTunnel mt = new MeteredTunnel (listenPort, forwardPort, speed);
	mt.run ();
    }

    public MeteredTunnel (int port, int forwardPort, int speed) {
	this.port = port;
	this.forwardPort = forwardPort;
	this.speed = speed;
    }

    public void run () {
	try {
	    ServerSocket ss = new ServerSocket (port);
	    while (true) {
		Socket s = ss.accept ();
		tunnelSocket (s);
	    }
	} catch (IOException e) {
	    e.printStackTrace ();
	}
    }

    private void tunnelSocket (Socket forward) throws IOException {
	Socket ret = new Socket (InetAddress.getLocalHost (), forwardPort);
	Thread f = new Thread (new Tunnel (forward.getInputStream (), 
					   ret.getOutputStream ()));
	Thread r = new Thread (new Tunnel (ret.getInputStream (), 
					   forward.getOutputStream ()));
	f.start ();
	r.start ();
    }
    
    private class Tunnel implements Runnable {
	private InputStream from; 
	private OutputStream to;
	private byte[] buf = new byte[speed / 10];

	public Tunnel (InputStream from, OutputStream to) {
	    this.from = from;
	    this.to = to;
	}
	
	public void run () {
	    try {
		while (true) {
		    int read = from.read (buf);
		    if (read == -1) 
			break;
		    to.write (buf, 0, read);
		    try {
			Thread.sleep (100);
		    } catch (InterruptedException e) {
			e.printStackTrace ();
		    }
		}
	    } catch (IOException e) {
		e.printStackTrace ();
	    } finally {
		close (from);
		close (to);
	    }
	}

	private void close (Closeable c) {
	    try {
		if (c != null)
		    c.close ();
	    } catch (IOException e) {
		e.printStackTrace ();
	    }
	}
    }
}
