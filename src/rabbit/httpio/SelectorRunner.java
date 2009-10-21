package rabbit.httpio;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import rabbit.io.HandlerRegistration;
import rabbit.io.HandleTimeoutErrors;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SocketHandler;
import rabbit.util.Level;
import rabbit.util.Logger;

/** A class that handles a selector. 
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class SelectorRunner implements Runnable, TaskRunner {

    /** The selector in use. */
    private Selector selector = null;

    /** Is this proxy running. */
    private boolean running = false;

    /** The queue to get back on the main thread. */
    private final Object returnedTasksLock = new Object ();
    private List<Runnable> returnedTasks1 = new ArrayList<Runnable> ();
    private List<Runnable> returnedTasks2 = new ArrayList<Runnable> ();

    private final Logger logger;

     /** The executor service. */
    private final ExecutorService executorService;
    
    private Thread mainThread = null;

    /** Create a SelectorRunner that uses the given thread pool 
     *  to run background tasks. 
     */
    public SelectorRunner (ExecutorService executorService, Logger logger) 
	throws IOException {
	this.executorService = executorService;
	this.logger = logger;
	selector = Selector.open ();
    }
    
    /** Start running the selector in a new thread.
     */
    public synchronized void start () {
	running = true;	
	Thread t = new Thread (this, getClass ().getName ());
	t.start ();
	mainThread = t;
    }

    /** Stop and close the selector and shutdown the thread pool.
     */
    public void shutdown () {
	synchronized (this) {
	    running = false;
	}
	executorService.shutdown ();
	try {
	    if (selector != null) {
		selector.close ();
		selector = null;
	    }
	} catch (IOException e) {
	    logger.logFatal ("Failed to close selector " + 
			     getStackTrace (e));
	} finally {
	    synchronized (returnedTasksLock) {
		logger.logInfo ("dropping returned tasks: "  + returnedTasks1);
		returnedTasks1.clear ();
		returnedTasks2.clear ();
	    }
	}
    }

    /** Run a task in the thread pool.
     */
    public void runThreadTask (Runnable r) {
	executorService.execute (r);
    }

    /** Run a task on the selector.
     */
    public void runMainTask (Runnable r) {
	runSelectorTask (r);
    }
    
    @Override
    public boolean isRunningInMainThread() {
    	return (Thread.currentThread().equals(mainThread));
    }

    /** Get the selector used for network operations. 
     */
    public Selector getSelector () {
	return selector;
    }

    private void runSelectorTask (Runnable r) {
	if (logger.showsLevel (Level.DEBUG))
	    logger.logDebug ("Trying to add a selector task: " + r);
	if (!getRunning ())
	    return;
	synchronized (returnedTasksLock) {
	    returnedTasks1.add (r);
	}
	synchronized (this) {
	    if (running)
		selector.wakeup ();
	}
    }

    private synchronized boolean getRunning () {
	return running;
    }

    /** Run the selector operations.
     */
    public void run () {
	long lastRun = System.currentTimeMillis ();
	int counter = 0;
	while (getRunning ()) {
	    while (getRunning () && !selector.isOpen ()) {
		try {
		    // wait for reconfigure
		    Thread.sleep (2 * 1000);
		} catch (InterruptedException e) {
		    // ignore
		}
	    }
	    try {
		Set<SelectionKey> keys = selector.keys ();
		if (keys.size () > 1)
		    selector.select (10 * 1000);
		else 
		    selector.select (100 * 1000);
		long now = System.currentTimeMillis ();
		long diff = now - lastRun;
		if (diff > 100) 
		    counter = 0;
		if (selector.isOpen ()) {
		    logger.logDebug ("selector selected or woken up");
		    cancelTimeouts (now);
		    int num = handleSelects ();
		    int rt = 0;
		    do {
			rt = runReturnedTasks ();
			num += rt;
		    } while (rt > 0);
		    if (num == 0)
			counter++;
		}
		if (counter > 100000) {
		    tryAvoidSpinning (counter, now, diff);
		    counter = 0;
		}
		lastRun = now;
	    } catch (IOException e) {
		logger.logError ("Failed to accept, " + 
				 "trying to restart serversocket: " + e +
				 "\n" + getStackTrace (e));
		synchronized (this) {
		    shutdown ();
		    start ();
		}
	    } catch (Exception e) {
		logger.logError ("Unknown error: " + e + 
				 " attemting to ignore\n" + 
				 getStackTrace (e));
	    }
	}
    }

    /* the epoll selector in linux is buggy in java/6, try a few things
     * to avoid selector spinning.
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
     *
     * Try to figure out the key that ought to be closed and cancel it.
     * 
     * This bug above is fixed in 6u1, but keep the code in case of 
     * other systems and possibly other bugs.
     */
    private void tryAvoidSpinning (int counter, long now, long diff) 
	throws IOException {
	logger.logError ("Trying to avoid spinning, may close some channels: " +
			 "counter: " + counter + ", now: " + now +
			 ", diff: " + diff);
	// Keys are generally writable, try to flip OP_WRITE 
	// so that the selector will remove the bad keys.
	Set<SelectionKey> triedKeys = new HashSet<SelectionKey> ();
	for (SelectionKey sk : selector.keys ()) {
	    int ops = sk.interestOps ();
	    if (ops == 0) {
		triedKeys.add (sk);
		sk.interestOps (SelectionKey.OP_WRITE);
	    }
	}
	selector.selectNow ();
	Set<SelectionKey> selected = selector.selectedKeys ();
	for (SelectionKey sk : selected) {
	    if (sk.isWritable ()) {
		triedKeys.remove (sk);
	    }
	    sk.interestOps (0);
	}
	
	// If we have any keys left here they are in an unknown state
	// cancel them and hope for the best.
	if (!triedKeys.isEmpty ()) {
	    logger.logError ("Some keys did not get writable, " + 
			     "trying to close them");
	    for (SelectionKey sk : triedKeys) {
		logger.logError ("Non writable key: " + sk + 
				 ", attachment: " + sk.attachment ());
		sk.cancel ();
	    }
	    selector.selectNow (); 
	}
 	logger.logError ("Spin evasion complete, " + 
			 "hopefully system is ok again.");
    }

    private String getStackTrace (Throwable t) {
    	StringWriter sw = new StringWriter ();
	PrintWriter ps = new PrintWriter (sw);
	t.printStackTrace (ps);
	return sw.toString ();
    }

    private void cancelTimeouts (long now) throws IOException {
	for (SelectionKey sk : selector.keys ()) {
	    Object a = sk.attachment ();
	    if (a instanceof HandleTimeoutErrors) {
		HandleTimeoutErrors hte = (HandleTimeoutErrors)a;
		if (now - hte.getWhen () > 60 * 1000 * 5) {	
		    logger.logError ("No handler for: " + sk + 
				     ", closing down, last seen reason: " + 
				     hte.getReason ());
		    cancelKeyAndCloseChannel (sk);
		}
	    } else {
		HandlerRegistration hr = (HandlerRegistration)a;
		if (hr != null && hr.isExpired (now, 60 * 1000)) {
		    if (logger.showsLevel (Level.DEBUG))
			logTimeoutDebug (sk, hr);		    
		    cancelKeyAndCloseChannel (sk);
		    hr.timeout ();
		}
	    }
	}
    }

    private void logTimeoutDebug (SelectionKey sk, HandlerRegistration hr) {
	logger.logDebug ("key timed out: " + sk + 
			 ", handler registration: " + hr);
    }
    
    /** Close down a client that has timed out. 
     */
    private void cancelKeyAndCloseChannel (SelectionKey sk) {
	sk.cancel ();
	try {
	    SocketChannel sc = (SocketChannel)sk.channel ();
	    sc.close ();
	} catch (IOException e) {
	    logger.logError ("failed to shutdown and close socket: " + e);
	}
    }

    
    private int handleSelects () throws IOException {
	Set<SelectionKey> selected = selector.selectedKeys ();
	int ret = selected.size ();
	if (logger.showsLevel (Level.DEBUG))
	    logger.logDebug ("selected set has: " + ret + 
			     " keys, total number or keys: " + 
			     selector.keys ().size ());
	for (SelectionKey sk : selected) {
	    Object a = sk.attachment ();
	    if (logger.showsLevel (Level.DEBUG))
		logSelectDebug (sk, a);
	    if (a != null && a instanceof HandlerRegistration) {
		HandlerRegistration hr = (HandlerRegistration)a;
		if (sk.isValid ()) {
		    runHandler (hr, sk);
		} else {
		    cancelKeyAndCloseChannel (sk);
		    hr.timeout ();
		}
	    } else if (a == null) {
		logger.logWarn ("No handler for:" + sk + 
				", sk.interest: " + sk.interestOps () + 
				", sk.ready: " + sk.readyOps ());
		if (sk.isValid ()) // do we want to close instead?
		    sk.interestOps (0);
	    } else {
		// Ok, something is very bad here, try to shutdown the channel
		// and hope that we handle it ok elsewhere...
		logger.logError ("Bad handler for:" + sk + ": " + a  + 
				 ", a.interest: " + sk.interestOps () + 
				 ", ready: " + sk.readyOps ());
		sk.cancel ();
		sk.channel ().close ();
	    }
	}
	selected.clear ();
	return ret;
    }

    private void logSelectDebug (SelectionKey sk, Object attachment) {
	logger.logDebug ("selected key: " + sk + 
			 ", attachment: " + attachment);
    }

    private void runHandler (HandlerRegistration hr, SelectionKey sk) 
	throws IOException {
	SocketHandler sh = hr.getHandler (sk);
	if (sh != null) {
	    SelectorRegistrator.unregister (selector, sk, sh, 
					    "Calling handle for: " + sh);
	    handle (sh);
	} else {
	    logger.logWarn ("failed to find valid handler: hr: " + hr);
	    sk.interestOps (0); // do we want to close instead?
	}
    }
    
    private void handle (SocketHandler handler) {
	if (handler.useSeparateThread ()) {
	    runThreadTask (handler);
	} else {
	    handler.run ();
	}
    }

    private int runReturnedTasks () {
	synchronized (returnedTasksLock) {
	    List<Runnable> toRun = returnedTasks1;
	    returnedTasks1 = returnedTasks2;
	    returnedTasks2 = toRun;
	}
	int s = returnedTasks2.size ();
	if (logger.showsLevel (Level.DEBUG))
	    logReturnedTaskDebugging (returnedTasks2);
	for (int i = 0; i < s; i++)
	    returnedTasks2.get (i).run ();
	returnedTasks2.clear ();
	return s;
    }

    private void logReturnedTaskDebugging (List<Runnable> runnables) {
	int s = runnables.size ();
	if (s == 0)
	    return;
	logger.logDebug ("returned task list size: " + s);
	for (Runnable r : runnables) {
	    logger.logDebug ("runnable: " + r);
	}
    }
}
