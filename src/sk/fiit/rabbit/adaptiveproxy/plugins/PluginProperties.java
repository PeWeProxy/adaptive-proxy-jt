package sk.fiit.rabbit.adaptiveproxy.plugins;

public abstract class PluginProperties {
	public abstract String getProperty(String name);
	
	public abstract String getProperty(String name, String defaultValue);
	
	public abstract boolean getBoolProperty(String name, boolean defaultValue);
	
	public abstract int getIntProperty(String name, int defaultValue);
	
	public abstract long getLongProperty(String name, long defaultValue);
	
	public abstract double getDoubleProperty(String name, double defaultValue);
	
	public abstract float getFloatProperty(String name, float defaultValue);
}
