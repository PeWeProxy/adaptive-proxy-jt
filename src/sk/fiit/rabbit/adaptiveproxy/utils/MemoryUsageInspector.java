package sk.fiit.rabbit.adaptiveproxy.utils;

import org.apache.log4j.Logger;

public final class MemoryUsageInspector {
	private static Runtime runtime = Runtime.getRuntime();
	
	private MemoryUsageInspector() {}
	
	public static void printMemoryUsage(Logger log, String message) {
		if (log.isTraceEnabled()) {
			long freeMBytes = runtime.freeMemory() / 1048576;
			long totalMBytes = runtime.totalMemory() / 1048576;
			log.trace(message+": "+(totalMBytes-freeMBytes)+" MB /"+totalMBytes+" MB ("+freeMBytes+" MB free)");
		}
	}
}
