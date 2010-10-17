package sk.fiit.peweproxy.services;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.plugins.services.ServiceModule;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;
import sk.fiit.peweproxy.plugins.services.content.ByteServiceImpl;
import sk.fiit.peweproxy.plugins.services.content.ModifiableByteServiceImpl;
import sk.fiit.peweproxy.plugins.services.content.ModifiableStringServiceImpl;
import sk.fiit.peweproxy.plugins.services.content.StringServiceImpl;
import sk.fiit.peweproxy.services.ProxyService.readonly;
import sk.fiit.peweproxy.services.content.ByteContentService;
import sk.fiit.peweproxy.services.content.ModifiableBytesService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.peweproxy.services.content.StringContentService;

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
		public void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices) {}
		@Override
		public void getProvidedResponseServices(Set<Class<? extends ProxyService>> providedServices) {}
		@Override
		public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices,
				RequestHeader clientRQHeaders) {}
		@Override
		public void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices,
				ResponseHeader webRPHeaders) {}
		@Override
		public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(
				HttpRequest request, Class<Service> serviceClass)
				throws ServiceUnavailableException {return null;}
		@Override
		public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
				HttpResponse response, Class<Service> serviceClass)
				throws ServiceUnavailableException {return null;}
	}
	
	
	final ModulesManager manager;
	final MessageType httpMessage;
	private final ClassLoader servicesCLoader;
	private final List<ModuleType> modules;
	private final Map<ProxyService, ServiceBinding<?>> serviceBindings;
	private final Queue<ServiceBinding<?>> inCodeOfStack;
	private final List<ServiceBinding<?>> actualServicesBindings;
	private ServiceBinding<?> changedModelBinding;
	private ServiceBinding<?> bindingDoingChanges;
	private ModuleType moduleExecutingProvide;
	
	public ServicesHandleBase(MessageType httpMessage, List<ModuleType> modules, ModulesManager manager) {
		this.httpMessage = httpMessage;
		this.modules = modules;
		this.serviceBindings = new HashMap<ProxyService, ServiceBinding<?>>();
		this.inCodeOfStack = new LinkedList<ServiceBinding<?>>();
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
				Set<Class<? extends ProxyService>> plgDesiredSvcs = new HashSet<Class<? extends ProxyService>>();
				try {
					discoverDesiredServices(module,plgDesiredSvcs);
				} catch (Throwable t) {
					log.info(getLogTextHead()+"Throwable raised while obtaining set of desired services from "
								+getLogTextCapital()+"ServiceModule of class '"+module.getClass()+"'",t);
				}
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
	
	abstract void discoverDesiredServices(ModuleType plugin,
			Set<Class<? extends ProxyService>> desiredServices);
	
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
		return manager.matchesStringServicePattern(httpMessage.getProxyHeader());
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
		final Service realService;
		final ServiceProvider<Service> provider;
		final ModuleType module;
		final Map<Method, Boolean> readonlyFlags;
		final boolean initChangedModel;
		
		public ServiceRealization(Service realService, ServiceProvider<Service> provider, ModuleType module, boolean initChangedModel) {
			this.realService = realService;
			this.provider = provider;
			this.module = module;
			readonlyFlags = new HashMap<Method, Boolean>();
			this.initChangedModel = initChangedModel;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName()+"(realService:"+realService.toString()+", provider:"
				+provider.toString()+", module:"+module.toString()+")";
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
			return getClass().getSimpleName()+"(service:"+svcInfo.serviceClass.getName()+", module:"
				+((realization != null) ? realization.module : "unbound")+")";
		}
	}
	
	private boolean isReadOnlyMethod(Method method, ServiceRealization<?> realization) {
		if (method.isAnnotationPresent(readonly.class))
			return true;
		try {
			Boolean readOnly = realization.readonlyFlags.get(method);
			if (readOnly == null) {
				readOnly = realization.realService.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(readonly.class);
				realization.readonlyFlags.put(method, readOnly);
			}
			return readOnly;
		} catch (Exception ignored) {}
		return false;
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
				httpMessage.checkThreadAccess();
				boolean readOnlyMethod = isReadOnlyMethod(method, binding.realization);
				if (log.isTraceEnabled())
					log.trace(getLogTextHead()+((readOnlyMethod)? "Read-only": "Modifying")+" method "+method.getName()+"("
								+Arrays.toString(method.getParameterTypes())
								+") of a service provided by "+realization.realService+" called");
				if (changedModelBinding != binding) {
					if (!actualServicesBindings.contains(binding)) {
						// usable service realization is not initialized (within group of others)
						readingAttempt(binding);
						if (log.isDebugEnabled())
							log.debug(getLogTextHead()+"New "+svcInfo.serviceClass.getName()+" service realization has to be created");
						ServiceRealization<Service> newRealization = getNewNextService(svcInfo);
						binding.bindToService(newRealization);
						registerSvcRealization(newRealization, binding);
					} else if (changedModelBinding != null) {
						// because the target service is in the list, but is not referenced by changedModelBinding
						// target service was initialized without need to access message data {e.g. service for response accessed request only}
						if (log.isDebugEnabled())
							log.debug(getLogTextHead()+"Inner model of called service realization is up-do-date, even if there's other realization with changed model");
						if (!readOnlyMethod) {
							if (log.isDebugEnabled())
								log.debug(getLogTextHead()+"Called method is modifying, commiting changed model of other service realization");
							// we commit changes referenced by changedModelBinding into the message so that it is safe
							// (changedModelBinding = null) to set changedModelBinding to new binding 
							while (changedModelBinding != null)
								applyLastChanges();
						}
					}
				} else {
					if (log.isTraceEnabled())
						log.trace(getLogTextHead()+"Same service used as last time");
				}
				// binding is actual
				inCodeOfStack.add(binding);
				Object retVal = method.invoke(binding.realization.realService, args);
				inCodeOfStack.poll();
				if (!readOnlyMethod) {
					modifyingAttempt(binding,true);
				}
				return retVal;
			}
		};
		Service proxyInstance = (Service) Proxy.newProxyInstance(servicesCLoader, new Class<?>[] {svcInfo.serviceClass}, invHandler);
		//proxyInstance.toString();
		binding.bindToProxy(proxyInstance);
		binding.bindToService(realization);
		registerSvcRealization(realization, binding);
		return proxyInstance;
	}
	
	private <Service extends ProxyService> void registerSvcRealization(ServiceRealization<Service> newRealization, ServiceBinding<Service> binding) {
		if (binding.realization != newRealization)
			throw new IllegalStateException("Passed binding does not reference passed service realization");
		if (newRealization.initChangedModel) {
			changedModelBinding = binding;
			actualServicesBindings.clear();
		}
		actualServicesBindings.add(binding);
	}
	
	private void readingAttempt(ServiceBinding<?> binding) {
		if (changedModelBinding != null && changedModelBinding != binding) {
			applyLastChanges();
		}
	}
	
	private ServiceBinding<?> getExecutingBinding() {
		return inCodeOfStack.peek();
	}
	
	public void headerBeingRead() {
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"An attempt to read message header");
		readingAttempt(getExecutingBinding());
	}
	
	private void modifyingAttempt(ServiceBinding<?> binding, boolean after) {
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"Modifying operation "+((after == true) ? "" : "to be ")+"executed, actualServicesBindings list cleared" +
					((binding != null) ? ", binding doing changes added" : "")+" and changedModelBinding set");
		actualServicesBindings.clear();
		if (binding != null)
			actualServicesBindings.add(binding);
		if (bindingDoingChanges != binding)
			changedModelBinding = binding;
	}
	
	public void headerBeingModified() {
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"An attempt to modify message header");
		if (inReadOnlyState()) {
			UnsupportedOperationException e =  new UnsupportedOperationException("Modifying message header is not allowed when" +
					" discovering services");
			log.info("Attempt to modify message header when discovering services",e);
			throw e;
		}
		ServiceBinding<?> binding = getExecutingBinding();
		if (binding != bindingDoingChanges)
			throw new UnsupportedOperationException("Service module is not allowed to modify header outside doChanges() method");
		readingAttempt(binding);
		modifyingAttempt(binding,false);
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
	
	private void providingDiscoveryStart(ModuleType module) {
		if (moduleExecutingProvide == null)
			moduleExecutingProvide = module; //go to read-only state
	}
	
	private void providingDiscoveryEnd(ModuleType module) {
		if (module == moduleExecutingProvide)
			moduleExecutingProvide = null; //cancel read-only state
	}
	
	public boolean inReadOnlyState() {
		return moduleExecutingProvide != null;
	}
	
	private <Service extends ProxyService> ServiceRealization<Service> getRealService(ModuleType module, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		ServiceProvider<Service> svcProvider = null;
		providingDiscoveryStart(module);
		try {
			svcProvider = callProvideService(module, serviceClass);
		} catch (Throwable e) {
			if (e instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException)e;
			else {
				log.info(getLogTextHead()+"Module "+module+" raised throwable when asked for provider for service "+serviceClass,e);
				throw new ServiceUnavailableException(serviceClass, "Module failed at providing service provider", e);
			}
		} finally {
			providingDiscoveryEnd(module);
		}
		if (svcProvider == null)
			throw new ServiceUnavailableException(serviceClass, "Module returned no service provider", null);
		boolean wasEmpty = actualServicesBindings.isEmpty();
		Service svcImpl = createRealService(svcProvider, serviceClass);
		if (log.isTraceEnabled())
			log.trace(getLogTextHead()+"Service provider "+svcProvider+" provided for service "+serviceClass.getName());
		return new ServiceRealization<Service>(svcImpl, svcProvider, module, (!wasEmpty && actualServicesBindings.isEmpty()));
	}
	
	abstract <Service extends ProxyService> ServiceProvider<Service> callProvideService(ModuleType module, Class<Service> serviceClass);
	
	abstract <Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider);
	
	@Override
	public <Service extends ProxyService> Service getService(Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if (log.isDebugEnabled())
			log.debug(getLogTextHead()+"Asking for service "+serviceClass.getName());
		ServiceInfo<Service> svcInfo = new ServiceInfo<Service>(serviceClass, null);
		return getNextService(svcInfo);
	}
	
	@SuppressWarnings("unchecked")
	public <Service extends ProxyService> Service getNextService(Service previousService) throws ServiceUnavailableException {
		if (previousService == null)
			throw new IllegalArgumentException("Previous service can not be null");
		ServiceBinding<Service> svcContainer = (ServiceBinding<Service>)serviceBindings.get(previousService);
		if (log.isDebugEnabled())
			log.debug(getLogTextHead()+"Asking for next service "+svcContainer.svcInfo.serviceClass.getName()+"(previous: "+previousService+")");
		ServiceInfo<Service> svcInfo = new ServiceInfo<Service>(svcContainer.svcInfo.serviceClass, svcContainer.realization.module);
		return getNextService(svcInfo);
	}
	
	private <Service extends ProxyService> Service getNextService(ServiceInfo<Service> svcInfo) {
		checkForbiddenServices(svcInfo);
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
				return (Service) existingBinding.proxiedService;
			}
		}
		return createDecoratedService(svcInfo, getNewNextService(svcInfo));
	}
	
	private <Service extends ProxyService> void checkForbiddenServices(ServiceInfo<Service> svcInfo) {
		if ((svcInfo.serviceClass == ModifiableStringService.class
				|| svcInfo.serviceClass == ModifiableBytesService.class) && inReadOnlyState()) {
			ServiceUnavailableException e = new ServiceUnavailableException(svcInfo.serviceClass, "Conent modifying services are" +
					"unavilable when discovering services", null);
			log.info(getLogTextHead()+"ServiceUnavailableException raised when asked for "+svcInfo.serviceClass.getSimpleName()
					+" service for "+getText4Logging(LogText.NORMAL)+" because we are discovering services now");
			throw e;
		}
	}
	
	@SuppressWarnings("unchecked")
	private <Service extends ProxyService> ServiceRealization<Service> getNewNextService(ServiceInfo<Service> svcInfo) throws ServiceUnavailableException {
		checkForbiddenServices(svcInfo);
		boolean skip = (svcInfo.ignoredModule != null);
		ServiceUnavailableException cause = null;
		// if one of base services, provide realizations
		ServiceProvider<Service> baseSvcProvider = getBasicProvider(svcInfo.serviceClass);
		if (baseSvcProvider != null) {
			if (log.isTraceEnabled())
				log.trace(getLogTextHead()+"Base service provider "+baseSvcProvider+" created");
			return new ServiceRealization<Service>(baseSvcProvider.getService(), baseSvcProvider, (ModuleType)baseModule, false);
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
					// providing a service may not access this message and therefore its binding may be added to the actualServicesBindings
					// without applying changes made by changedModelBinding
					//readingAttempt(null);
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
		inCodeOfStack.add(changedModelBinding);
		bindingDoingChanges = changedModelBinding;
		changedModelBinding = null;
		callDoChanges(svcProvider);
		inCodeOfStack.poll();
		bindingDoingChanges = null;
	}
	
	public void finalize() {
		if (changedModelBinding != null) {
			applyLastChanges();
		}
	}
	
	public ModulesManager getManager() {
		return manager;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
}
