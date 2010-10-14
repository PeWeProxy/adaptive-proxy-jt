package sk.fiit.peweproxy.plugins;

import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PluginsThreadPool extends ThreadPoolExecutor {
	private static ThreadGroup group = new ThreadGroup(PluginsThreadPool.class.getSimpleName()+".ThreadGroup");
	private static int number = 0;
	private static ThreadFactory threadFactory
		= new ThreadFactory() {
				@Override
				public synchronized Thread newThread(Runnable r) {
					// maybe we'll need the threads to be in the same group in the future
					Thread newThread = new Thread(group,r,PluginsThreadPool.class.getSimpleName()+"-thread-"+number);
					number++;
					return newThread;
				}};
	
	public PluginsThreadPool(int coreThreads) {
		super(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), threadFactory);
	}
	
	@Override
	public void shutdown() {
		throw new IllegalAccessError("Plugins are not allowed to shutdown provided thread pool");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalAccessError("Plugins are not allowed to shutdown provided thread pool");
	}
}
