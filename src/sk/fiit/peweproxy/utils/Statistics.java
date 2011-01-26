package sk.fiit.peweproxy.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import sk.fiit.peweproxy.plugins.PluginHandler.PluginInstance;
import sk.fiit.peweproxy.plugins.PluginHandler;
import sk.fiit.peweproxy.plugins.ProxyPlugin;

public class Statistics {
	
	public enum ProcessType {
		// generic
		PLUGIN_START,
		PLUGIN_STOP,
		
		// generic request,response
		REQUEST_DESIRED_SERVICES,
		RESPONSE_DESIRED_SERVICES,
		
		// processing
		REQUEST_PROCESSING,
		REQUEST_LATE_PROCESSING,
		REQUEST_CONSTRUCTION,
		RESPONSE_PROCESSING,
		RESPONSE_LATE_PROCESSING,
		RESPONSE_CONSTRUCTION,
		
		// services
		REQUEST_PROVIDE_SERVICE,
		REQUEST_SERVICE_COMMIT,
		RESPONSE_PROVIDE_SERVICE,
		RESPONSE_SERVICE_COMMIT,
		SERVICE_METHOD,
		
		
		// events
		CONNECTION_CREATE,
		CONNECTION_CLOSED,
		READ_FAIL,
		DELIVERY_FAIL,
		READ_TIMEOUT,
		DELIVERY_TIMEOUT,
	}
	
	public class ProcessStats {
		float avg = -1;
		int min = -1;
		int max = -1;
		
		int count = 0;
		
		void processExecuted(int executionTime) {
			count++;
			if (count == 1)
				avg = min = max = executionTime;
			else  {
				if (max < executionTime)
					max = executionTime;
				else if (min > executionTime)
					min = executionTime;
				avg += ((executionTime - avg) / count);
			}
		}
		
		void join(ProcessStats stats) {
			if (stats == null)
				return;
			avg = ((avg*count) + (stats.avg*stats.count)) / (count + stats.count);
			count += stats.count;
			min = (min < stats.min) ? min : stats.min;
			max = (max > stats.max) ? max : stats.max;
		}

		public float getAverage() {
			return avg;
		}

		public int getMin() {
			return min;
		}

		public int getMax() {
			return max;
		}
		
		public int getCount() {
			return count;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName()+"[avg:"+avg+", min:"+min+", max:"+max+"]";
		}
	}
	
	public class PluginStats {
		PluginInstance plgInstance;
		final Map<ProcessType, ProcessStats> processStats = new HashMap<ProcessType, ProcessStats>();
		
		private void join(PluginStats stats) {
			for (Entry<ProcessType, ProcessStats> entry : processStats.entrySet()) {
				entry.getValue().join(stats.processStats.remove(entry.getKey()));
			}
			processStats.putAll(stats.processStats);
		}
		
		public PluginInstance getPluginInstance() {
			return plgInstance;
		}
		
		ProcessStats getProcessStats(ProcessType type) {
			return processStats.get(type);
		}
	}
	
	final Map<ProxyPlugin, PluginStats> statsForPlugins = new HashMap<ProxyPlugin, PluginStats>();
	//final Map<Thread, Map<ProcessType, Long>> startTimes = new HashMap<Thread, Map<ProcessType,Long>>();	
	
	public void pluginsRealoaded(PluginHandler plgHandler) {
		Map<ProxyPlugin, PluginStats> oldStatsForPlugins = new HashMap<ProxyPlugin, PluginStats>();
		oldStatsForPlugins.putAll(statsForPlugins);
		statsForPlugins.clear();
		for (PluginInstance plgInstance : plgHandler.getAllPlugins()) {
			PluginStats stats = oldStatsForPlugins.remove(plgInstance.getInstance());
			statsForPlugins.put(plgInstance.getInstance(), stats);
			stats.plgInstance = plgInstance;
			for (PluginStats existingStats : oldStatsForPlugins.values()) {
				if (existingStats.plgInstance == null)
					continue;
				Class<?> existingPlgClass = existingStats.plgInstance.getPluginClass();
				Class<?> newPlgClass = plgInstance.getPluginClass();
				if ((existingPlgClass.equals(newPlgClass) || existingPlgClass.getName().equals(newPlgClass.getName()))
						&& existingStats.plgInstance.getName().equals(plgInstance.getName())) {
					stats.join(existingStats);
				}
			}
		}
	}
	
	public void executeProcess(final Runnable task, ProxyPlugin plugin, ProcessType type) throws Throwable {
		executeProcess(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				task.run();
				return null;
			}
		},plugin, type);
	}
	
	public <Type> Type executeProcess(Callable<Type> task, ProxyPlugin plugin, ProcessType type) throws Throwable {
		long time = System.currentTimeMillis();
		Type retVal = task.call();
		time = System.currentTimeMillis() - time;
		PluginStats plgStats = statsForPlugins.get(plugin);
		if (plgStats == null) {
			plgStats = new PluginStats();
			statsForPlugins.put(plugin,plgStats);
			// PluginHandler may be starting plugin before Statistics can access corresponding PluginInstance 
		}
		ProcessStats processStats = plgStats.processStats.get(type);
		if (processStats == null) {
			processStats = new ProcessStats();
			plgStats.processStats.put(type, processStats);
		}
		processStats.processExecuted((int)time);
		System.out.println(((plgStats.plgInstance == null) ? plugin : plgStats.plgInstance)+"\t"+type+"\t"+processStats);
		return retVal;
	}
	
	/*public void processStart(ProcessType type) {
		long curTime = System.currentTimeMillis();
		Thread curThread = Thread.currentThread();
		Map<ProcessType, Long> threadStartTimes = startTimes.get(curThread);
		if (threadStartTimes == null) {
			threadStartTimes = new HashMap<Statistics.ProcessType, Long>();
			startTimes.put(curThread, threadStartTimes);
		}
		threadStartTimes.put(type, new Long(curTime));
	}
	
	public void processEnd(ProxyPlugin plugin, ProcessType type) {
		long curTime = System.currentTimeMillis();
		Thread curThread = Thread.currentThread();
		Map<ProcessType, Long> threadStartTimes = startTimes.get(curThread);
		if (threadStartTimes == null)
			throw new IllegalStateException("No start times for thread "+curThread);
		Long startTime = threadStartTimes.remove(type);
		if (startTime == null)
			throw new IllegalStateException("No start time for thread "+curThread+" and process type "+type);
		long executionTime = startTime.longValue() - curTime;
		PluginStats plgStats = statsForPlugins.get(plugin);
		if (plgStats == null)
			throw new IllegalArgumentException("Can't find PluginStats for passed plugin "+plugin);
		ProcessStats processStats = plgStats.processStats.get(type);
		if (processStats == null) {
			processStats = new ProcessStats();
			plgStats.processStats.put(type, processStats);
		}
		processStats.processExecuted((int)executionTime);
	}*/
	
	PluginStats getPluginsStatistics(ProxyPlugin plugin) {
		return statsForPlugins.get(plugin);
	}
}
