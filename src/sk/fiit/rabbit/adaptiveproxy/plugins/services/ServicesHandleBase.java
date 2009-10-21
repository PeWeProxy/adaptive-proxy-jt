package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import rabbit.http.HttpHeader;
import rabbit.util.CharsetDetector;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ByteContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableBytesService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.utils.MemoryUsageInspector;

public abstract class ServicesHandleBase implements ServicesHandle {
	static final Logger log = Logger.getLogger(ServicesHandleBase.class);
			
	final HttpMessageImpl httpMessage;
	final List<ServiceProvider> providersList;
	final Map<ProxyService, ServiceProvider> serviceProviders;
	final Map<Class<? extends ProxyService>, List<ProxyService>> services;
	boolean wantContent = false;
	boolean servicesDiscovered = false;
	int lastRtnedSvcProviderIndex = -1;
	boolean changesPropagation = false;
	
	static class ServicePluginsComparator implements Comparator<ServicePlugin> {
		Map<ServicePlugin, Set<Class<? extends ProxyService>>> dependencies =
			new HashMap<ServicePlugin, Set<Class<? extends ProxyService>>>();
		Map<ServicePlugin, Set<Class<? extends ProxyService>>> providedServices =
			new HashMap<ServicePlugin, Set<Class<? extends ProxyService>>>();
		
		public ServicePluginsComparator(List<? extends ServicePlugin> plugins) {
			for (ServicePlugin servicePlugin : plugins) {
				Set<Class<? extends ProxyService>> pluginDependencies = servicePlugin.getDependencies();
				if (pluginDependencies != null)
					dependencies.put(servicePlugin, pluginDependencies);
				Set<Class<? extends ProxyService>> pluginServices = servicePlugin.getProvidedServices();
				if (pluginServices != null)
					providedServices.put(servicePlugin, servicePlugin.getProvidedServices());
			}
		}
		
		@Override
		public int compare(ServicePlugin o1, ServicePlugin o2) {
			Set<Class<? extends ProxyService>> dependenciesOf2 = dependencies.get(o2);
			if (dependenciesOf2 != null) {
				Set<Class<? extends ProxyService>> providedSvcsBy1 = providedServices.get(o1);
				for (Class<? extends ProxyService> dependencyClass : dependenciesOf2) {
					if (providedSvcsBy1.contains(dependencyClass)) {
						return -1;
					}
				}
			}
			Set<Class<? extends ProxyService>> dependenciesOf1 = dependencies.get(o1);
			if (dependenciesOf1 != null) {
				Set<Class<? extends ProxyService>> providedSvcsBy2 = providedServices.get(o2);
				for (Class<? extends ProxyService> dependencyClass : dependenciesOf1) {
					if (providedSvcsBy2.contains(dependencyClass)) {
						return 1;
					}
				}
			}
			return 0;
		}
	}
	
	abstract class GenericServiceProvider implements ProxyService, RequestServiceProvider, ResponseServiceProvider {
		public void setRequestContext(ModifiableHttpRequest request) {
			// no-op
		}
		
		@Override
		public void setResponseContext(ModifiableHttpResponse response) {
			// no-op
		}

		@Override
		public void doChanges() {
			// no-op
		}

		@Override
		public ProxyService getService() {
			return this;
		}
	}
	
	class BytesServiceProvider extends GenericServiceProvider implements ByteContentService , RequestServiceProvider, ResponseServiceProvider {
		
		public BytesServiceProvider() {
			if (httpMessage.getData() == null)
				throw new IllegalStateException("Associated HTTP message does not contain any data");
		}
		
		@Override
		public byte[] getData() {
			byte[] data = httpMessage.getData();
			return Arrays.copyOf(data, data.length);
		}

		@Override
		public String getServiceIdentification() {
			return "AdaptiveProxy.ByteContentService";
		}

		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return ByteContentService.class;
		}
		
