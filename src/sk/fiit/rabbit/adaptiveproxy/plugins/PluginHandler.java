package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sk.fiit.rabbit.adaptiveproxy.utils.MD5ChecksumGenerator;
import sk.fiit.rabbit.adaptiveproxy.utils.XMLFileParser;

public class PluginHandler {
	private static final Logger log = Logger.getLogger(PluginHandler.class);
	private static String filepathSeparator = System.getProperty("file.separator");
	
	private File pluginRepositoryDir;
	//private URLClassLoader classLoader;
	private Set<String> excludeFileNames;
	
	private final List<PluginConfigEntry> configEntries;
	private final Map<PluginConfigEntry, ProxyPlugin> ldPlugin4EntryMap;
	private final Map<ProxyPlugin, PluginConfigEntry> entry4ldPluginMap;
	private final Map<String, Set<? extends ProxyPlugin>> ldPlugins4TypeMap;
	private final Map<Class<?>, String> checksums4ldClassMap;
	private final AbcPluginsComparator comparator;
	
	public PluginHandler() {
		configEntries = new LinkedList<PluginConfigEntry>();
		ldPlugin4EntryMap = new LinkedHashMap<PluginConfigEntry, ProxyPlugin>();
		entry4ldPluginMap = new LinkedHashMap<ProxyPlugin, PluginConfigEntry>();
		ldPlugins4TypeMap = new LinkedHashMap<String, Set<? extends ProxyPlugin>>();
		checksums4ldClassMap = new HashMap<Class<?>, String>();
		comparator = new AbcPluginsComparator();
	}
	
	public void setPluginRepository(File pluginRepositoryDir, Set<String> excludeFileNames) {
		if (!pluginRepositoryDir.isDirectory()) {
			throw new IllegalArgumentException("Argument does not denote a directory");
		}
		this.pluginRepositoryDir = pluginRepositoryDir;
		this.excludeFileNames = excludeFileNames;
	}
	
	public synchronized void reloadPlugins() {
		if (pluginRepositoryDir == null)
			return;
		ldPlugins4TypeMap.clear();
		entry4ldPluginMap.clear();
		loadPluginsConfigs();
		Map<PluginConfigEntry, ProxyPlugin> tmpPlugin4EntryMap = new LinkedHashMap<PluginConfigEntry, ProxyPlugin>();
		for (PluginConfigEntry oldCfgEntry : ldPlugin4EntryMap.keySet()) {
			ProxyPlugin loadedPlugin = ldPlugin4EntryMap.get(oldCfgEntry);
			boolean supportsReconfigure = false;
			try {
				supportsReconfigure = loadedPlugin.supportsReconfigure();
			} catch (Exception e) {
				log.warn("Exception occured while calling supportsReconfigure() on '"+loadedPlugin+"' of class '"+loadedPlugin.getClass()+"'");
			}
			if (supportsReconfigure) {
				log.debug("Loaded plugin '"+loadedPlugin+"' supports reconfiguring");
				PluginConfigEntry newCfgEntry = null;
				for (PluginConfigEntry cfgEntry : configEntries) {
					if (cfgEntry.className.equals(oldCfgEntry.className) &&
							cfgEntry.classLocation.equals(oldCfgEntry.classLocation)) {
						newCfgEntry = cfgEntry;
						break;
					}
				}
				if (newCfgEntry != null) {
					Class<?> newClazz = null;
					try {
						newClazz = loadClass(newCfgEntry.className, newCfgEntry.classLoader);
					} catch (ClassNotFoundException e) {
						log.warn("Plugin '"+newCfgEntry.name+"' | plugin class '"+newCfgEntry.className+"' not found at '"+
								new File(".",newCfgEntry.classLocation).getAbsolutePath()+"'", e);
					}
					Class<?> oldClass = loadedPlugin.getClass();
					String newClassChecksum = null;
					if (newClazz != null) {
						newClassChecksum = checksums4ldClassMap.get(newClazz);
						if (oldClass == newClazz || checksums4ldClassMap.get(oldClass).equals(newClassChecksum)) {
							log.debug("Seems like class '"+newClazz.getName()+"' hasn't changed, so we try to keep already loaded plugin '"+loadedPlugin+"'");
							if (setupPlugin(loadedPlugin, newCfgEntry.properties)) {
								tmpPlugin4EntryMap.put(newCfgEntry,loadedPlugin);
								entry4ldPluginMap.put(loadedPlugin, newCfgEntry);
								if (newClazz != loadedPlugin.getClass())
									checksums4ldClassMap.remove(newClazz);
								log.debug("Loaded plugin '"+loadedPlugin+"' preserved");
								continue;
							} else
								log.debug("Plugin of class '"+newCfgEntry.className+"' is not reconfigured properly, it was stoped and thrown away");
						} else
							log.debug("Seems like class '"+newClazz.getName()+"' has changed, so we won't keep already loaded plugin '"+loadedPlugin+"'");
					}
				}
			} else
				log.debug("Loaded plugin '"+loadedPlugin+"' does not support reconfiguring at it's current state");
			checksums4ldClassMap.remove(loadedPlugin.getClass());
			stopPlugin(loadedPlugin);
		}
		ldPlugin4EntryMap.clear();
		ldPlugin4EntryMap.putAll(tmpPlugin4EntryMap);
	}
	
