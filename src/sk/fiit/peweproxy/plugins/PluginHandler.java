package sk.fiit.peweproxy.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sk.fiit.peweproxy.utils.ChecksumUtils;
import sk.fiit.peweproxy.utils.XMLFileParser;

public class PluginHandler {
	private static final Logger log = Logger.getLogger(PluginHandler.class);
	private static String filepathSeparator = System.getProperty("file.separator");
	private static final String ELEMENT_PLUGIN = "plugin";
	private static final String ELEMENT_NAME = "name";
	private static final String ELEMENT_CLASSLOC = "classLocation";
	private static final String ELEMENT_CLASSNAME = "className";
	private static final String ELEMENT_WORKDIR = "workingDir";
	private static final String ELEMENT_LIBS = "libraries";
	private static final String ELEMENT_LIB = "lib";
	private static final String ELEMENT_TYPES = "types";
	private static final String ELEMENT_TYPE = "type";
	private static final String ELEMENT_PARAMS = "parameters";
	private static final String ELEMENT_PARAM = "param";
	private static final String ELEMENT_VARIABLES = "variables";
	private static final String ELEMENT_VARIABLE = "variable";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_VARIABLE_NAME = "name";
	private static final String VARIABLE_CONFIGURATION_FILE = "variables.xml";
	private static final int DEF_CORE_THREADS = 3;
	
	private static final FilenameFilter jarFilter;
	private static final FilenameFilter classFilter;
	private static final FilenameFilter loadableFilter;
	
	private File pluginRepositoryDir;
	private File servicesDir;
	private URL[] servicesLibsURLs;
	private File sharedLibsDir;
	private URL[] sharedLibsURLs;
	private Set<String> excludeFileNames;
	private ClassLoader servicesCLoader;
	
