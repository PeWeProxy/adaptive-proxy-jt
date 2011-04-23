package sk.fiit.peweproxy.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.plugins.PluginHandler;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.plugins.services.RequestChunksServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseChunksServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;

public class ModulesManager {
	private static final Logger log = Logger.getLogger(ModulesManager.class);
	public static final String DEF_PATTERN_TEXTMSGS = "text/html|application/xhtml(\\+xml)?|application/x-www-form-urlencoded";
	
	private final AdaptiveEngine adaptiveEngine;
	private final List<RequestServiceModule> rqModules;
	private final List<ResponseServiceModule> rpModules;
	private final List<RequestChunksServiceModule> rqChunkModules;
	private final List<ResponseChunksServiceModule> rpChunkModules;
	private final Map<ProxyPlugin, Set<Class<? extends ProxyService>>> providedRqServices;
	private final Map<ProxyPlugin, Set<Class<? extends ProxyService>>> providedRpServices;
	private Pattern stringServicesPattern = Pattern.compile(DEF_PATTERN_TEXTMSGS);
	
	public ModulesManager(AdaptiveEngine adaptiveEngine) {
		this.adaptiveEngine = adaptiveEngine;
		rqModules = new LinkedList<RequestServiceModule>();
		rpModules = new LinkedList<ResponseServiceModule>();
		rqChunkModules = new LinkedList<RequestChunksServiceModule>();
		rpChunkModules = new LinkedList<ResponseChunksServiceModule>();
		providedRqServices = new HashMap<ProxyPlugin, Set<Class<? extends ProxyService>>>();
		providedRpServices = new HashMap<ProxyPlugin, Set<Class<? extends ProxyService>>>();
	}
	
	List<RequestServiceModule> getLoadedRequestModules() {
		return rqModules;
	}
	
	List<RequestChunksServiceModule> getLoadedRequestChunksModules() {
		return rqChunkModules;
	}
	
	List<ResponseServiceModule> getLoadedResponseModules() {
		return rpModules;
	}
	
	List<ResponseChunksServiceModule> getLoadedResponseChunksModules() {
		return rpChunkModules;
	}
	
	public void initPlugins(PluginHandler pluginHandler) {
		providedRqServices.clear();
		providedRpServices.clear();
		log.info("Loading request service modules");
		initPlugins(RequestServiceModule.class, rqModules, providedRqServices, new ProvidedServicesGetter<RequestServiceModule>() {
			@Override
			public Set<Class<? extends ProxyService>> getProvidedServices(RequestServiceModule module) {
				Set<Class<? extends ProxyService>> providedServices = new HashSet<Class<? extends ProxyService>>();
				module.getProvidedRequestServices(providedServices);
				return providedServices;
			}
		});
		log.info("Loading request chunks service modules");
		initPlugins(RequestChunksServiceModule.class, rqChunkModules, providedRqServices, new ProvidedServicesGetter<RequestChunksServiceModule>() {
			@Override
			public Set<Class<? extends ProxyService>> getProvidedServices(RequestChunksServiceModule module) {
				Set<Class<? extends ProxyService>> providedServices = new HashSet<Class<? extends ProxyService>>();
				module.getProvidedRequestChunkServices(providedServices);
				return providedServices;
			}
		});
		log.info("Loading response service modules");
		initPlugins(ResponseServiceModule.class, rpModules, providedRpServices, new ProvidedServicesGetter<ResponseServiceModule>() {
			@Override
			public Set<Class<? extends ProxyService>> getProvidedServices(ResponseServiceModule module) {
				Set<Class<? extends ProxyService>> providedServices = new HashSet<Class<? extends ProxyService>>();
				module.getProvidedResponseServices(providedServices);
				return providedServices;
			}
		});
		log.info("Loading response chunks service modules");
		initPlugins(ResponseChunksServiceModule.class, rpChunkModules, providedRpServices, new ProvidedServicesGetter<ResponseChunksServiceModule>() {
			@Override
			public Set<Class<? extends ProxyService>> getProvidedServices(ResponseChunksServiceModule module) {
				Set<Class<? extends ProxyService>> providedServices = new HashSet<Class<? extends ProxyService>>();
				module.getProvidedResponseChunkServices(providedServices);
				return providedServices;
			}
		});
	}
	
	private interface ProvidedServicesGetter<ModuleType extends ProxyPlugin> {
		Set<Class<? extends ProxyService>> getProvidedServices(ModuleType module);
	}
	
	private <ModuleType extends ProxyPlugin> void initPlugins(Class<ModuleType> modulesClass, List<ModuleType> modulesList
			, Map<ProxyPlugin, Set<Class<? extends ProxyService>>> providedServicesMap
			, ProvidedServicesGetter<ModuleType> providedSvcsGetter) {
		modulesList.clear();
		modulesList.addAll(adaptiveEngine.getPluginHandler().getPlugins(modulesClass));
		for (ModuleType module : modulesList) {
			Set<Class<? extends ProxyService>> providedServices = null;
			try {
				providedServices = providedSvcsGetter.getProvidedServices(module);
			} catch (Throwable e) {
				log.warn("Unable to obtain set of provided services from "+modulesClass.getSimpleName()+" "+module, e);
				providedServices =  Collections.emptySet();
			}
			providedServicesMap.put(module, providedServices);
		}
	}
	
	public Set<Class<? extends ProxyService>> getProvidedRequestServices(RequestServiceModule module) {
		return providedRqServices.get(module);
	}
	
	public Set<Class<? extends ProxyService>> getProvidedRequestChunksServices(RequestChunksServiceModule module) {
		return null;
	}
	
	public Set<Class<? extends ProxyService>> getProvidedResponseServices(ResponseServiceModule module) {
		return providedRpServices.get(module);
	}
	
	public Set<Class<? extends ProxyService>> getProvidedResponseChunksServices(ResponseChunksServiceModule module) {
		return null;
	}

	public AdaptiveEngine getAdaptiveEngine() {
		return adaptiveEngine;
	}

	public void setPattern(String patternString) {
		if (patternString == null)
			patternString = DEF_PATTERN_TEXTMSGS;
		try {
			stringServicesPattern = Pattern.compile(patternString);
		} catch (PatternSyntaxException e) {
			log.info("Invalid or no pattern for string content services, default one will be used", e);
			stringServicesPattern = Pattern.compile(ModulesManager.DEF_PATTERN_TEXTMSGS);
		}
	}
	
	public boolean matchesStringServicePattern(ReadableHeader header) {
		String contentType = header.getField("Content-Type");
		if (contentType == null)
			return false;
		int index = contentType.indexOf(';');
		if (index != -1)
			contentType = contentType.substring(0, index);
		contentType = contentType.trim();
		return stringServicesPattern.matcher(contentType).matches();
	}
}
