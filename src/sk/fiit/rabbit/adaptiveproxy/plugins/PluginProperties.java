package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.io.File;
import java.util.Scanner;

/**
 * Configuration properties for a proxy plugin loaded from the plugin's configuration
 * file in the plugins folder. Each property is a <code>String</code> value for a
 * <code>String</code> key. Besides methods returning particular <code>String</code>
 * value for a key, this class also defines five convenience methods that return
 * value as <code>boolean</code>, <code>int</code>, <code>long</code>,
 * <code>double</code> or <code>float</code>.  
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface PluginProperties {
	/**
	 * Returns <code>String</code> value for a configuration property with the given
	 * <code>name</code> if it is present, otherwise returns <code>null</code>.
	 * @param name name of the configuration property
	 * @return value for the property, or <code>null</code> if no such
	 * property is present
	 */
	String getProperty(String name);
	
	/**
	 * Returns <code>String</code> value for a configuration property with the given
	 * <code>name</code> if it is present, otherwise returns passed <code>defaultValue</code>.
	 * @param name name of the configuration property
	 * @param defaultValue default value for a property
	 * @return value for the property, or passed <code>defaultValue</code> if no such
	 * property is present 
	 */
	String getProperty(String name, String defaultValue);
	
	/**
	 * Returns <code>boolean</code> value for a configuration property with the given
	 * <code>name</code> if it is present and can be converted to <code>boolean</code>
	 * using {@link Scanner#nextBoolean()}, otherwise returns passed <code>defaultValue</code>.
	 * @param name name of the configuration property
	 * @param defaultValue default value for a property
	 * @return value for the property, or <code>defaultValue</code> if no such
	 * property is present or could not be converted
	 */
	boolean getBoolProperty(String name, boolean defaultValue);
	
	/**
	 * Returns <code>int</code> value for a configuration property with the given
	 * <code>name</code> if it is present and can be converted to <code>int</code>
	 * using {@link Integer#valueOf(String)}, otherwise returns passed <code>defaultValue</code>.
	 * @param name name of the configuration property
	 * @param defaultValue default value for a property
	 * @return value for the property, or <code>defaultValue</code> if no such
	 * property is present or could not be converted
	 */
	int getIntProperty(String name, int defaultValue);
	
	/**
	 * Returns <code>long</code> value for a configuration property with the given
	 * <code>name</code> if it is present and can be converted to <code>long</code>
	 * using {@link Long#valueOf(String)}, otherwise returns passed <code>defaultValue</code>.
	 * @param name name of the configuration property
	 * @param defaultValue default value for a property
	 * @return value for the property, or <code>defaultValue</code> if no such
	 * property is present or could not be converted
	 */
	long getLongProperty(String name, long defaultValue);
	
	/**
	 * Returns <code>double</code> value for a configuration property with the given
	 * <code>name</code> if it is present and can be converted to <code>double</code>
	 * using {@link Double#valueOf(String)}, otherwise returns passed <code>defaultValue</code>.
	 * @param name name of the configuration property
	 * @param defaultValue default value for a property
	 * @return value for the property, or <code>defaultValue</code> if no such
	 * property is present or could not be converted
	 */
	double getDoubleProperty(String name, double defaultValue);
	
	/**
	 * Returns <code>float</code> value for a configuration property with the given
	 * <code>name</code> if it is present and can be converted to <code>float</code>
	 * using {@link Float#valueOf(String)}, otherwise returns passed <code>defaultValue</code>.
	 * @param name name of the configuration property
	 * @param defaultValue default value for a property
	 * @return value for the property, or <code>defaultValue</code> if no such
	 * property is present or could not be converted
	 */
	float getFloatProperty(String name, float defaultValue);
	
	/**
	 * Returns a <code>File</code> object pointing to plugin's specific deployment directory
	 * as set in plugin's configuration file.
	 * @return a file pointing to plugin's deployment directory 
	 */
	File getRootDir();
}
