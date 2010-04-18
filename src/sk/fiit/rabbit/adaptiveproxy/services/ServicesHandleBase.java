package sk.fiit.rabbit.adaptiveproxy.services;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeader;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceModule;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ByteServiceImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableByteServiceImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringServiceImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringServiceImpl;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService.readonly;
import sk.fiit.rabbit.adaptiveproxy.services.content.ByteContentService;
import sk.fiit.rabbit.adaptiveproxy.services.content.ModifiableBytesService;
import sk.fiit.rabbit.adaptiveproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.services.content.StringContentService;

public abstract class ServicesHandleBase<MessageType extends HttpMessageImpl<?>, ModuleType extends ServiceModule> implements ServicesHandle {
	static final Logger log = Logger.getLogger(ServicesHandleBase.class);
	private static ServiceModule baseModule = new BaseModule();
	
	static class BaseModule implements RequestServiceModule, ResponseServiceModule {
		@Override
		public boolean supportsReconfigure(PluginProperties newProps) {return false;}
		@Override
		public void stop() {}
		@Override
		public boolean start(PluginProperties props) {return true;}
		@Override
		public Set<Class<? extends ProxyService>> getProvidedRequestServices() {return null;}
		@Override
		public Set<Class<? extends ProxyService>> getProvidedResponseServices() {return null;}
		@Override
		public Set<Class<? extends ProxyService>> desiredRequestServices(
				RequestHeader clientRQHeaders) {return null;}
		@Override
		public Set<Class<? extends ProxyService>> desiredResponseServices(
				ResponseHeader webRPHeaders) {return null;}
		@Override
		public <Service extends ProxyService> ServiceProvider<Service> provideRequestService(
				HttpRequest request, Class<Service> serviceClass) {return null;}
		@Override
		public <Service extends ProxyService> ServiceProvider<Service> provideResponseService(
				HttpResponse response, Class<Service> serviceClass)
				throws ServiceUnavailableException {return null;}
	}
	
	
	final ServiceModulesManager manager;
	final MessageType httpMessage;
	private final ClassLoader servicesCLoader;
	private final List<ModuleType> modules;
	private final Map<ProxyService, ServiceBinding<?>> serviceBindings;
	private final List<ServiceBinding<?>> actualServicesBindings;
	private ServiceBinding<?> changedModelBinding;
	
	public ServicesHandleBase(MessageType httpMessage, List<ModuleType> modules, ServiceModulesManager manager) {
		this.httpMessage = httpMessage;
		this.modules = modules;
		this.serviceBindings = new HashMap<ProxyService, ServiceBinding<?>>();
		this.actualServicesBindings = new LinkedList<ServiceBinding<?>>();
		this.changedModelBinding = null;
		this.manager = manager;
		this.servicesCLoader = manager.getAdaptiveEngine().getPluginHandler().getServicesCLoader();
	}
	
	public boolean needContent(Set<Class<? extends ProxyService>> desiredServices) {
		/*if (contentNeeded(desiredServices))
			return true;*/
		for (ListIterator<ModuleType> iterator = modules.listIterator(modules.size()); iterator.hasPrevious();) {
			ModuleType module = iterator.previous();
			if (overlapSets(desiredServices, getProvidedSvcs(module))) {
				Set<Class<? extends ProxyService>> plgDesiredSvcs = null;
				try {
					plgDesiredSvcs = discoverDesiredServices(module);
				} catch (Throwable t) {
					log.info(getLogTextHead()+"Throwable raised while obtaining set of desired services from "+getLogTextCapital()+"ServiceModule of class '"+module.getClass()+"'",t);
				}
				if (plgDesiredSvcs == null)
					plgDesiredSvcs = Collections.emptySet();
				desiredServices.addAll(plgDesiredSvcs);
				if (contentNeeded(desiredServices)) {
					if (log.isDebugEnabled())
						log.debug(getLogTextHead()+"Service module "+module+" wants "
								+"'content' service for "+getLogTextNormal());
					return true;
				}
			}
		}
		return false;
	}
	
