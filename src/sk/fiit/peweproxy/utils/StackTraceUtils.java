package sk.fiit.peweproxy.utils;

public class StackTraceUtils {
	public static String getStackTraceText(Thread thread) {
		StringBuilder sb = new StringBuilder(thread.toString());
		sb.append('\n');
		for (StackTraceElement element : thread.getStackTrace()) {
			sb.append("\tat ");
			sb.append(element.toString());
			sb.append('\n');
		}
		return sb.toString();
	}
	
	public static String getStackTraceText() {
		return getStackTraceText(Thread.currentThread());
	}
}
