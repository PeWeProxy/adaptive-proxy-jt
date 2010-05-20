package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.io.File;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

public final class PluginPropertiesImpl implements PluginProperties {
	private final File rootFile;
	private final Map<String, String> properties;
	
	public PluginPropertiesImpl(File rootFile, Map<String, String> properties) {
		this(rootFile);
		this.properties.putAll(properties);
	}
	
	public PluginPropertiesImpl(File rootFile) {
		properties = new HashMap<String, String>();
		this.rootFile = rootFile;
	}
	
	public void addProperty(String name, String property) {
		properties.put(name, property);
	}
	
	@Override
	public boolean getBoolProperty(String name, boolean defaultValue) {
		boolean retVal = defaultValue;
		String property = properties.get(name);
		if (property != null) {
			try {
				retVal = new Scanner(property).nextBoolean();
			} catch (InputMismatchException ignorede) {}
		}
		return retVal;
	}

	@Override
	public double getDoubleProperty(String name, double defaultValue) {
		double retVal = defaultValue;
		String property = properties.get(name);
		if (property != null) {
			try {
				retVal = Double.valueOf(property);
			} catch (NumberFormatException ignorede) {}
		}
		return retVal;
	}

	@Override
	public float getFloatProperty(String name, float defaultValue) {
		float retVal = defaultValue;
		String property = properties.get(name);
		if (property != null) {
			try {
				retVal = Float.valueOf(property);
			} catch (NumberFormatException ignorede) {}
		}
		return retVal;
	}

	@Override
	public int getIntProperty(String name, int defaultValue) {
		int retVal = defaultValue;
		String property = properties.get(name);
		if (property != null) {
			try {
				retVal = Integer.valueOf(property);
			} catch (NumberFormatException ignorede) {}
		}
		return retVal;
	}

	@Override
	public long getLongProperty(String name, long defaultValue) {
		long retVal = defaultValue;
		String property = properties.get(name);
		if (property != null) {
			try {
				retVal = Long.valueOf(property);
			} catch (NumberFormatException ignorede) {}
		}
		return retVal;
	}

	@Override
	public String getProperty(String name) {
		return properties.get(name);
	}

	@Override
	public String getProperty(String name, String defaultValue) {
		String retVal = defaultValue;
		String property = properties.get(name);
		if (property != null)
			retVal = property;
		return retVal;
	}
	
	@Override
	public String toString() {
		return properties.toString();
	}

	@Override
	public File getRootDir() {
		return rootFile;
	}
}
