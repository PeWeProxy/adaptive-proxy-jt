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
import java.util.Iterator;
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
	private static final String ELEMENT_PLUGIN = "plugin";
	private static final String ELEMENT_NAME = "name";
	private static final String ELEMENT_CLASSLOC = "classLocation";
	private static final String ELEMENT_CLASSNAME = "className";
	private static final String ELEMENT_LIBS = "libraries";
	private static final String ELEMENT_LIB = "lib";
	private static final String ELEMENT_TYPES = "types";
	private static final String ELEMENT_TYPE = "type";
	private static final String ELEMENT_PARAMS = "parameters";
	private static final String ELEMENT_PARAM = "param";
	private static final String ATTR_NAME = "name";
	
	private static final FilenameFilter jarFilter;
	private static final FilenameFilter classFilter;
	private static final FilenameFilter loadableFilter;
	
	private File pluginRepositoryDir;
	private File sharedLibsDir;
	private URL[] sharedLibsURLs;
	private Set<String> excludeFileNames;
	
	private final List<PluginConfigEntry> configEntries;
	private final Map<PluginConfigEntry, ProxyPlugin> ldPlugin4EntryMap;
	private final Map<ProxyPlugin, PluginConfigEntry> entry4ldPluginMap;
	private final Map<String, Set<? extends ProxyPlugin>> ldPlugins4TypeMap;
	private final Map<Class<?>, String> checksums4ldClassMap;
	private final Map<URL, String> checksums4ldLibsMap;
	private final AbcPluginsComparator comparator;
	
	static {
		jarFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		};
		classFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".class");
			}
		};
		
		loadableFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return jarFilter.accept(dir, name) || classFilter.accept(dir, name);
			}
		};
	}
	
	public PluginHandler() {
		configEntries = new LinkedList<PluginConfigEntry>();
		ldPlugin4EntryMap = new LinkedHashMap<PluginConfigEntry, ProxyPlugin>();
		entry4ldPluginMap = new LinkedHashMap<ProxyPlugin, PluginConfigEntry>();
		ldPlugins4TypeMap = new LinkedHashMap<String, Set<? extends ProxyPlugin>>();
		checksums4ldClassMap = new HashMap<Class<?>, String>();
		checksums4ldLibsMap = new HashMap<URL, String>();
		comparator = new AbcPluginsComparator();
	}
	
	public void setPluginRepository(File pluginRepositoryDir, File sharedLibsDir, Set<String> excludeFileNames) {
		if (!pluginRepositoryDir.isDirectory()) {
			throw new IllegalArgumentException("Argument does not denote a directory");
		}
		this.sharedLibsDir = sharedLibsDir;
		this.pluginRepositoryDir = pluginRepositoryDir;
		this.excludeFileNames = excludeFileNames;
		String path = pluginRepositoryDir.getAbsolutePath();
		try {
			path = pluginRepositoryDir.getCanonicalPath();
		} catch (IOException e) {
			log.info("Error when converting plugins home directory file '"+path+"' to cannonical form");
		}
		log.info("Plugins home directory is set to "+path);
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
		final List<String> libraries;
		final List<String> types;
		final PluginProperties properties;
		ClassLoader classLoader;
		final Set<URL> libsrariesURLSet;
		boolean classLocValid = true;
		boolean loadError = false;
		
		public PluginConfigEntry(String name, String className, String classLocation,
				List<String> libraries, List<String> types, PluginProperties properties) {
			this.name = name;
			this.className = className;
			this.classLocation = classLocation;
			this.libraries = libraries;
			this.libsrariesURLSet = new HashSet<URL>();
			this.types = types;
			this.properties = properties;
		}
	}

	private void createClassLoaders(Map<URL, String> newLibChecksums) {
		Map<URL, ClassLoader> cLoaders = new HashMap<URL, ClassLoader>();
		for (Iterator<PluginConfigEntry> iterator = configEntries.iterator(); iterator.hasNext();) {
			PluginConfigEntry cfgEntry =  iterator.next();
			boolean libsOK = true;
			for (String libLocation : cfgEntry.libraries) {
				File libRelFile = new File(pluginRepositoryDir,libLocation);
				File libFile = null;
				try {
					libFile = libRelFile.getCanonicalFile();
				} catch (IOException e1) {
					log.info("Error when converting library file '"+libRelFile.getAbsolutePath()+"' to cannonical form");
				}
				if (libFile == null || !libFile.canRead()) {
					// unable to locate library file (jar/dir)
					log.info("Library location '"+libLocation+"' for the plugin '"+cfgEntry.name+
						"' does not point to valid jar/directory, this plugin will not be loaded");
					libsOK = false;
					break;
				}
				URI libLocURI = libFile.toURI();
				URL url = null;
				try {
					url = libLocURI.toURL();
				} catch (MalformedURLException e) {
					log.info("Error when converting valid library file path '"+libFile.getAbsolutePath()+"' to URL, plugin '"+cfgEntry.name
							+" will not be loaded");
					libsOK = false;
					break;
				}
				
				try {
					newLibChecksums.put(url, MD5ChecksumGenerator.createHexChecksum(libFile,classFilter));
				} catch (IOException e) {
					log.info("Error when converting library file '"+libRelFile.getAbsolutePath()+"' for MD5 checksum computing");
				}
				cfgEntry.libsrariesURLSet.add(url);
			}
			if (!libsOK) {
				// something went wrong with creating URLClassLoader for this library entry
				iterator.remove();
				continue;
			}
			File pluginRelFile = new File(pluginRepositoryDir,cfgEntry.classLocation); 
			File pluginFile = null;
			try {
				pluginFile = pluginRelFile.getCanonicalFile();
			} catch (IOException e1) {
				log.info("Error when converting plugin class file '"+pluginRelFile.getAbsolutePath()+"' to cannonical form");
			}
			if (pluginFile == null || !pluginFile.canRead()) {
				// unable to locate plugin's binary file (jar/dir)
				log.info("Can not read classpath file/directory "+pluginFile.getAbsolutePath()+",  Plugin '"+cfgEntry.name
					+"' will not be loaded");
				iterator.remove();
				continue;
			}
			URI classLocURI = pluginFile.toURI();
			URL classLocURL = null;
			try {
				classLocURL = classLocURI.toURL();
			} catch (MalformedURLException e) {
				log.info("Error when converting valid plugin class file path '"+pluginFile.getAbsolutePath()+"' to URL, plugin '"+cfgEntry.name
					+" will not be loaded");
				continue;
			}
			ClassLoader sharedClassLoader = cLoaders.get(classLocURL);
			// check if the parent class loader uses the same libraries
			if (sharedClassLoader != null) {
				ClassLoader libsClassLoader = sharedClassLoader.getParent();
				if (sharedLibsURLs != null)
					libsClassLoader = libsClassLoader.getParent();
				if (sameURLsInCLoader(libsClassLoader, cfgEntry.libsrariesURLSet)) {
					cfgEntry.classLoader = sharedClassLoader;
					log.debug("Plugin '"+cfgEntry.name+"' shares already created ClassLoader "+cfgEntry.classLoader);
				} else {
					log.debug("Plugin '"+cfgEntry.name+"' can not share already created ClassLoader "+sharedClassLoader
						+" because of different library dependencies");
				}
			}
			if (cfgEntry.classLoader == null) {
				// plugin can not share existing ClassLoader
				URLClassLoader parentCLoader = null;
				URL[] urls = new URL[] {classLocURL};
				if (!cfgEntry.libsrariesURLSet.isEmpty()) {
					URL[] libsURLs = cfgEntry.libsrariesURLSet.toArray(new URL[cfgEntry.libsrariesURLSet.size()]);
					parentCLoader = createClassLoader(libsURLs, null, cfgEntry); 
					parentCLoader = createClassLoader(urls, parentCLoader, cfgEntry); 
				} else {
					parentCLoader = createClassLoader(urls, null, cfgEntry);
				}
				if (sharedLibsURLs != null) {
					cfgEntry.classLoader = parentCLoader = createClassLoader(sharedLibsURLs, parentCLoader, cfgEntry);
				} else
					cfgEntry.classLoader = parentCLoader;
				cLoaders.put(classLocURL, cfgEntry.classLoader);
				log.debug("Plugin '"+cfgEntry.name+"' may be (if such need occurs) loaded by new ClassLoader "+cfgEntry.classLoader+" with URLs set to "+Arrays.toString(urls)
						+" with parent ClassLoader set to "+cfgEntry.classLoader.getParent());
			}
		}
	}
	
	private URLClassLoader createClassLoader(URL[] urls, URLClassLoader parent, PluginConfigEntry cfgEntry) {
		URLClassLoader retVal = null;
		if (parent != null)
			retVal = URLClassLoader.newInstance(urls,parent);
		else
			retVal = URLClassLoader.newInstance(urls);
		log.debug("Creating new ClassLoader "+retVal+" with URLs set to "+Arrays.toString(retVal.getURLs())
				+" with parent ClassLoader set to "+retVal.getParent()+ " for potential use by plugin '"+cfgEntry.name+"'");
		return retVal;
	}
	
	private File[] getNestedFiles(File dir, FilenameFilter nameFilter) {
		int filesNum = 0;
		File[] childs = dir.listFiles();
		List<File[]> nestedFiles = new LinkedList<File[]>();
		List<File> directChilds = new LinkedList<File>();
		for (File child : childs) {
			if (child.isDirectory()) {
				File[] nestedFilesArr = getNestedFiles(child, nameFilter);
				nestedFiles.add(nestedFilesArr);
				filesNum += nestedFilesArr.length;
			} else if (nameFilter.accept(dir, child.getName()))
				directChilds.add(child);
		}
		filesNum += directChilds.size();
		File[] retVal = new File[filesNum];
		int pos = 0;
		for (File file : directChilds) {
			retVal[pos++] = file;
		}
		for (File[] files : nestedFiles) {
			System.arraycopy(files, 0, retVal, pos, files.length);
			pos += files.length;
		}
		return retVal;
	}
	
	private void createSharedLibsURLs(Map<URL, String> newLibChecksums) {
		if (sharedLibsDir != null && sharedLibsDir.isDirectory() && sharedLibsDir.canRead()) {
			URL sharedDirURL = null;
			try {
				sharedDirURL = sharedLibsDir.toURI().toURL();
			} catch (MalformedURLException e) {
				log.warn("Error when converting valid shared libraries directory path '"+sharedLibsDir.getAbsolutePath()
						+"' to URL, no shared libraries will be used");
			}
			if (sharedDirURL != null) {
				String path = sharedLibsDir.getAbsolutePath();
				try {
					path = sharedLibsDir.getCanonicalPath();
				} catch (IOException e) {
					log.info("Error when converting shared libraries directory file '"+path+"' to cannonical form");
				}
				log.info("Using shared libraries directory "+path);
				File[] jarFiles = getNestedFiles(sharedLibsDir, jarFilter);
				URL[] urls = new URL[jarFiles.length+1];
				urls[0] = sharedDirURL;
				int i = 1;
				for (File lib : jarFiles) {
					try {
						lib = lib.getCanonicalFile();
					} catch (IOException e1) {
						log.info("Error when converting shared library file '"+lib.getAbsolutePath()+"' to cannonical form");
						return;
					}
					try {
						urls[i++] = lib.toURI().toURL();
						log.debug("Using shared library "+urls[i-1]);
					} catch (MalformedURLException e) {
						log.warn("Error when converting valid shared library file path '"+lib.getAbsolutePath()
								+"' to URL, no shared libraries will be used");
					}
				}
				try {
					newLibChecksums.put(sharedDirURL, MD5ChecksumGenerator.createHexChecksum(sharedLibsDir,loadableFilter));
				} catch (IOException e) {
					log.info("Error when reading shared libraries direcotry '"+sharedLibsDir.getAbsolutePath()+"' for MD5 checksum computing");
				}
				sharedLibsURLs = urls;
			}
		} else
			log.info("Can not access shared libraries directory "+sharedLibsDir.getAbsolutePath()+", no shared libraries will be used");
	}
	
	public synchronized void reloadPlugins() {
		if (pluginRepositoryDir == null)
			return;
		ldPlugins4TypeMap.clear();
		entry4ldPluginMap.clear();
		loadPluginsConfigs();
		Map<URL, String> newLibChecksums = new HashMap<URL, String>();
		createSharedLibsURLs(newLibChecksums);
		createClassLoaders(newLibChecksums);
		
		URL sharedDirURL = null;
		try {
			sharedDirURL = sharedLibsDir.toURI().toURL();
		} catch (MalformedURLException e) {
			log.warn("Error when converting valid shared libraries directory path '"+sharedLibsDir.getAbsolutePath()
					+"' to URL, no shared libraries will be used");
		}
		boolean sharedLibsDirSame = false;
		if (sharedDirURL != null) { 
			String newChecksum = newLibChecksums.get(sharedDirURL);
			if (newChecksum != null)
				sharedLibsDirSame = newChecksum.equals(checksums4ldLibsMap.get(sharedDirURL));
		}
		if (sharedLibsDirSame)
			log.debug("Shared libraries directory hasn not been changed");
		Map<PluginConfigEntry, ProxyPlugin> tmpPlugin4EntryMap = new LinkedHashMap<PluginConfigEntry, ProxyPlugin>();
		for (PluginConfigEntry oldCfgEntry : ldPlugin4EntryMap.keySet()) {
			ProxyPlugin loadedPlugin = ldPlugin4EntryMap.get(oldCfgEntry);
			PluginConfigEntry newCfgEntry = null;
			if (sharedLibsDirSame) {
				// try to find matching oldCfgEntry
				for (PluginConfigEntry cfgEntry : configEntries) {
					if (cfgEntry.className.equals(oldCfgEntry.className) &&
							cfgEntry.classLocation.equals(oldCfgEntry.classLocation)) {
						newCfgEntry = cfgEntry;
						break;
					}
				};
			} else
				log.debug("Shared libraries directory has been changed so plugin '"+loadedPlugin+"' will be reloaded");
			if (newCfgEntry != null && oldCfgEntry.libsrariesURLSet.equals(newCfgEntry.libsrariesURLSet)) {
				boolean libsChanged = false;
				for (URL libURL : newCfgEntry.libsrariesURLSet) {
					String newChecksum = newLibChecksums.get(libURL);
					if (!newChecksum.equals(checksums4ldLibsMap.get(libURL))) {
						libsChanged = true;
						break;
					}
				}
				if (!libsChanged) {
					boolean supportsReconfigure = false;
					try {
						supportsReconfigure = loadedPlugin.supportsReconfigure();
					} catch (Throwable t) {
						log.info("Throwable raised while calling supportsReconfigure() on '"+loadedPlugin+"' of class '"+loadedPlugin.getClass()+"'",t);
					}
					if (supportsReconfigure) {
						log.debug("Loaded plugin '"+loadedPlugin+"' supports reconfiguring at it's current state");
						if (newCfgEntry != null) {
							Class<?> newClazz = null;
							try {
								newClazz = loadClass(newCfgEntry.className, newCfgEntry.classLoader);
							} catch (ClassNotFoundException e) {
								log.info("Plugin '"+newCfgEntry.name+"' | plugin class '"+newCfgEntry.className+"' not found at '"+
										new File(pluginRepositoryDir,newCfgEntry.classLocation).getAbsolutePath()+"'", e);
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
										newCfgEntry.classLoader = oldClass.getClassLoader();
										log.debug("Loaded plugin '"+loadedPlugin+"' preserved, no classes (re)loading will ocur");
										continue;
									} else
										log.debug("Plugin of class '"+newCfgEntry.className+"' is not reconfigured properly, it was stoped and thrown away");
								} else {
									log.debug("Seems like class '"+newClazz.getName()+"' has changed, thus plugin '"+loadedPlugin+"' will be reloaded");
								}
							}
						}
					} else
						log.debug("Loaded plugin '"+loadedPlugin+"' does not support reconfiguring at it's current state so it'll be reloaded");
				} else {
					log.debug("Dependencies of plugin '"+loadedPlugin+"' changed so it'll be reloaded");
				}
			} else {
				if (sharedLibsDirSame)
					log.debug("New or changed configuration of plugin '"+loadedPlugin+"', plugin will be reloaded");
			}
			checksums4ldClassMap.remove(loadedPlugin.getClass());
			stopPlugin(loadedPlugin);
		}
		ldPlugin4EntryMap.clear();
		ldPlugin4EntryMap.putAll(tmpPlugin4EntryMap);
		checksums4ldLibsMap.clear();
		checksums4ldLibsMap.putAll(newLibChecksums);
	}
	
	/*private boolean sameURLsInClassLoaders(ClassLoader cLoader1, ClassLoader cLoader2) {
		return
	}*/
	
	private Set<URL> urlArrayToSet(URL[] urls) {
		Set<URL> retVal = new HashSet<URL>();
		for (URL url : urls) {
			retVal.add(url);
		}
		return retVal;
	}
	
	private Set<URL> getCLoaderURLSet(URLClassLoader cLoader) {
		return urlArrayToSet(cLoader.getURLs());
	}
	
	private boolean sameURLsInCLoader(ClassLoader cLoader, Set<URL> urls) {
		ClassLoader appClassLoader = this.getClass().getClassLoader();
		if (cLoader == null)
			return false;
		if (urls.isEmpty())
			if (cLoader.equals(appClassLoader))
				return true;
			else
				return false;
		if (cLoader.equals(appClassLoader))
			return false;
		if (!(cLoader instanceof URLClassLoader))
			return false;
		return urls.equals(getCLoaderURLSet((URLClassLoader)cLoader));
	}
	
	private boolean setupPlugin(ProxyPlugin plugin, PluginProperties props) {
		log.info("Setting up plugin '"+plugin+"'");
		try {
			return plugin.setup(props);
		} catch (Throwable t) {
			log.info("Throwable raised while seting up plugin '"+plugin+"' of class '"+plugin.getClass()+"'",t);
		}
		return false;
	}
	
	private boolean startPlugin(ProxyPlugin plugin) {
		log.info("Starting plugin '"+plugin+"'");
		try {
			plugin.start();
			return true;
		} catch (Throwable t) {
			log.info("Throwable raised while starting plugin '"+plugin+"' of class '"+plugin.getClass()+"'",t);
		}
		return false;
	}
	
	private boolean stopPlugin(ProxyPlugin plugin) {
		log.info("Stopping loaded plugin '"+plugin+"'");
		try {
			plugin.stop();
			return true;
		} catch (Throwable t) {
			log.info("Throwable raised while stoping plugin '"+plugin+"' of class '"+plugin.getClass()+"'",t);
		}
		return false;
	}
	
	private synchronized void loadPluginsConfigs() {
		configEntries.clear();
		//excludeFileNames.add("services.xml");
		File[] configFiles = pluginRepositoryDir.listFiles(new PluginsXMLFileFilter(excludeFileNames));
		Set<String> pluginNames = new HashSet<String>();
		for (File file : configFiles) {
			Document document = XMLFileParser.parseFile(file);
			if (document != null) {
				PluginConfigEntry cfgEntry = null;
				try {
					cfgEntry = loadPluginConfig(document);
				} catch (PluginConfigurationException e) {
					log.info("Invalid configuration file "+file.getAbsolutePath()+" ("+e.getText()+")");
					continue;
				}
				int num = 1;
				String configedName = cfgEntry.name;
				while (pluginNames.contains(cfgEntry.name)) {
					cfgEntry = new PluginConfigEntry(configedName+"#"+Integer.toString(num++), cfgEntry.className, cfgEntry.classLocation, cfgEntry.libraries, cfgEntry.types, cfgEntry.properties);
				}
				if (num > 1)
					log.info("Duplicate plugin name '"+configedName+"', name of the plugin config of which is stored in file "+file.getAbsolutePath()+" is set to '"+cfgEntry.name+"'");
				configEntries.add(cfgEntry);
				pluginNames.add(cfgEntry.name);
			} else
				log.info("Corrupted plugin configuration file "+file.getAbsolutePath());
		}
	}
	
	private PluginConfigEntry loadPluginConfig(Document doc) throws PluginConfigurationException {
		Element docRoot = doc.getDocumentElement();
		if (!ELEMENT_PLUGIN.equals(docRoot.getTagName()))
			throw new PluginConfigurationException("missing document root element '"+ELEMENT_PLUGIN+"'");
		
		NodeList nodeList = docRoot.getElementsByTagName(ELEMENT_NAME);
		if (nodeList.getLength() == 0)
			throw new PluginConfigurationException("missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_NAME+"'");
		Element pluginNameElement = (Element)nodeList.item(0);
		String pluginName = pluginNameElement.getTextContent();
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_CLASSLOC);
		String classLocation = "";
		if (nodeList.getLength() == 0)
			log.debug("Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_CLASSLOC+"' for plugin with name '"+pluginName
					+"', using default class location");
		else {
			Element classLocationElement = (Element)docRoot.getElementsByTagName(ELEMENT_CLASSLOC).item(0);
			classLocation = classLocationElement.getTextContent();
		}
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_CLASSNAME);
		if (nodeList.getLength() == 0)
			throw new PluginConfigurationException("missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_CLASSNAME+"'");
		Element classNameElement = (Element)nodeList.item(0);
		String className = classNameElement.getTextContent();
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_LIBS);
		List<String> pluginLibs = new LinkedList<String>();
		if (nodeList.getLength() == 0)
			log.debug("Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_LIBS+"' for plugin with name '"+pluginName
					+"', no additional libraries will be loaded");
		else {
			Element libsElement = (Element)nodeList.item(0);
			NodeList libs = libsElement.getElementsByTagName(ELEMENT_LIB);
			if (libs.getLength() == 0)
				log.debug("Missing elements '"+ELEMENT_LIB+"' in '"+ELEMENT_PLUGIN+"/"+ELEMENT_LIBS+"' for plugin with name '"+pluginName
						+"', no additional libraries will be loaded");
			else {
				for (int i = 0; i < libs.getLength(); i++) {
					Element type = (Element)libs.item(i);
					pluginLibs.add(type.getTextContent());
				}
			}
		}
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_TYPES);
		if (nodeList.getLength() == 0)
			throw new PluginConfigurationException("missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_TYPES+"'");
		Element typesElement = (Element)nodeList.item(0);
		NodeList types = typesElement.getElementsByTagName(ELEMENT_TYPE);
		List<String> pluginTypes = new ArrayList<String>(types.getLength());
		if (types.getLength() == 0)
			log.debug("Missing elements '"+ELEMENT_TYPE+"' in '"+ELEMENT_PLUGIN+"/"+ELEMENT_TYPES+"' for plugin with name '"+pluginName
					+"', this plugin won't be used");
		for (int i = 0; i < types.getLength(); i++) {
			Element type = (Element)types.item(i);
			pluginTypes.add(type.getTextContent());
		}
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_PARAMS);
		PluginPropertiesImpl properties = new PluginPropertiesImpl();
		if (nodeList.getLength() == 0)
			log.debug("Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_PARAMS+"' for plugin with name '"+pluginName
					+"', no parameters will be provided at plugin configuration");
		else {
			Element parametersElement = (Element)nodeList.item(0);
			NodeList params = parametersElement.getElementsByTagName(ELEMENT_PARAM);
			if (params.getLength() == 0)
				log.debug("Missing elements '"+ELEMENT_PARAM+"' in '"+ELEMENT_PLUGIN+"/"+ELEMENT_PARAMS+"' for plugin with name '"+pluginName
						+"', no parameters will be provided at plugin configuration");
			else
				for (int i = 0; i < params.getLength(); i++) {
					Element param = (Element)params.item(i);
					String nameAttr = param.getAttribute(ATTR_NAME);
					if (nameAttr != null && !nameAttr.isEmpty())
						properties.addProperty(nameAttr, param.getTextContent());
			}
		}
		return new PluginConfigEntry(pluginName,className,classLocation,pluginLibs,pluginTypes,properties);
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
			log.info("Unable to create an instance of the class '"+clazz.getName()+"'",e);
		} catch (IllegalAccessException e) {
			log.info("Instantiation with zero arguments constructor forbidden for class '"+clazz.getName()+"'",e);
		} catch (Throwable t) {
			log.info("Throwable raised while instantiation of the class '"+clazz.getName()+"'",t);
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
			log.info("Plugin '"+cfgEntry.name+"' | plugin class '"+cfgEntry.className+"' not found at '"+
					new File(pluginRepositoryDir,cfgEntry.classLocation).getAbsolutePath()+"'");
			return null;
		}
		if (!asClass.isAssignableFrom(clazz)) {
			log.info("Found class '"+clazz.getName()+"' is not a subclass of '"+
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
			log.info("Unable to get code source location for class '"+clazz.getName()+"'");
			return null;
		}
		File codeSourceFile = new File(classFileUri);
		if (codeSourceFile.isFile())
			classFile = codeSourceFile;
		else {
			String classPath = clazz.getName().replace(".", filepathSeparator);
			classFile = new File(codeSourceFile,classPath+".class");
			if (!classFile.isFile()) {
				log.info("Unable to find actual class at "+classFile.getAbsolutePath());
			}
		}
		return classFile;
	}
	
	private Class<?> loadClass(String className, ClassLoader cLoader) throws ClassNotFoundException {
		Class<?> clazz = cLoader.loadClass(className);
		try {
			File classFile = getClassFile(clazz);
			checksums4ldClassMap.put(clazz, MD5ChecksumGenerator.createHexChecksum(classFile,null));
			log.debug("File from which the class '"+clazz.getSimpleName()+"' was loaded by class loader "+clazz.getClassLoader()+" is "+classFile.toString());
			if (clazz.getClassLoader() == ClassLoader.getSystemClassLoader())
				log.debug("Watch out, class '"+clazz.getSimpleName()+"' is loaded by root class loader so proxy server won't be able to reload it on the fly if it changes");
		} catch (IOException e) {
			log.info("Error while reading class file for MD5 checksum computing");
		} 
		return clazz;
	}
	
	private  <T extends ProxyPlugin> T getPlugin(PluginConfigEntry cfgEntry, Class<T> asClass) {
		if (cfgEntry.loadError)
			return null;
		T plugin = null;
		ProxyPlugin loadedPlugin = ldPlugin4EntryMap.get(cfgEntry);
		if (loadedPlugin != null) {
			try {
				plugin = asClass.cast(loadedPlugin);
			} catch (ClassCastException e) {
				log.info("Plugin '"+cfgEntry.name+"' | plugin class '"+cfgEntry.className+"' is not a subclass of '"
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
				} else {
					cfgEntry.loadError = true;
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