	private <E> boolean overlapSets(Set<E> set1, Set<E> set2) {
		for (E element : set1) {
			if (set2.contains(element))
				return true;
		}
		return false;
	}
	
	public static boolean contentNeeded(Set<Class<? extends ProxyService>> desiredServices) {
		return (desiredServices.contains(ModifiableStringService.class)
				|| desiredServices.contains(StringContentService.class)
				|| desiredServices.contains(ModifiableBytesService.class)
				|| desiredServices.contains(ByteContentService.class));
	}
	
	abstract Set<Class<? extends ProxyService>> getProvidedSvcs(ModuleType plugin);
	
	abstract Set<Class<? extends ProxyService>> discoverDesiredServices(ModuleType plugin);
	
	protected enum LogText {NORMAL,CAPITAL,SHORT};
	
	private String getLogTextHead() {
		return getText4Logging(LogText.SHORT)+": "+toString()+" | ";
	}
	
	private String getLogTextCapital() {
		return getText4Logging(LogText.CAPITAL);
	}
	
	private String getLogTextNormal() {
		return getText4Logging(LogText.NORMAL);
	}
	
	abstract String getText4Logging(LogText type);
	
	private ServiceProvider<ByteContentService> getByteService() {
		if (httpMessage.hasBody())
			return new ByteServiceImpl<MessageType>(httpMessage);
		else
			throw new ServiceUnavailableException(ByteContentService.class, "The massage carries no data", null);
	}
	
	private ServiceProvider<ModifiableBytesService> getModByteServie()  {
		if (httpMessage.hasBody())
			return new ModifiableByteServiceImpl<MessageType>(httpMessage);
		else
			throw new ServiceUnavailableException(ModifiableBytesService.class, "The massage carries no data", null);
	}
	
	private boolean hasTextutalContent () {
		if (!httpMessage.hasBody())
			return false;
		return manager.matchesStringServicePattern(httpMessage.getOriginalHeader());
	}
	
	private ServiceProvider<StringContentService> getStringService() {
		Throwable cause = null;
		String excMessage = "The message does not carry textual content";
		if (hasTextutalContent())
			try {
				return new StringServiceImpl<MessageType>(httpMessage,false);
			} catch (CharacterCodingException e) {
				excMessage = "Data of this message don't match it's charset";
				cause = e;
				log.debug(getLogTextHead()+excMessage);
			} catch (IOException e) {
				excMessage = "IOException raised while trying to detect charset by cpDetector";
				cause = e;
				log.warn(getLogTextHead()+excMessage,e);
			} catch (OutOfMemoryError e) {
				excMessage = "Java heap space saturated, unable to provide string content services \n";
				cause = e;
				log.warn(getLogTextHead()+"Java heap space saturated, unable to provide string content services \n",e);
			}  
		throw new ServiceUnavailableException(StringContentService.class, excMessage, cause);
	}
	
	private ServiceProvider<ModifiableStringService> getModStringService() {
		Throwable cause = null;
		String excMessage = "The message does not carry textual content";
		if (hasTextutalContent())
			try {
				return new ModifiableStringServiceImpl<MessageType>(httpMessage, false);
			}  catch (UnsupportedCharsetException e) {
				log.warn(getLogTextHead()+getLogTextCapital()
						+" header denotes unsupported charset "+e.getCharsetName());
			} catch (CharacterCodingException e) {
				excMessage = "Data of this message don't match it's charset";
				cause = e;
				log.debug(getLogTextHead()+excMessage);
			} catch (IOException e) {
				excMessage = "IOException raised while trying to detect charset by cpDetector";
				cause = e;
				log.warn(getLogTextHead()+excMessage,e);
			} catch (OutOfMemoryError e) {
				excMessage = "Java heap space saturated, unable to provide string content services \n";
				cause = e;
				log.warn(getLogTextHead()+"Java heap space saturated, unable to provide string content services \n",e);
			}  
		throw new ServiceUnavailableException(ModifiableStringService.class, excMessage, cause);
	}
	