	private boolean setupPlugin(ProxyPlugin plugin, PluginProperties props) {
		log.info("Setting up plugin '"+plugin+"'");
		try {
			return plugin.setup(props);
		} catch (Exception e) {
			log.warn("Exception occured while seting up plugin '"+plugin+"' of class '"+plugin.getClass()+"'");
		}
		return false;
	}
	
	private boolean startPlugin(ProxyPlugin plugin) {
		log.info("Starting plugin '"+plugin+"'");
		try {
			plugin.start();
			return true;
		} catch (Exception e) {
			log.warn("Exception occured while starting plugin '"+plugin+"' of class '"+plugin.getClass()+"'");
		}
		return false;
	}
	
	private boolean stopPlugin(ProxyPlugin plugin) {
		log.info("Stopping loaded plugin '"+plugin+"'");
		try {
			plugin.stop();
			return true;
		} catch (Exception e) {
			log.warn("Exception occured while stoping plugin '"+plugin+"' of class '"+plugin.getClass()+"'");
		}
		return false;
	}
	
	class PluginsXMLFileFilter implements FilenameFilter {
		final Set<String> excludeFileNames;
		
		public PluginsXMLFileFilter(Set<String> excludeFileNames) {
			this.excludeFileNames = excludeFileNames;
		}
		
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".xml") && !excludeFileNames.contains(name);
		}
	}
	
	class PluginConfigEntry {
		final String name;
		final String className;
		final String classLocation;
		final List<String> types;
		final PluginProperties properties;
		ClassLoader classLoader;
		boolean classLocValid = true;
		
		public PluginConfigEntry(String name, String className, String classLocation,
				List<String> types, PluginProperties properties) {
			this.name = name;
			this.className = className;
			this.classLocation = classLocation;
			this.types = types;
			this.properties = properties;
		}
	}
	
	private synchronized void loadPluginsConfigs() {
		configEntries.clear();
		excludeFileNames.add("services.xml");
		File[] configFiles = pluginRepositoryDir.listFiles(new PluginsXMLFileFilter(excludeFileNames));
		Set<String> pluginNames = new HashSet<String>();
		Map<URI, ClassLoader> cLoaders = new HashMap<URI, ClassLoader>();
		for (File file : configFiles) {
			Document document = XMLFileParser.parseFile(file);
			if (document != null) {
				PluginConfigEntry cfgEntry = loadPluginConfig(document);
				if (pluginNames.contains(cfgEntry.name)) {
					log.warn("Duplicate plugin name '"+cfgEntry.name+"', plugin config of which is stored in file "+file.getAbsolutePath()+" won't be available");
					continue;
				}
				URI classLocURI = new File(pluginRepositoryDir,cfgEntry.classLocation).toURI();
				cfgEntry.classLoader = cLoaders.get(classLocURI);
				if (cfgEntry.classLoader == null) {
					URL url = null;
					try {
						url = classLocURI.toURL();
					} catch (MalformedURLException e) {
						log.warn("Class location '"+cfgEntry.classLocation+"' for the plugin '"+cfgEntry.name+
								"' is not a valid path. This plugin will not be loaded");
						continue;
					}
					URL[] urls = new URL[] {url};
					cfgEntry.classLoader = URLClassLoader.newInstance(urls);
					cLoaders.put(classLocURI, cfgEntry.classLoader);
					log.trace("Plugin '"+cfgEntry.name+"' will be loaded by new ClassLoader "+cfgEntry.classLoader+" with URLs set to "+Arrays.toString(urls));
				} else
					log.trace("Plugin '"+cfgEntry.name+"' shares already created ClassLoader "+cfgEntry.classLoader);
				configEntries.add(cfgEntry);
				pluginNames.add(cfgEntry.name);
			} else
				log.warn("Corrupted plugin configuration file "+file.getAbsolutePath());
		}
	}
	
	private PluginConfigEntry loadPluginConfig(Document doc) {
		Element docRoot = doc.getDocumentElement();
		Element pluginNameElement = (Element)docRoot.getElementsByTagName("name").item(0);
		String pluginName = pluginNameElement.getTextContent();
		Element classLocationElement = (Element)docRoot.getElementsByTagName("classLocation").item(0);
		String classLocation = "";
		if (classLocationElement != null)
			classLocation = classLocationElement.getTextContent();
		Element classNameElement = (Element)docRoot.getElementsByTagName("className").item(0);
		String className = classNameElement.getTextContent();
		Element typesElement = (Element)docRoot.getElementsByTagName("types").item(0);
		NodeList types = typesElement.getElementsByTagName("type");
		List<String> pluginTypes = new ArrayList<String>(types.getLength());
		for (int i = 0; i < types.getLength(); i++) {
			Element type = (Element)types.item(i);
			pluginTypes.add(type.getTextContent());
		}
		Element parametersElement = (Element)docRoot.getElementsByTagName("parameters").item(0);
		NodeList params = parametersElement.getElementsByTagName("param");
		PluginPropertiesImpl properties = new PluginPropertiesImpl();
		for (int i = 0; i < params.getLength(); i++) {
			Element param = (Element)params.item(i);
			properties.addProperty(param.getAttribute("name"), param.getTextContent());
		}
		return new PluginConfigEntry(pluginName,className,classLocation,pluginTypes,properties);
	}
	
	private List<PluginConfigEntry> getEntriesForType(String type) {
		List<PluginConfigEntry> retVal = new LinkedList<PluginConfigEntry>();
		for (PluginConfigEntry cfgEntry : configEntries) {
			if (cfgEntry.types.contains(type))
				retVal.add(cfgEntry);
		}
		return retVal;
	}
	
	<T> T getInstance(Class<T> clazz) {
		T instance = null;
		try {
			instance = clazz.newInstance();
		} catch (InstantiationException e) {
			log.warn("Unable to create an instance of the class '"+clazz.getName()+"'",e);
		} catch (IllegalAccessException e) {
			log.warn("Instantiation forbidden for class '"+clazz.getName()+"'",e);
		}
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends ProxyPlugin> Class<T> getClass(PluginConfigEntry cfgEntry, Class<T> asClass) {
		Class<?> clazz = null;
		try {
			clazz = loadClass(cfgEntry.className, cfgEntry.classLoader);
		} catch (ClassNotFoundException e) {
			cfgEntry.classLocValid = false;
			log.warn("Plugin '"+cfgEntry.name+"' | plugin class '"+cfgEntry.className+"' not found at '"+
					new File(pluginRepositoryDir,cfgEntry.classLocation).getAbsolutePath()+"'");
			return null;
		}
		if (!asClass.isAssignableFrom(clazz)) {
			log.warn("Found class '"+clazz.getName()+"' is not a subclass of '"+
					asClass.getName()+"' class/interface");
			return null;
		}
		return (Class<T>) clazz.asSubclass(asClass);
	}
	
	private File getClassFile(Class<?> clazz) {
		File classFile = null;
		URI classFileUri = null;
		try {
			classFileUri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
		} catch (URISyntaxException e) {
			log.warn("Unable to get code source location for class '"+clazz.getName()+"'");
			return null;
		}
		File codeSourceFile = new File(classFileUri);
		if (codeSourceFile.isFile())
			classFile = codeSourceFile;
		else {
			String classPath = clazz.getName().replace(".", filepathSeparator);
			classFile = new File(codeSourceFile,classPath+".class");
			if (!classFile.isFile()) {
				log.warn("Unable to find actual class at "+classFile.getAbsolutePath());
			}
		}
		return classFile;
	}
	
	private Class<?> loadClass(String className, ClassLoader cLoader) throws ClassNotFoundException {
		Class<?> clazz = cLoader.loadClass(className);
		try {
			File classFile = getClassFile(clazz);
			log.trace("File from which the class '"+clazz.getSimpleName()+"' was loaded: "+classFile.toString());
			checksums4ldClassMap.put(clazz, MD5ChecksumGenerator.createHexChecksum(classFile));
		} catch (IOException e) {
			log.warn("Error while reading class file for MD5 checksum computing");
		} 
		return clazz;
	}
	
	private  <T extends ProxyPlugin> T getPlugin(PluginConfigEntry cfgEntry, Class<T> asClass) {
		T plugin = null;
		ProxyPlugin loadedPlugin = ldPlugin4EntryMap.get(cfgEntry);
		if (loadedPlugin != null) {
			try {
				plugin = asClass.cast(loadedPlugin);
			} catch (ClassCastException e) {
				log.warn("Plugin '"+cfgEntry.name+"' | plugin class '"+cfgEntry.className+"' is not a subclass of '"
						+asClass.getName()+"' class/interface");
			}
		} else if (cfgEntry.classLocValid) {
			Class<T> clazz = getClass(cfgEntry, asClass);
			if (clazz != null) {
				plugin = getInstance(clazz);
				if (plugin != null) {
					if (setupPlugin(plugin, cfgEntry.properties) && startPlugin(plugin)) {
						ldPlugin4EntryMap.put(cfgEntry, plugin);
						entry4ldPluginMap.put(plugin, cfgEntry);
					} else {
						log.debug("Plugin of class '"+cfgEntry.className+"' is not set up properly, it is thrown away");
						plugin = null;
					}
				}
			}
		}
		return plugin;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T extends ProxyPlugin> Set<T> getPlugins(String type, Class<T> asClass) {
		Set<T> plugins = null;
		Set<? extends ProxyPlugin> loadedPlugins = ldPlugins4TypeMap.get(type);
		if (loadedPlugins != null) {
			plugins = (Set<T>) loadedPlugins;
		} else {
			plugins = new LinkedHashSet<T>();
			List<PluginConfigEntry> cfgEntries = getEntriesForType(type);
			if (cfgEntries != null) {
				for (PluginConfigEntry cfgEntry : cfgEntries) {
					T plugin = getPlugin(cfgEntry, asClass);
					if (plugin != null)
						plugins.add(plugin);
				}
			}
			ldPlugins4TypeMap.put(type,plugins);
		}
		Set<T> retVal = new LinkedHashSet<T>();
		retVal.addAll(plugins);
		return retVal;
	}

	public synchronized <T extends ProxyPlugin> Set<T> getPlugins(Class<T> asClass) {
		return getPlugins(asClass.getSimpleName(), asClass);
	}
	
	public synchronized <T extends ProxyPlugin> T getPlugin(String pluginName, Class<T> asClass) {
		T plugin = null;
		// this method is called only at the beginning (when AdaptiveEngine is ordering
		// processing plugins according to ordering file), so it's not that bad to
		// iterate over and over again. I've chosen this way instead of another map (names->plugins).
		for (PluginConfigEntry cfgEntry : configEntries) {
			if (cfgEntry.name.equals(pluginName))
				plugin = getPlugin(cfgEntry, asClass);
		}
		return plugin;
	}
	
	/*public <T extends ProxyPlugin> List<String> getLoadedPluginNames(Class<T> ofClass) {
		List<String> retVal = new LinkedList<String>();
		Set<? extends ProxyPlugin> loadedPlugins = ldPlugins4TypesMap.get(ofClass.getSimpleName());
		if (loadedPlugins != null) {
			for (ProxyPlugin proxyPlugin : loadedPlugins) {
				retVal.add(configs4PluginsMap.get(proxyPlugin).name);
			}
		}
		return retVal;
	}*/
	
	public synchronized String getPluginName(ProxyPlugin plugin) {
		return entry4ldPluginMap.get(plugin).name;
	}
	
	private class AbcPluginsComparator implements Comparator<ProxyPlugin> {
		@Override
		public int compare(ProxyPlugin o1, ProxyPlugin o2) {
			return entry4ldPluginMap.get(o1).name.compareTo(entry4ldPluginMap.get(o2).name);
		}
	}
	
	public synchronized <T extends ProxyPlugin> List<String> getLoadedPluginNames() {
		List<String> retVal = new LinkedList<String>();
		List<ProxyPlugin> ldPlugins = new LinkedList<ProxyPlugin>();
		ldPlugins.addAll(entry4ldPluginMap.keySet());
		Collections.sort(ldPlugins, comparator);
		for (ProxyPlugin proxyPlugin : ldPlugins) {
			retVal.add(entry4ldPluginMap.get(proxyPlugin).name);
		}
		return retVal;
	}
	
	public synchronized List<String> getTypesOfPlugin(ProxyPlugin plugin) {
		List<String> retVal = new LinkedList<String>();
		for (String type : entry4ldPluginMap.get(plugin).types) {
			if (ldPlugins4TypeMap.get(type).contains(plugin))
				retVal.add(type);
		}
		return retVal;
	}
}