	private final List<PluginInstance> pluginInstances;
	private final Map<Class<?>, String> checksums4ldClassMap;
	private final Map<URL, String> checksums4ldLibsMap;
	private boolean pluginsStopped = false;
	private final StringBuffer loadingLogBuffer;
	private PluginsThreadPool threadPool;
	
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
		pluginInstances = new LinkedList<PluginInstance>();
		checksums4ldClassMap = new HashMap<Class<?>, String>();
		checksums4ldLibsMap = new HashMap<URL, String>();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (!pluginsStopped)
					stopPlugins();
			}
		});
		log.setLevel(Level.ALL);
		StringWriter outStream = new StringWriter();
		loadingLogBuffer = outStream.getBuffer();
		log.addAppender(new WriterAppender(new PatternLayout("%d{HH:mm:ss,SSS} %-5p %x - %m%n"), outStream));
	}
	
	public void setup(File pluginRepositoryDir, File servicesDir, File sharedLibsDir,
			Set<String> excludeFileNames, int coreThreads) {
		if (!pluginRepositoryDir.isDirectory()) {
			throw new IllegalArgumentException("Argument does not denote a directory");
		}
		this.sharedLibsDir = tryGetCanonicalFile(sharedLibsDir);
		this.servicesDir = tryGetCanonicalFile(servicesDir);
		this.pluginRepositoryDir = tryGetCanonicalFile(pluginRepositoryDir);
		this.excludeFileNames = excludeFileNames;
		if (coreThreads < 1) {
			coreThreads = DEF_CORE_THREADS;
		}
		threadPool = new PluginsThreadPool(coreThreads);
		log.info("Plugins home directory is set to '"+pluginRepositoryDir.getPath()+"'");
		log.info("Shared libraries directory is set to '"+sharedLibsDir.getPath()+"'");
		log.info("Services definitions directory is set to '"+servicesDir.getPath()+"'");
		log.info("Thread pool for plugins created with "+coreThreads+" core threads");
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
	
	private class PluginConfig {
		private final String name;
		private final String className;
		private final String classLocation;
		private final List<String> libraries;
		private final List<String> types;
		private final Map<String, String> properties;
		private final String workingDir;
		
		public PluginConfig(String name, String className, String classLocation, List<String> libraries,
				List<String> types, Map<String, String> properties, String workingDir) {
			this.name = name;
			this.className = className;
			this.classLocation = classLocation;
			this.libraries = libraries;
			this.types = types;
			this.properties = properties;
			this.workingDir = workingDir;
		}
	}

	public class PluginInstance {
		final PluginConfig plgConfig;
		final PluginPropertiesImpl properties;
		Set<Class<? extends ProxyPlugin>> realTypes = new LinkedHashSet<Class<? extends ProxyPlugin>>();
		final Set<URL> libsrariesURLSet = new HashSet<URL>();
		ClassLoader classLoader = null;
		ProxyPlugin instance = null;
		
		public PluginInstance(PluginConfig plgConfig, Map<String, String> variables) throws PluginConfigurationException {
			this.plgConfig = plgConfig;
			File workDir = null;
			if (plgConfig.workingDir == null) {
				workDir = pluginRepositoryDir;
			} else{
				workDir = tryGetCanonicalFile(new File(pluginRepositoryDir, plgConfig.workingDir));
				if (!workDir.exists()) {
					log.info("Plugin '"+plgConfig.name+"' - Creating working directory '"+workDir.getPath()+"'");
					if (!workDir.mkdir()) {
						log.info("Plugin '"+plgConfig.name+"' - Unable to create working directory '"+workDir.getPath()+"', no working directory will be used");
						throw new PluginConfigurationException("Plugin '"+plgConfig.name+"' - Unable to create missing working directory");
					}
				} else if (!workDir.isDirectory())
					throw new PluginConfigurationException("Plugin '"+plgConfig.name+"' - Working directory path '"+workDir.getPath()+"' is pointing to regular"+
							" file, not a directory");
				String path = workDir.getAbsolutePath();
				try {
					path = workDir.getCanonicalPath();
				} catch (IOException e) {
					log.warn("Plugin '"+plgConfig.name+"' - Unable to get cannonical path for file "+workDir,e);
				}
				log.info("Plugin '"+plgConfig.name+"' - plugin will use working directory '"+path+"'");
				// TODO nastavit prava na read/write cez policy
			}
			
			properties = new PluginPropertiesImpl(workDir,threadPool);
			for (Entry<String, String> property : plgConfig.properties.entrySet()) {
				properties.addProperty(property.getKey(),replaceVariables(property.getValue(), variables));
			}
		}
		
		public String getName() {
			return plgConfig.name;
		}
		
		public Class<? extends ProxyPlugin> getPluginClass() {
			if (instance == null) 
				throw new IllegalStateException("Plugin not instantiated");
			return instance.getClass();
		}
		
		public Set<Class<? extends ProxyPlugin>> getTypes() {
			return Collections.unmodifiableSet(realTypes);
			/*List<String> retVal = new LinkedList<String>();
			for (Class<? extends ProxyPlugin> typeClass : realTypes) {
				retVal.add(typeClass.getSimpleName());
			}
			return retVal;*/
		}
		
		public ProxyPlugin getInstance() {
			return instance;
		}
		
		@Override
		public String toString() {
			return "'"+getName()+"'("+((instance != null) ? instance.toString() : "not instantiated yet")+")";
		}
	}
	
	private class CLoadersChains {
		final ClassLoader classpathCLoader;
		final Map<Set<URL>, ClassLoader> libsCLoaders;
		
		public CLoadersChains(ClassLoader classpathCLoader) {
			this.classpathCLoader = classpathCLoader;
			libsCLoaders = new HashMap<Set<URL>, ClassLoader>();
		}
	}
	
	private File tryGetCanonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		} catch (IOException e) {
			log.info("Error when converting file '"+file.getAbsolutePath()+"' to cannonical form");
		}
		return file.getAbsoluteFile();
	}
	
	private void createClassLoaders(List<PluginInstance> plgInstances, Map<URL, String> newLibChecksums) {
		Map<URL, CLoadersChains> cLoaders = new HashMap<URL, CLoadersChains>();
		if (servicesLibsURLs != null) {
			servicesCLoader = URLClassLoader.newInstance(servicesLibsURLs);
			log.debug("Creating new services definitions ClassLoader "+servicesCLoader+" with URLs set to "+Arrays.toString(servicesLibsURLs)
					+" with parent ClassLoader set to "+servicesCLoader.getParent());
		} else {
			servicesCLoader = null;
		}
		for (Iterator<PluginInstance> iterator = plgInstances.iterator(); iterator.hasNext();) {
			PluginInstance plgInstance =  iterator.next();
			// Construct and validate classpath URL
			File pluginFile = tryGetCanonicalFile(new File(pluginRepositoryDir,plgInstance.plgConfig.classLocation)); 
			if (!pluginFile.canRead()) {
				log.info("Can not read classpath file/directory '"+pluginFile.getPath()+"',  Plugin '"
						+plgInstance.plgConfig.name+"' will not be loaded");
				iterator.remove();
				continue;
			}
			URI classLocURI = pluginFile.toURI();
			URL classLocURL = null;
			try {
				classLocURL = classLocURI.toURL();
			} catch (MalformedURLException e) {
				log.info("Error when converting valid classpath file/directory path '"+pluginFile.getAbsolutePath()+"' to URL, plugin '"
						+plgInstance.plgConfig.name+" will not be loaded");
				iterator.remove();
				continue;
			}
			// Construct and validate libraries URLs
			for (String libLocation : plgInstance.plgConfig.libraries) {
				File libFile = tryGetCanonicalFile(new File(pluginRepositoryDir,libLocation));
				if (!libFile.canRead()) {
					// unable to locate library file (jar/dir)
					log.info("Library location '"+libLocation+"' for the plugin '"+plgInstance.plgConfig.name+
						"' does not point to valid jar/directory, this plugin will be loaded, but bad things might happen " +
						" if you are running in production. You can safely ignore this message in development mode.");
				}
				URI libLocURI = libFile.toURI();
				URL url = null;
				try {
					url = libLocURI.toURL();
				} catch (MalformedURLException e) {
					log.info("Error when converting valid library file path '"+libFile.getAbsolutePath()+"' to URL (plugin '" +
							plgInstance.plgConfig.name+"). Library will be considered changed when reloading plugins next time.");
				}
				try {
					newLibChecksums.put(url, ChecksumUtils.createHexChecksum(libFile,classFilter));
				} catch (IOException e) {
					log.info("Error when converting library file '"+libFile.getAbsolutePath()+"' for MD5 checksum computing");
				}
				plgInstance.libsrariesURLSet.add(url);
			}
			// Try to find created class loader to share
			CLoadersChains existingChains = cLoaders.get(classLocURL);
			if (existingChains == null) {
				existingChains = new CLoadersChains(createClassLoader(new URL[] {classLocURL}, servicesCLoader, plgInstance.getName()));
				cLoaders.put(classLocURL, existingChains);
			}
			
			ClassLoader tmpFinalCLoader = null;
			if (plgInstance.libsrariesURLSet.isEmpty()) {
				tmpFinalCLoader = existingChains.libsCLoaders.get(plgInstance.libsrariesURLSet);
				if (tmpFinalCLoader == null) {
					if (sharedLibsURLs != null)
						// MAIN <- SVCS <- PLG <- SHD
						tmpFinalCLoader = createClassLoader(sharedLibsURLs, existingChains.classpathCLoader, plgInstance.getName());
					else
						// MAIN <- SVCS <- PLG
						tmpFinalCLoader = existingChains.classpathCLoader;
					existingChains.libsCLoaders.put(plgInstance.libsrariesURLSet, tmpFinalCLoader);
				} else
					log.debug("Plugin '"+plgInstance.getName()+"' shares already created ClassLoader "+tmpFinalCLoader);
			} else {
				tmpFinalCLoader = existingChains.libsCLoaders.get(plgInstance.libsrariesURLSet);
				if (tmpFinalCLoader == null) {
					// MAIN <- SVCS <- PLG <- LIBS
					URL[] libsURLS = plgInstance.libsrariesURLSet.toArray(new URL[plgInstance.libsrariesURLSet.size()]);
					tmpFinalCLoader = createClassLoader(libsURLS, existingChains.classpathCLoader, plgInstance.getName());
					if (sharedLibsURLs != null)
						// MAIN <- SVCS <- PLG <- LIBS <- SHD
						tmpFinalCLoader = createClassLoader(sharedLibsURLs, tmpFinalCLoader, plgInstance.getName());
					existingChains.libsCLoaders.put(plgInstance.libsrariesURLSet, tmpFinalCLoader);
				} else
					log.debug("Plugin '"+plgInstance.getName()+"' shares already created ClassLoader "+tmpFinalCLoader);
			}
			plgInstance.classLoader = tmpFinalCLoader;
			if (log.isDebugEnabled()) {
				String hierarchy = "";
				for (ClassLoader cLoader = plgInstance.classLoader; cLoader.getParent() != null; cLoader = cLoader.getParent()) {
					hierarchy = cLoader+" <- "+hierarchy;
				}
				log.debug("Plugin's '"+plgInstance.plgConfig.name+"' classloader hierarchy: "+hierarchy);
			}
		}
	}
	
	private URLClassLoader createClassLoader(URL[] urls, ClassLoader parent, String pluginName) {
		URLClassLoader retVal = null;
		if (parent != null)
			retVal = URLClassLoader.newInstance(urls,parent);
		else
			retVal = URLClassLoader.newInstance(urls);
		log.debug("Creating new ClassLoader "+retVal+" with URLs set to "+Arrays.toString(retVal.getURLs())
				+" with parent ClassLoader set to "+retVal.getParent()+ " for potential use by plugin '"
				+pluginName+"'");
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
	
	private URL[] createLibsURLs(File dir, Map<URL, String> newLibChecksums, String logDirDescr, String logEntryDescr) {
		if (dir != null && dir.isDirectory() && dir.canRead()) {
			URL sharedDirURL = null;
			try {
				sharedDirURL = tryGetCanonicalFile(dir).toURI().toURL();
			} catch (MalformedURLException e) {
				log.warn("Error when converting valid "+logDirDescr+" directory path '"+dir.getAbsolutePath()
						+"' to URL, no "+logDirDescr+" will be used");
			}
			if (sharedDirURL != null) {
				File[] jarFiles = getNestedFiles(dir, jarFilter);
				URL[] urls = new URL[jarFiles.length+1];
				urls[0] = sharedDirURL;
				log.debug("Using "+logEntryDescr+" resource '"+sharedDirURL+"'");
				int i = 1;
				for (File lib : jarFiles) {
					try {
						lib = lib.getCanonicalFile();
					} catch (IOException e1) {
						log.info("Error when converting "+logEntryDescr+" file '"+lib.getAbsolutePath()+"' to cannonical form");
						return null;
					}
					try {
						urls[i++] = lib.toURI().toURL();
						log.debug("Using "+logEntryDescr+" resource '"+urls[i-1]+"'");
					} catch (MalformedURLException e) {
						log.warn("Error when converting valid "+logEntryDescr+" file path '"+lib.getAbsolutePath()
								+"' to URL, no shared libraries will be used");
					}
				}
				try {
					newLibChecksums.put(sharedDirURL, ChecksumUtils.createHexChecksum(dir,loadableFilter));
				} catch (IOException e) {
					log.info("Error when reading "+logDirDescr+" direcotry '"+dir.getAbsolutePath()+"' for MD5 checksum computing");
				}
				return urls;
			}
		} else
			log.info("Can not access "+logDirDescr+" directory '"+dir.getAbsolutePath()+"', no "+logDirDescr+" will be used");
		return null;
	}
	
	private void createServicesLibsURLs(Map<URL, String> newLibChecksums) {
		if (servicesDir != null) {
			servicesLibsURLs = createLibsURLs(servicesDir, newLibChecksums, "services definitions", "service definition");
		} else
			log.info("No services definitions directory set, no services will be available to processing plugins");
	}
	
	private void createSharedLibsURLs(Map<URL, String> newLibChecksums) {
		if (sharedLibsDir != null) {
			sharedLibsURLs = createLibsURLs(sharedLibsDir, newLibChecksums, "shared libraries", "shared library");
		} else
			log.info("Configured not to use shared libraries directory");
	}
	
	private boolean checkLibsChange(File libsDir, Map<URL, String> newLibChecksums, String libsText) {
		boolean retVal = false;
		if (libsDir != null) {
			URL dirURL = null;
			try {
				dirURL = libsDir.toURI().toURL();
			} catch (MalformedURLException e) {
				log.warn("Error when converting valid "+libsText.toLowerCase()+" directory path '"+libsDir.getAbsolutePath()
						+"' to URL, no "+libsText.toLowerCase()+" will be available to processing plugins");
			}
			if (dirURL != null) { 
				String newChecksum = newLibChecksums.get(dirURL);
				if (newChecksum != null)
					retVal = newChecksum.equals(checksums4ldLibsMap.get(dirURL));
			}
			if (retVal)
				log.info(libsText+" directory has not been changed");
			else
				log.info(libsText+" directory has been changed so all plugins will be reloaded");
		} else
			retVal = true;
		return retVal;
	}
	
	public synchronized void reloadPlugins() {
		if (pluginRepositoryDir == null) {
			log.info("Plugins home repository not set");
			return;
		}
		//loadingLogBuffer.setLength(0);	//keep previous logs
		List<PluginConfig> plgConfigs = loadPluginsConfigs();
		Map<String, String> variables = loadVariablesConfiguration();
		List<PluginInstance> newPlgInstances = createPluginInstances(plgConfigs, variables);
		Map<URL, String> newLibChecksums = new HashMap<URL, String>();
		createServicesLibsURLs(newLibChecksums);
		createSharedLibsURLs(newLibChecksums);
		createClassLoaders(newPlgInstances,newLibChecksums);
		boolean servicesLibsDirSame = checkLibsChange(servicesDir, newLibChecksums, "Services definitions");;
		boolean sharedLibsDirSame = checkLibsChange(sharedLibsDir, newLibChecksums, "Shared libraries");;
		for (PluginInstance oldplgInstance : pluginInstances) {
			ProxyPlugin loadedPlugin = oldplgInstance.instance;
			PluginInstance newPlgInstance = null;
			if (servicesLibsDirSame) {
				if (sharedLibsDirSame) {
					// try to find matching newPlgInstance
					for (PluginInstance plgInstance : newPlgInstances) {
						if (plgInstance.plgConfig.className.equals(oldplgInstance.plgConfig.className) &&
								plgInstance.plgConfig.classLocation.equals(oldplgInstance.plgConfig.classLocation)) {
							newPlgInstance = plgInstance;
							break;
						}
					};
				} else
					log.debug("Shared libraries directory has been changed so plugin '"+loadedPlugin+"' will be reloaded");
			} else
				log.debug("Services definitions directory has been changed so plugin '"+loadedPlugin+"' will be reloaded");
			if (newPlgInstance != null && oldplgInstance.libsrariesURLSet.equals(newPlgInstance.libsrariesURLSet)) {
				boolean libsChanged = false;
				for (URL libURL : newPlgInstance.libsrariesURLSet) {
					String newChecksum = newLibChecksums.get(libURL);
					if (!newChecksum.equals(checksums4ldLibsMap.get(libURL))) {
						libsChanged = true;
						break;
					}
				}
				if (!libsChanged) {
					boolean supportsReconfigure = false;
					try {
						supportsReconfigure = loadedPlugin.supportsReconfigure(newPlgInstance.properties);
					} catch (Throwable t) {
						log.info("Throwable raised while calling supportsReconfigure() on '"+loadedPlugin+"' of class '"+loadedPlugin.getClass()+"'",t);
					}
					if (supportsReconfigure) {
						log.debug("Loaded plugin '"+loadedPlugin+"' supports reconfiguring with new properties at it's current state");
						Class<?> newClazz = null;
						try {
							newClazz = loadClass(newPlgInstance.plgConfig.className, newPlgInstance.classLoader);
						} catch (ClassNotFoundException e) {
							log.info("Plugin '"+newPlgInstance.plgConfig.name+"' | plugin class '"+newPlgInstance.plgConfig.className+"' not found at '"+
									tryGetCanonicalFile(new File(pluginRepositoryDir,newPlgInstance.plgConfig.classLocation)).getPath()+"'", e);
						}
						Class<?> oldClass = loadedPlugin.getClass();
						String newClassChecksum = null;
						if (newClazz != null) {
							newClassChecksum = checksums4ldClassMap.get(newClazz);
							if (oldClass == newClazz || checksums4ldClassMap.get(oldClass).equals(newClassChecksum)) {
								log.debug("Seems like class '"+newClazz.getName()+"' hasn't changed, so we try to keep already loaded plugin '"+loadedPlugin+"'");
								if (startPlugin(loadedPlugin, newPlgInstance.properties)) {
									if (newClazz != oldClass)
										checksums4ldClassMap.remove(newClazz);
									newPlgInstance.instance = loadedPlugin;
									newPlgInstance.classLoader = oldClass.getClassLoader();
									newPlgInstance.realTypes = oldplgInstance.realTypes;
									log.debug("Loaded plugin '"+loadedPlugin+"' preserved, no classes (re)loading will ocur");
									continue;
								} else
									log.debug("Plugin of class '"+newPlgInstance.plgConfig.className+"' is not reconfigured properly, it will be stoped and thrown away");
							} else {
								log.debug("Seems like class '"+newClazz.getName()+"' has changed, thus plugin '"+loadedPlugin+"' will be reloaded");
							}
						}
					} else
						log.debug("Loaded plugin '"+loadedPlugin+"' does not support reconfiguring with new properties at it's current state, plugin will be reloaded");
				} else {
					log.debug("Dependencies of plugin '"+loadedPlugin+"' changed, plugin will be reloaded");
				}
			} else {
				if (newPlgInstance == null)
					log.debug("Loaded plugin '"+loadedPlugin+"' not to be integrated, plugin will be stopped");
				else
					log.debug("Changed configuration of plugin '"+loadedPlugin+"', plugin will be reloaded");
			}
			checksums4ldClassMap.remove(loadedPlugin.getClass());
			stopPlugin(loadedPlugin);
		}
		checksums4ldLibsMap.clear();
		checksums4ldLibsMap.putAll(newLibChecksums);
		pluginInstances.clear();
		pluginInstances.addAll(newPlgInstances);
		
		// now load / start plugins
		for (PluginInstance pluginInstance : pluginInstances) {
			if (pluginInstance.instance == null) {
				getPlugin(pluginInstance);
			}
		}
	}
	
	private List<PluginInstance> createPluginInstances(List<PluginConfig> plgConfigs, Map<String, String> variables) {
		List<PluginInstance> retVal = new LinkedList<PluginHandler.PluginInstance>();
		for (PluginConfig plgConfig : plgConfigs) {
			try {
				retVal.add(new PluginInstance(plgConfig, variables));
			} catch (PluginConfigurationException e) {
				log.info("Invalid configuration of plugin '"+plgConfig.name+"': "+e.getText());
			}
		}
		return retVal;
	}
	
	/*private Set<URL> getCLoaderURLSet(URLClassLoader cLoader) {
		URL[] urls = cLoader.getURLs();
		Set<URL> retVal = new HashSet<URL>();
		for (URL url : urls) {
			retVal.add(url);
		}
		return retVal;
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
	}*/
	
	private boolean startPlugin(ProxyPlugin plugin, PluginProperties props) {
		log.info("Starting plugin '"+plugin+"'");
		try {
			return plugin.start(props);
		} catch (Throwable t) {
			log.info("Throwable raised while seting up and starting plugin '"+plugin+"' of class '"+plugin.getClass()+"'",t);
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
	
	/**
	 * @author Tomas Kramar
	 */
	private Map<String, String> loadVariablesConfiguration() {
		Map<String, String> variables = new HashMap<String, String>();
		
		File variablesFile = tryGetCanonicalFile(new File(pluginRepositoryDir.getAbsolutePath() + File.separator + VARIABLE_CONFIGURATION_FILE));
		if (!variablesFile.canRead()) {
			log.info("Can not read variables file '"+variablesFile.getPath()+"', no variables will be loaded");
			return variables;
		}
		Document document = XMLFileParser.parseFile(variablesFile);
		
		if(document != null) {
			Element docRoot = document.getDocumentElement();
			if(ELEMENT_VARIABLES.equals(docRoot.getTagName())) {
				NodeList nodeList = docRoot.getElementsByTagName(ELEMENT_VARIABLE);
				for(int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					
					NamedNodeMap attributes = node.getAttributes();
					if(attributes != null) {
						Node nameNode = attributes.getNamedItem(ATTR_VARIABLE_NAME);
						if(nameNode != null) {
							variables.put(nameNode.getTextContent(), node.getTextContent());
						} else {
							log.error("The variable configuration file '"+variablesFile.getAbsolutePath()+"' is malformed - expected '"
									+ATTR_VARIABLE_NAME+"' attribute on tag "+node.getTextContent());
						}
					} else {
						log.error("The variable configuration file '"+variablesFile.getAbsolutePath()+"' is malformed - '"
								+node.getTextContent()+"' has no attributes");
					}
				}
			} else {
				log.error("The variable configuration file '"+variablesFile.getAbsolutePath()+"' is missing the document root element '"
						+ELEMENT_VARIABLES+"'");
			}
		} else {
			log.error("Corrupted variables file '"+variablesFile.getAbsolutePath()+"'");
		}
		
		return variables;
	}
	
	/**
	 * @author Tomas Kramar
	 */
	private String replaceVariables(String textWithVariables, Map<String, String> variableValues) {
		Pattern pattern = Pattern.compile("\\$\\{.*?\\}");
		Matcher matcher = pattern.matcher(textWithVariables);
		StringBuffer replacedText = new StringBuffer();
		while(matcher.find()) {
			String variableDefinition = matcher.group();
			String variable = variableDefinition.substring(2, variableDefinition.length() - 1);
			if(variableValues.containsKey(variable)) {
				matcher.appendReplacement(replacedText, variableValues.get(variable));
			} else {
				String definedVariablesList = "";
				for(String definedVariable : variableValues.keySet()) {
					definedVariablesList += definedVariable + ", ";
				}
				if(definedVariablesList.length() > 0) {
					definedVariablesList = definedVariablesList.substring(0, definedVariablesList.length() - 2);
				}
				log.error("Undefined variable '" + variable + "'. Defined variables are: '" + definedVariablesList + "'");
			}
		}
		
		matcher.appendTail(replacedText);
		
		return replacedText.toString();
	}
	
	private synchronized List<PluginConfig> loadPluginsConfigs() {
		List<PluginConfig> configs = new LinkedList<PluginHandler.PluginConfig>();
		File[] configFiles = pluginRepositoryDir.listFiles(new PluginsXMLFileFilter(excludeFileNames));
		Set<String> pluginNames = new HashSet<String>();
		for (File file : configFiles) {
			Document document = XMLFileParser.parseFile(file);
			if (document != null) {
				PluginConfig plgConfig = null;
				try {
					plgConfig = loadPluginConfig(document,file.getName());
				} catch (PluginConfigurationException e) {
					log.info("Invalid configuration file '"+file.getAbsolutePath()+"' ("+e.getText()+")");
					continue;
				}
				int num = 1;
				String configedName = plgConfig.name;
				while (pluginNames.contains(plgConfig.name)) {
					plgConfig = new PluginConfig(configedName+"#"+Integer.toString(num++), plgConfig.className, plgConfig.classLocation, plgConfig.libraries
							,plgConfig.types,plgConfig.properties, plgConfig.workingDir);
				}
				if (num > 1)
					log.info("Duplicate plugin name '"+configedName+"', name of the plugin config of which is stored in file '"+file.getAbsolutePath()
							+"' is set to '"+plgConfig.name+"'");
				configs.add(plgConfig);
				pluginNames.add(plgConfig.name);
			} else
				log.info("Corrupted plugin configuration file '"+file.getAbsolutePath()+"'");
		}
		return configs;
	}
	
	private PluginConfig loadPluginConfig(Document doc, String xmlName) throws PluginConfigurationException {
		Element docRoot = doc.getDocumentElement();
		if (!ELEMENT_PLUGIN.equals(docRoot.getTagName()))
			throw new PluginConfigurationException("Configuration '"+xmlName+"' - Missing document root element '"+ELEMENT_PLUGIN+"'");
		
		NodeList nodeList = docRoot.getElementsByTagName(ELEMENT_NAME);
		if (nodeList.getLength() == 0)
			throw new PluginConfigurationException("Configuration '"+xmlName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_NAME+"'");
		Element pluginNameElement = (Element)nodeList.item(0);
		String pluginName = pluginNameElement.getTextContent();
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_CLASSLOC);
		String classLocation = "";
		if (nodeList.getLength() == 0)
			log.debug("Plugin '"+pluginName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_CLASSLOC+"', using default class location");
		else {
			Element classLocationElement = (Element)docRoot.getElementsByTagName(ELEMENT_CLASSLOC).item(0);
			classLocation = classLocationElement.getTextContent();
		}
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_CLASSNAME);
		if (nodeList.getLength() == 0)
			throw new PluginConfigurationException("Plugin '"+pluginName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_CLASSNAME+"'");
		Element classNameElement = (Element)nodeList.item(0);
		String className = classNameElement.getTextContent();
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_WORKDIR);
		String workDir = null;
		if (nodeList.getLength() == 0) {
			log.debug("Plugin '"+pluginName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_WORKDIR+"', plugins directory will be used as working directory");
		} else {
			Element workDirElement = (Element)nodeList.item(0);
			workDir = workDirElement.getTextContent();
		}
		nodeList = docRoot.getElementsByTagName(ELEMENT_LIBS);
		List<String> pluginLibs = new LinkedList<String>();
		if (nodeList.getLength() == 0)
			log.debug("Plugin '"+pluginName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_LIBS+"', no additional libraries will be loaded");
		else {
			Element libsElement = (Element)nodeList.item(0);
			NodeList libs = libsElement.getElementsByTagName(ELEMENT_LIB);
			if (libs.getLength() == 0)
				log.debug("Plugin '"+pluginName+"' - Missing elements '"+ELEMENT_LIB+"' in '"+ELEMENT_PLUGIN+"/"+ELEMENT_LIBS+"', no additional libraries will be loaded");
			else {
				for (int i = 0; i < libs.getLength(); i++) {
					Element type = (Element)libs.item(i);
					pluginLibs.add(type.getTextContent());
				}
			}
		}
		nodeList = docRoot.getElementsByTagName(ELEMENT_TYPES);
		if (nodeList.getLength() == 0)
			throw new PluginConfigurationException("Plugin '"+pluginName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_TYPES+"'");
		Element typesElement = (Element)nodeList.item(0);
		NodeList types = typesElement.getElementsByTagName(ELEMENT_TYPE);
		List<String> pluginTypes = new ArrayList<String>(types.getLength());
		if (types.getLength() == 0)
			log.debug("Plugin '"+pluginName+"' - Missing elements '"+ELEMENT_TYPE+"' in '"+ELEMENT_PLUGIN+"/"+ELEMENT_TYPES+"', this plugin won't be used");
		for (int i = 0; i < types.getLength(); i++) {
			Element type = (Element)types.item(i);
			pluginTypes.add(type.getTextContent());
		}
		
		nodeList = docRoot.getElementsByTagName(ELEMENT_PARAMS);
		Map<String, String> properties = new  HashMap<String, String>();
		if (nodeList.getLength() == 0)
			log.debug("Plugin '"+pluginName+"' - Missing element '"+ELEMENT_PLUGIN+"/"+ELEMENT_PARAMS+"', no parameters will be provided at plugin configuration");
		else {
			Element parametersElement = (Element)nodeList.item(0);
			NodeList params = parametersElement.getElementsByTagName(ELEMENT_PARAM);
			if (params.getLength() == 0)
				log.debug("Plugin '"+pluginName+"' - Missing elements '"+ELEMENT_PARAM+"' in '"+ELEMENT_PLUGIN+"/"+ELEMENT_PARAMS+"', no parameters will be provided at" +
						" plugin configuration");
			else
				for (int i = 0; i < params.getLength(); i++) {
					Element param = (Element)params.item(i);
					String nameAttr = param.getAttribute(ATTR_NAME);
					if (nameAttr != null && !nameAttr.isEmpty())
						properties.put(nameAttr, param.getTextContent());
			}
		}
		return new PluginConfig(pluginName,className,classLocation,pluginLibs,pluginTypes,properties,workDir);
	}
	
	private ProxyPlugin getInstance(Class<? extends ProxyPlugin> clazz) {
		ProxyPlugin instance = null;
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
	
	private Class<? extends ProxyPlugin> getClass(PluginInstance plgInstance) {
		Class<?> clazz = null;
		try {
			clazz = loadClass(plgInstance.plgConfig.className, plgInstance.classLoader);
		} catch (ClassNotFoundException e) {
			log.info("Plugin '"+plgInstance.plgConfig.name+"' | plugin class '"+plgInstance.plgConfig.className+"' not found at '"+
					tryGetCanonicalFile(new File(pluginRepositoryDir,plgInstance.plgConfig.classLocation)).getPath()+"'");
			return null;
		}
		if (!ProxyPlugin.class.isAssignableFrom(clazz)) {
			log.info("Found class '"+clazz.getName()+"' is not a subclass of '"+
					ProxyPlugin.class.getName()+"' interface");
			return null;
		}
		return clazz.asSubclass(ProxyPlugin.class);
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
		File codeSourceFile = tryGetCanonicalFile(new File(classFileUri));
		if (codeSourceFile.isFile())
			classFile = codeSourceFile;
		else {
			String classPath = clazz.getName().replace(".", filepathSeparator);
			classFile = tryGetCanonicalFile(new File(codeSourceFile,classPath+".class"));
			if (!classFile.isFile()) {
				log.info("Unable to find actual class at '"+classFile.getPath()+"'");
			}
		}
		return classFile;
	}
	
	private Class<?> loadClass(String className, ClassLoader cLoader) throws ClassNotFoundException {
		Class<?> clazz = cLoader.loadClass(className);
		try {
			File classFile = getClassFile(clazz);
			checksums4ldClassMap.put(clazz, ChecksumUtils.createHexChecksum(classFile,null));
			log.debug("File from which the class '"+clazz.getSimpleName()+"' was loaded by class loader "+clazz.getClassLoader()+" is "+classFile.getPath());
			if (clazz.getClassLoader() == ClassLoader.getSystemClassLoader())
				log.warn("Watch out, class '"+clazz.getSimpleName()+"' is loaded by root class loader so only classes accessible from classpath will be visible" +
						" to the plugin, " + "and the proxy server won't be able to reload it on the fly if it changes");
		} catch (IOException e) {
			log.info("Error while reading class file for MD5 checksum computing");
		} 
		return clazz;
	}
	
	private ProxyPlugin getPlugin(PluginInstance plgInstance) {
		ProxyPlugin plugin = null;
		Class<? extends ProxyPlugin> clazz = getClass(plgInstance);
		if (clazz != null) {
			plugin = getInstance(clazz);
			if (plugin != null) {
				if (startPlugin(plugin,plgInstance.properties)) {
					plgInstance.instance = plugin;
				} else {
					log.debug("Plugin of class '"+plgInstance.plgConfig.className+"' is not set up properly, it is thrown away");
					plugin = null;
				}
			}
		}
		return plugin;
	}
	
	public static class AbcPluginsComparator implements Comparator<PluginInstance> {
		@Override
		public int compare(PluginInstance o1, PluginInstance o2) {
			return o1.plgConfig.name.compareTo(o2.plgConfig.name);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ProxyPlugin> List<T> getPlugins(Class<T> asClass) {
		List<PluginInstance> recognizedPlugins = new LinkedList<PluginInstance>();
		for (PluginInstance plgInstance : pluginInstances) {
			if (!plgInstance.realTypes.contains(asClass))  {
				boolean dynamicTypes = plgInstance.plgConfig.types.isEmpty();
				if (dynamicTypes || plgInstance.plgConfig.types.contains(asClass.getSimpleName())) {
					try {
						asClass.cast(plgInstance.instance);
						if (dynamicTypes)
							log.debug("Dynamic plugin type discovery: plugin '"+plgInstance.plgConfig.name+"' is a "+asClass.getSimpleName());
						plgInstance.realTypes.add(asClass);
						recognizedPlugins.add(plgInstance);
					} catch (ClassCastException e) {
						if (dynamicTypes)
							log.debug("Dynamic plugin type discovery: plugin '"+plgInstance.plgConfig.name+"' is not a "+asClass.getSimpleName());
						else
							log.info("Plugin '"+plgInstance.plgConfig.name+"' | plugin class '"+plgInstance.plgConfig.className+"' is not a subclass of '"
									+asClass.getName()+"' class/interface. Fix plugin's configuration !");
					}
				}
			} else
				recognizedPlugins.add(plgInstance);
		}
		log.info("Recognized plugins of type "+asClass.getSimpleName()+": "+recognizedPlugins.toString());
		List<T> retVal = new LinkedList<T>();
		for (PluginInstance plgInstance : recognizedPlugins) {
			retVal.add((T) plgInstance.instance);
		}
		return Collections.unmodifiableList(retVal);
	}
	
	public List<PluginInstance> getAllPlugins() {
		return Collections.unmodifiableList(pluginInstances);
	}
	
	public void stopPlugins() {
		for (PluginInstance plgInstance : pluginInstances) {
			stopPlugin(plgInstance.instance);
		}
		pluginsStopped = true;
	}
	
	public ClassLoader getServicesCLoader() {
		if (servicesCLoader == null)
			return getClass().getClassLoader();
		return servicesCLoader;
	}
	
	public String getLoadingLogText() {
		return loadingLogBuffer.toString();
	}
	
	public void submitTaskToThreadPool(Runnable task) {
		threadPool.execute(task);
	}
}