	@SuppressWarnings("unchecked")
	private <Service extends ProxyService> ServiceProvider<Service> getBasicProvider(Class<Service> serviceClass) {
		if (serviceClass == ModifiableStringService.class)
			return (ServiceProvider<Service>)getModStringService();
		if (serviceClass == StringContentService.class)
			return (ServiceProvider<Service>)getStringService();
		if (serviceClass == ModifiableBytesService.class)
			return (ServiceProvider<Service>)getModByteServie();
		if (serviceClass == ByteContentService.class)
			return (ServiceProvider<Service>)getByteService();
		return null;
	}
	
	boolean isBasicService(Class<? extends ProxyService> serviceClass) {
		return (serviceClass == ModifiableStringService.class ||
				serviceClass == StringContentService.class ||
				serviceClass == ModifiableBytesService.class ||
				serviceClass == ByteContentService.class);
	}
	
	private class ServiceInfo<Service extends ProxyService> {
		final Class<Service> serviceClass;
		final ModuleType ignoredModule;
		
		public ServiceInfo(Class<Service> serviceClass, ModuleType ignoredModule) {
			this.serviceClass = serviceClass;
			this.ignoredModule = ignoredModule;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName()+"(class:"+serviceClass.getName()
				+", ignored:"+((ignoredModule == null)?"null":ignoredModule.toString())+")";
		}
	}
	
	private class ServiceRealization<Service extends ProxyService> {
		Service realService;
		ServiceProvider<Service> provider;
		ModuleType module;
		
		public ServiceRealization(Service realService, ServiceProvider<Service> provider, ModuleType module) {
			this.realService = realService;
			this.provider = provider;
			this.module = module;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName()+"(realService:"+realService.toString()+", provider:"+provider.toString()+", module:"+module.toString()+")";
		}
	}
	
	private class ServiceBinding<Service extends ProxyService> {
		final ServiceInfo<Service> svcInfo;
		Service proxiedService;
		ServiceRealization<Service> realization;
		
		public ServiceBinding(ServiceInfo<Service> svcInfo) {
			this.svcInfo = svcInfo;
		}
		
		void bindToProxy(Service proxiedService) {
			if (this.proxiedService != null)
				throw new IllegalStateException("Already bound to proxiedService");
			this.proxiedService = proxiedService;
		}
		
		void bindToService(ServiceRealization<Service> svcRealization) {
			this.realization = svcRealization;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return getClass().getSimpleName()+"(service:"+svcInfo.serviceClass.getName()+", module:"
				+((realization != null) ? realization.module : "unbound")+")";
		}
	}
	
