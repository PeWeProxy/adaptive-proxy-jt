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
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ServiceModule;

public class ModulesManager {
	private static final Logger log = Logger.getLogger(ModulesManager.class);
	public static final String DEF_PATTERN_TEXTMSGS = "text/html|application/xhtml(\\+xml)?|application/x-www-form-urlencoded";
	
	private final AdaptiveEngine adaptiveEngine;
	private final List<RequestServiceModule> rqModules;
	private final List<ResponseServiceModule> rpModules;
	private final Map<RequestServiceModule, Set<Class<? extends ProxyService>>> providedRqServices;
	private final Map<ResponseServiceModule, Set<Class<? extends ProxyService>>> providedRpServices;
	private Pattern stringServicesPattern = Pattern.compile(DEF_PATTERN_TEXTMSGS);
	
	public ModulesManager(AdaptiveEngine adaptiveEngine) {
		this.adaptiveEngine = adaptiveEngine;
		rqModules = new LinkedList<RequestServiceModule>();
		rpModules = new LinkedList<ResponseServiceModule>();
		providedRqServices = new HashMap<RequestServiceModule, Set<Class<? extends ProxyService>>>();
		providedRpServices = new HashMap<ResponseServiceModule, Set<Class<? extends ProxyService>>>();
	}
	
	public List<RequestServiceModule> getRequestModules() {
		return rqModules;
	}
	
	public List<ResponseServiceModule> getResponseModules() {
		return rpModules;
	}
	
	public void initPlugins(PluginHandler pluginHandler) {
		log.info("Loading request service modules");
		initPlugins(RequestServiceModule.class, rqModules, providedRqServices, new ProvidedServicesGetter<RequestServiceModule>() {
			@Override
			public Set<Class<? extends ProxyService>> getProvidedServices(RequestServiceModule module) {
				Set<Class<? extends ProxyService>> providedServices = new HashSet<Class<? extends ProxyService>>();
				module.getProvidedRequestServices(providedServices);
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
	}
	
	private interface ProvidedServicesGetter<ModuleType extends ServiceModule> {
		Set<Class<? extends ProxyService>> getProvidedServices(ModuleType module);
	}
	
	private <ModuleType extends ServiceModule> void initPlugins(Class<ModuleType> modulesClass, List<ModuleType> modulesList
			, Map<ModuleType, Set<Class<? extends ProxyService>>> providedServicesMap
			, ProvidedServicesGetter<ModuleType> providedSvcsGetter) {
		modulesList.clear();
		providedServicesMap.clear();
		modulesList.addAll(adaptiveEngine.getPluginHandler().getPlugins(modulesClass));
		/*Map<ServiceModule, Set<Class<? extends ProxyService>>> dependencies
			= new HashMap<ServiceModule, Set<Class<? extends ProxyService>>>();*/
		for (ModuleType module : modulesList) {
			try {
				/*Set<Class<? extends ProxyService>> pluginDependencies = module.getDependencies();
				if (pluginDependencies == null)
					pluginDependencies = Collections.emptySet();
				dependencies.put(module, pluginDependencies);*/
				Set<Class<? extends ProxyService>> providedServices = providedSvcsGetter.getProvidedServices(module);
				if (providedServices == null)
					providedServices = Collections.emptySet();
				providedServicesMap.put(module, providedServices);
			} catch (Throwable e) {
				// TODO: handle exception
			}
		}
	}
	
	public List<RequestServiceModule> getLoadedRequestModules() {
		List<RequestServiceModule> retVal = new LinkedList<RequestServiceModule>();
		retVal.addAll(rqModules);
		return retVal;
	}
	
	public List<ResponseServiceModule> getLoadedResponseModules() {
		List<ResponseServiceModule> retVal = new LinkedList<ResponseServiceModule>();
		retVal.addAll(rpModules);
		return retVal;
	}
	
	public Set<Class<? extends ProxyService>> getProvidedRequestServices(RequestServiceModule module) {
		return providedRqServices.get(module);
	}
	
	public Set<Class<? extends ProxyService>> getProvidedResponseServices(ResponseServiceModule module) {
		return providedRpServices.get(module);
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
