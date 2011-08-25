package sk.fiit.peweproxy.utils;

public class StackTraceUtils {
	private static final String THIS_CLASS_NAME = StackTraceUtils.class.getName();
	private static final String THREAD_CLASS_NAME = Thread.class.getName();
	
	public static String getStackTraceText(Thread thread) {
		StringBuilder sb = new StringBuilder(thread.toString());
		sb.append('\n');
		boolean ignoreElement = true;
		for (StackTraceElement element : thread.getStackTrace()) {
			if (ignoreElement) {
				String elementClass = element.getClassName();
				if (THIS_CLASS_NAME.equals(elementClass) || THREAD_CLASS_NAME.equals(elementClass))
					continue;
				else
					ignoreElement = false;
			}
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