	@SuppressWarnings("unchecked")
	private <Service extends ProxyService> Service createDecoratedService(final ServiceInfo<Service> svcInfo,
				final ServiceRealization<Service> realization) {
		final ServiceBinding<Service> binding = new ServiceBinding<Service>(svcInfo);
		InvocationHandler invHandler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
				if (method.getDeclaringClass() == Object.class)
					if (binding.realization != null)
						return method.invoke(binding.realization.realService, args);
					else
						return method.invoke(binding, args);
				boolean readOnlyMethod = method.isAnnotationPresent(readonly.class);
				if (log.isTraceEnabled())
					log.trace(getLogTextHead()+((readOnlyMethod)? "Read-only": "Modifying")+" method of a service provided by "+realization.realService+" called");
				if (changedModelBinding != binding) {
					if (!actualServicesBindings.contains(binding)) {
						// usable service realization is not initialized (within group of others)
						readingAttempt(binding);
						if (log.isDebugEnabled())
							log.debug(getLogTextHead()+"New "+svcInfo.serviceClass.getName()+" service realization has to be created");
						ServiceRealization<Service> newRealization = getNextService(svcInfo);
						binding.bindToService(newRealization);
						actualServicesBindings.add(binding);
					}
				} else {
					if (log.isTraceEnabled())
						log.trace(getLogTextHead()+"Same service used as last time");
				}
				// binding is actual
				Object retVal = method.invoke(binding.realization.realService, args);
				if (!readOnlyMethod) {
					modifyingAttempt(binding);
				}
				return retVal;
			}
		};
		Service proxyInstance = (Service) Proxy.newProxyInstance(servicesCLoader, new Class<?>[] {svcInfo.serviceClass}, invHandler);
		proxyInstance.toString();
		binding.bindToProxy(proxyInstance);
		binding.bindToService(realization);
		actualServicesBindings.add(binding);
		return proxyInstance;
	}
	
	private void readingAttempt(ServiceBinding<?> binding) {
		if (changedModelBinding != null && changedModelBinding != binding) {
			applyLastChanges();
		}
	}
	
	public void headerBeingRead(Object obj) {
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"An attempt to read message header");
		ServiceBinding<?> binding = null;
		if (obj instanceof ProxyService) {
			// TODO zistit binding z proxyInstance = obj
		}
		readingAttempt(binding);
	}
	
	private void modifyingAttempt(ServiceBinding<?> binding) {
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"Modifying attempt, actualServicesBindings list cleared");
		actualServicesBindings.clear();
		if (binding != null)
			actualServicesBindings.add(binding);
		changedModelBinding = binding;
	}
	
	public void headerBeingModified(Object obj) {
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"An attempt to modify message header");
		headerBeingRead(obj);
		ServiceBinding<?> binding = null;
		if (obj instanceof ProxyService) {
			// TODO zistit binding z proxyInstance = obj
		}
		modifyingAttempt(binding);
	}
	
	private <Service extends ProxyService> Service createRealService(final ServiceProvider<Service> svcProvider, Class<Service> serviceClass) {
		Service svc = null;
		Throwable cause = null;
		try {
			svc = svcProvider.getService();
			boolean modifyingInit = true;
			try {
				modifyingInit = svcProvider.initChangedModel();
			} catch (Throwable t) {
				log.info(getLogTextHead()+"Service provider "+svcProvider+" raised throwable when initChangedModel() called",t);
			}
			if (log.isDebugEnabled())
				log.debug(getLogTextHead()+((modifyingInit)? "M": "Non-m")+"odifying initialization of provider "+svcProvider+" made");
			if (modifyingInit)
				actualServicesBindings.clear();
		} catch (Throwable t) {
			log.info(getLogTextHead()+"Service provider "+svcProvider+" raised throwable when getService() called",t);
			cause = t;
		}
		if (svc == null) {
			if (cause == null)
				throw new ServiceUnavailableException(serviceClass, "Module's provider returned no implementation", null);
			else
				throw new ServiceUnavailableException(serviceClass, "Module's provider failed at providing implementation", cause);
		}
		return svc;
	}
	
	
	private <Service extends ProxyService> ServiceRealization<Service> getRealService(ModuleType module, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		ServiceProvider<Service> svcProvider = null;
		try {
			svcProvider = callProvideService(module, serviceClass);
		} catch (Throwable e) {
			if (e instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException)e;
			else {
				log.info(getLogTextHead()+"Module "+module+" raised throwable when asked for provider for service "+serviceClass,e);
				throw new ServiceUnavailableException(serviceClass, "Module failed at providing service provider", e);
			}
		}
		if (svcProvider == null)
			throw new ServiceUnavailableException(serviceClass, "Module returned no service provider", null);
		Service svcImpl = createRealService(svcProvider, serviceClass);
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"Service provider "+svcProvider+" provided for service "+serviceClass.getName());
		return new ServiceRealization<Service>(svcImpl, svcProvider, module);
	}
	
	abstract <Service extends ProxyService> ServiceProvider<Service> callProvideService(ModuleType module, Class<Service> serviceClass);
	
	abstract <Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider);
	
	@Override
	public <Service extends ProxyService> Service getService(Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if (log.isDebugEnabled())
			log.debug(getLogTextHead()+"Asking for service "+serviceClass.getName());
		ServiceInfo<Service> svcInfo = new ServiceInfo<Service>(serviceClass, null);
		return createDecoratedService(svcInfo, getNextService(svcInfo));
	}
	
	@SuppressWarnings("unchecked")
	public <Service extends ProxyService> Service getNextService(Service previousService) throws ServiceUnavailableException {
		if (previousService == null)
			throw new IllegalArgumentException("Previous service can not be null");
		ServiceBinding<Service> svcContainer = (ServiceBinding<Service>)serviceBindings.get(previousService);
		if (log.isDebugEnabled())
			log.debug(getLogTextHead()+"Asking for next service "+svcContainer.svcInfo.serviceClass.getName()+"(previous: "+previousService+")");
		ServiceInfo<Service> svcInfo = new ServiceInfo<Service>(svcContainer.svcInfo.serviceClass, svcContainer.realization.module);
		return createDecoratedService(svcInfo, getNextService(svcInfo));
	}
	
	@SuppressWarnings("unchecked")
	private <Service extends ProxyService> ServiceRealization<Service> getNextService(ServiceInfo<Service> svcInfo) throws ServiceUnavailableException {
		boolean skip = (svcInfo.ignoredModule != null);
		// try to use already initialized service
		for (ServiceBinding<?> existingBinding : actualServicesBindings) {
			if (log.isTraceEnabled())
				log.trace(getLogTextHead()+"Available binding "+existingBinding);
			if (existingBinding.svcInfo.serviceClass == svcInfo.serviceClass) {
				if (existingBinding.realization.module == svcInfo.ignoredModule) {
					log.trace("This is ignored module, skipping ended");
					skip = false;
					continue;
				}
				if (skip)
					continue;
				if (log.isTraceEnabled())
					log.trace(getLogTextHead()+"This binding was picked");
				return (ServiceRealization<Service>)existingBinding.realization;
			}
		}
		skip = (svcInfo.ignoredModule != null);
		ServiceUnavailableException cause = null;
		// if one of base services, provide realizations
		ServiceProvider<Service> baseSvcProvider = getBasicProvider(svcInfo.serviceClass);
		if (baseSvcProvider != null) {
			if (log.isTraceEnabled())
				log.trace(getLogTextHead()+"Base service provider "+baseSvcProvider+" created");
			return new ServiceRealization<Service>(baseSvcProvider.getService(), baseSvcProvider, (ModuleType)baseModule);
		}
		for (ModuleType module : modules) {
			try {
				Set<Class<? extends ProxyService>> providedSvcs = getProvidedSvcs(module);
				if (providedSvcs != null && providedSvcs.contains(svcInfo.serviceClass)) {
					if (log.isTraceEnabled())
						log.trace(getLogTextHead()+"Fitting module "+module);
					if (module == svcInfo.ignoredModule) {
						log.trace("This is ignored module, skipping ended");
						skip = false;
						continue;
					}
					if (skip)
						continue;
					if (log.isTraceEnabled())
						log.trace(getLogTextHead()+"This module was picked");
					return getRealService(module, svcInfo.serviceClass);
				}
			} catch (ServiceUnavailableException e) {
				log.info(getLogTextHead()+"ServiceUnavailableException raised while obtaining service providers from "
							+getText4Logging(LogText.CAPITAL)+"ServiceModule '"+module+"'",e);
				cause = e;
			}
		}
		// throw the last exception 
		throw cause;
	}
	
	@Override
	public <Service extends ProxyService> boolean isServiceAvailable(Class<Service> serviceClass) {
		try {
			getService(serviceClass);
			return true;
		} catch (ServiceUnavailableException e) {
			log.trace(getLogTextHead()+"Service unavailable: "+serviceClass);
		}
		return false;
	}
	
	private void applyLastChanges() {
		if (log.isDebugEnabled())
			log.debug(getLogTextHead()+"Aplying changes made in inner model of "+changedModelBinding.realization.realService);
		ServiceProvider<?> svcProvider = changedModelBinding.realization.provider;
		changedModelBinding = null;
		callDoChanges(svcProvider);
	}
	
	public void finalize() {
		if (changedModelBinding != null) {
			applyLastChanges();
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
}