		@Override
		public void doChanges() {
			byte[] data = httpMessage.getData();
			if (getOriginalHeader().getHeader("Content-Length") != null) {
				getProxyHeader().setHeader("Content-Length", Integer.toString(data.length, 10));
			}
		}
	}
	
	class ModifiableBytesServiceProvider extends GenericServiceProvider implements ModifiableBytesService {
		
		public ModifiableBytesServiceProvider(BytesServiceProvider byteSvcprovider) {
			if (httpMessage.getData() == null)
				throw new IllegalStateException("Associated HTTP message does not contain any data");;
		}
		
		@Override
		public void setData(byte[] data) {
			httpMessage.setData(data);
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return ModifiableBytesService.class;
		}
		
		@Override
		public String getServiceIdentification() {
			return "AdaptiveProxy.ModifiableBytesService";
		}
	}
	
	class ContentServicesProvider extends GenericServiceProvider implements StringContentService {
		byte[] lastByteData = null;
		StringBuilder sb;
		
		private boolean invoked = false;
		
		public ContentServicesProvider(String content) {
			if (httpMessage.getData() == null)
				throw new IllegalStateException("Associated HTTP message does not contain any data");
			// try to load bytes into StringBuilder, this can cause HeapOverflow
			//sb = new StringBuilder(content);
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return StringContentService.class;
		}
		
		private boolean underlyingBytesChanged() {
			return httpMessage.getData() != lastByteData;
		}
		
		@Override
		public String getContent() {
			this.invoked = true;
			
			if (underlyingBytesChanged()) {
				byte[] data = httpMessage.getData();
				MemoryUsageInspector.printMemoryUsage(log, "Before StringBuilder creation");
				sb = new StringBuilder(new String(data,CharsetDetector.detectCharset(getProxyHeader())));
				MemoryUsageInspector.printMemoryUsage(log, "After StringBuilder creation");
				lastByteData = data;
			}
			return sb.toString();
		}
		
		@Override
		public void doChanges() {
			if(invoked) {
				HttpHeader headers = getProxyHeader();
				String s = sb.toString();
				httpMessage.setData(s.getBytes(CharsetDetector.detectCharset(headers)));
				lastByteData = httpMessage.getData();
			}
		}
		
		@Override
		public String getServiceIdentification() {
			return "AdaptiveProxy.StringContentService";
		}
	}
	
	class ModifiableContentServiceProvider extends GenericServiceProvider implements ModifiableStringService {
		final ContentServicesProvider stringSvcProvider;
		
		public ModifiableContentServiceProvider(ContentServicesProvider stringSvcProvider) {
			this.stringSvcProvider = stringSvcProvider;;
		}
		
		@Override
		public StringBuilder getModifiableContent() {
			if (stringSvcProvider.sb == null)
				stringSvcProvider.getContent();
			return stringSvcProvider.sb;
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return ModifiableStringService.class;
		}
		
		@Override
		public String getServiceIdentification() {
			return "AdaptiveProxy.ModifiableContentService";
		}

		@Override
		public void setContent(String content) {
			stringSvcProvider.sb.setLength(0);
			stringSvcProvider.sb.append(content);
		}

		@Override
		public void setCharset(Charset charset) {
			HttpHeader headers = getProxyHeader();
			String cType = headers.getHeader("Content-Type");
			if (cType == null)
				return;
			String trailing = "";
			String leading = "charset=";
			int chsIndex = cType.indexOf(leading);
			if (chsIndex != -1) {
				int afterChIndex = cType.substring(chsIndex+8).indexOf(';');
				if (afterChIndex != -1)
					trailing = cType.substring(chsIndex+8+afterChIndex);
			} else {
				chsIndex = cType.length();
				leading = "; "+leading;
			}
			StringBuilder sbTmp = new StringBuilder();
			sbTmp.append(cType.substring(0, chsIndex));
			sbTmp.append(leading);
			sbTmp.append(charset.toString());
			sbTmp.append(trailing);
			headers.setHeader("Content-Type", sbTmp.toString());
		}
	}
	
	public ServicesHandleBase(HttpMessageImpl httpMessage) {
		this.httpMessage = httpMessage;
		providersList = new LinkedList<ServiceProvider>();
		serviceProviders = new HashMap<ProxyService, ServiceProvider>();
		services = new HashMap<Class<? extends ProxyService>, List<ProxyService>>();
	}
	
	<T extends ServicePlugin> void doContentNeedDiscovery(List<T> plugins) {
		for (T plugin : plugins) {
			if (discoverContentNeed(plugin)) {
					wantContent = true;
					break;
			}
			//discoverContentNeed(pluginDependencies);
		}
	}
	
	abstract <T extends ServicePlugin> boolean discoverContentNeed(T plugin);
	
	/*protected void discoverContentNeed(Set<Class<ProxyService>> dependencies) {
		for (Class<ProxyService> serviceClass : dependencies) {
			if (serviceClass.equals(StringContentService.class) ||
					serviceClass.equals(ModifiableContentService.class)) {
				wantContent = true;
				return;
			}
		}
	}*/
	
	public boolean wantContent() {
		return wantContent;
	}
	
	public void doServiceDiscovery() {
		if (servicesDiscovered)
			return;
		servicesDiscovered = true;
		byte[] data = httpMessage.getData();
		if (data != null) {
			// message has some content so we add BytesServiceProvider provider
			BytesServiceProvider byteSvcProvider = new BytesServiceProvider();
			addServiceProvider(byteSvcProvider, byteSvcProvider, ByteContentService.class);
			
			HttpHeader header = getOriginalHeader();
			String contentType = header.getHeader("Content-Type");
			if (contentType != null && contentType.startsWith("text")) {
				Charset charset = CharsetDetector.detectCharset(header);
				CharsetDecoder decoder = charset.newDecoder();
				decoder.onMalformedInput(CodingErrorAction.REPORT);
				decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
				CharBuffer charbuf = null;
				try {
					charbuf = decoder.decode(ByteBuffer.wrap(data));
				} catch (CharacterCodingException e) {
					log.debug("Data of this message is not valid text data [requested: "+
							getRequestHeader().getRequestLine()+" | message entity type: "+contentType+"]");
				}
				if (charbuf != null) {
					// message has some valid text content so we add StringContentService provider
					try {
						ContentServicesProvider contentSvcProvider = new ContentServicesProvider(new String(charbuf.array()));
						addServiceProvider(contentSvcProvider, contentSvcProvider, StringContentService.class);
					} catch (OutOfMemoryError e) {
						log.warn("Java heap space saturated, unable to provide StringContentService",e);
					}
				}
			}
		}
		doSpecificServiceDiscovery();
		setServicesContext();
	}
	
	abstract HttpHeader getRequestHeader();
	
	abstract HttpHeader getOriginalHeader();
	
	abstract HttpHeader getProxyHeader();
	
	abstract void doSpecificServiceDiscovery();
	
	final void addServiceProviders(ServicePlugin plugin, List<? extends ServiceProvider> providedServices) {
		for (ServiceProvider serviceProvider : providedServices) {
			ProxyService service = serviceProvider.getService();
			Class<? extends ProxyService> implementedService = serviceProvider.getServiceClass();
			if (!implementedService.isInstance(service)) {
				log.warn("Provided service "+service+" is not of claimed type "+implementedService+" ("+
						"service provided by "+serviceProvider+" obtained from plugin "+plugin+")");
				continue;
			}
			addServiceProvider(serviceProvider, service, implementedService);
		}
	}
	
	void addServiceProvider(ServiceProvider serviceProvider, ProxyService service, Class<? extends ProxyService> serviceClass) {
		providersList.add(serviceProvider);
		serviceProviders.put(service, serviceProvider);
		List<ProxyService> suchServicesList = services.get(serviceClass);
		if (suchServicesList == null) {
			suchServicesList = new ArrayList<ProxyService>(1);
			services.put(serviceClass, suchServicesList);
		}
		suchServicesList.add(service);
	}
	
	private void setServicesContext() {
		if (httpMessage.getData() != null) {
			// message has some content so we add ModifiableBytesServiceProvider provider
			BytesServiceProvider byteSvcProvider = (BytesServiceProvider)services.get(ByteContentService.class).get(0);
			ModifiableBytesServiceProvider modByteSvcProvider = new ModifiableBytesServiceProvider(byteSvcProvider);
			addServiceProvider(modByteSvcProvider, modByteSvcProvider, ModifiableBytesService.class);
			
			List<ProxyService> tmp = services.get(StringContentService.class);
			if (tmp != null) {
				// message has some string content so we add ModifiableContentService provider
				ContentServicesProvider contentSvcProvider = (ContentServicesProvider)tmp.get(0);
				ModifiableContentServiceProvider modContentSvcProvider = new ModifiableContentServiceProvider(contentSvcProvider);
				addServiceProvider(modContentSvcProvider, modContentSvcProvider, ModifiableStringService.class);
			}
		}
		doSetContext();
	}
	
	abstract void doSetContext();

	public void doChanges() {
		if (lastRtnedSvcProviderIndex != -1) {
			for (ListIterator<ServiceProvider> backwardIterator = providersList.listIterator(lastRtnedSvcProviderIndex+1); backwardIterator.hasPrevious();) {
				ServiceProvider serviceProvider = (ServiceProvider) backwardIterator.previous();
				serviceProvider.doChanges();
			}
		}
		providersList.clear();
		serviceProviders.clear();
		services.clear();
	}
	
	@Override
	public Set<Class<? extends ProxyService>> getAvailableServices() {
		return services.keySet();
	}
	
	@Override
	public <S extends ProxyService> S getService(Class<S> serviceClass)
			throws ServiceUnavailableException {
		return getServices(serviceClass).get(0);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <S extends ProxyService> List<S> getServices(Class<S> serviceClass)
			throws ServiceUnavailableException {
		List<S> retVal = (List<S>) services.get(serviceClass);
		if (retVal == null || retVal.size() == 0)
			throw new ServiceUnavailableException(serviceClass);
		if (!changesPropagation) {
			changesPropagation = true;
			for (S service : retVal) {
				ServiceProvider thisServiceProvider = serviceProviders.get(service);
				int indexOfThisProvider = providersList.indexOf(thisServiceProvider); 
				if (indexOfThisProvider < lastRtnedSvcProviderIndex) {
					for (ListIterator<ServiceProvider> listIterator = providersList.listIterator(lastRtnedSvcProviderIndex+1); listIterator.hasPrevious();) {
						listIterator.previous().doChanges();
						lastRtnedSvcProviderIndex--;
						if (indexOfThisProvider == lastRtnedSvcProviderIndex)
							break;
					}
				} else
					lastRtnedSvcProviderIndex = indexOfThisProvider;
			}
			changesPropagation = false;
		}
		return retVal;
	}
}
