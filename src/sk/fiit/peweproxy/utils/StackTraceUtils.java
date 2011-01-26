package sk.fiit.peweproxy.utils;

public class StackTraceUtils {
	public static String getStackTraceText(Thread thread) {
		StringBuilder sb = new StringBuilder(thread.toString());
		sb.append('\n');
		boolean ignoreElements = true;
		for (StackTraceElement element : thread.getStackTrace()) {
			if (ignoreElements)
				continue; // do not print Thread method on the top
			if (StackTraceUtils.class.getName().equals(element.getClassName()))
				ignoreElements = false; // do not print this StackTraceUtils method, but switch ignore flag
			else {
				sb.append("\tat ");
				sb.append(element.toString());
				sb.append('\n');
			}
		}
		return sb.toString();
	}
	
	public static String getStackTraceText() {
		return getStackTraceText(Thread.currentThread());
	}
}
