package sk.fiit.peweproxy.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.plugins.services.ChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.ServiceProvider;
import sk.fiit.peweproxy.services.content.ModifiableStringService;

public abstract class ChunkServicesHandleImpl<ModuleType extends ProxyPlugin> extends
		ServicesHandleBase<ModuleType> implements ChunkServicesHandle {
	
	protected byte[] data = null;
	boolean sent = false;
	protected final ChunksRemainsImpl remainsStore;
	protected final boolean finalization;

	public ChunkServicesHandleImpl(HttpMessageImpl<?> httpMessage, List<ModuleType> modules,
			ModulesManager manager, byte[] data) {
		super(httpMessage, modules, manager);
		this.finalization = data == null;
		this.remainsStore = httpMessage.getChunkRemains();
		this.data = remainsStore.joinCeasedData(data);
		HttpMessageImpl<?>.CharsetWrapper charsetWrp = httpMessage.getCharsetWrapper();
		if (charsetWrp == null) {
			// first time detecting textual content / charset
			httpMessage.setContentCharset(null);
			if (super.hasTextutalContent()) {
				Charset charset = null;
				try {
					charset = CharsetUtils.detectCharset(httpMessage.originalMessage().getHeader(), data, false);
					httpMessage.setContentCharset(charset);
				} catch (IOException ignored) {
					// CharsetWrapper is set (to reference null) already
				}
			}
		}
		if (hasTextutalContent()) {
			// needed for ceasing undecoded bytes to avoid having chunk started with bytes
			// remaining for character split between two chunks
			getService(ModifiableStringService.class);
		}
	}
	
	@Override
	protected boolean hasTextutalContent() {
		if (httpMessage.getCharsetWrapper().getCharset() != null)
			return true;
		return false;
	}
	
	@Override
	boolean dataAccessible() {
		return true;
	}

	@Override
	boolean isReadOnly() {
		return sent;
	}
	
	void setReadOnly() {
		sent = true;
	}
	
	@Override
	public byte[] getData() {
		return data;
	}
	
	@Override
	public void setData(byte[] data) {
		HttpMessageImpl<?>.CharsetWrapper charsetWrp = httpMessage.getCharsetWrapper(); 
		this.data = data;
		if (charsetWrp != null)
			httpMessage.setContentCharset(charsetWrp.getCharset()); // if data == null
	}
	
	public void commitChanges() {
		super.commitChanges();
	}
	
	@Override
	public void ceaseData(byte[] data) {
		// called by base services implementations
		remainsStore.ceaseData(data);
	}
	
	public <T, Service extends ProxyService> void ceaseContent(Service byService, T chunkPart) {
		ceaseContentInternal(byService, chunkPart);
	};
	
	@SuppressWarnings("unchecked")
	public <T, Service extends ProxyService> void ceaseContentInternal(Service byService, T chunkPart) {
		/*if (moduleExecutingProvide == null) {
			log.debug("Attempt to cease data outside of provideService() method, ignoring");
			throw new IllegalStateException("Calling ceaseContent() is allowed from provideService() method only");
		}*/
		ServiceBinding<Service> svcContainer = getBindingForService(byService);
		ChunkServiceProvider<T, ProxyService> svcProvider = null;
		try {
			svcProvider = (ChunkServiceProvider<T, ProxyService>) svcContainer.realization.provider;
		} catch (ClassCastException e) {
			throw new ServiceUnavailableException(svcContainer.svcInfo.serviceClass,
					"Unable to pass chunkPart to target service because of illegal casting", e);
		}
		if (svcProvider != null) {
			// TODO statistics maybe
			svcProvider.ceaseContent(chunkPart, this);
		}
	};
	
	public <T> void setRemains(Object key, T remains) {
		remainsStore.setRemains(key, remains);
	};
	
	@Override
	public <T> T getRemains(Object key) {
		return remainsStore.getRemains(key);
	}
	
	@Override
	public Charset getCharset() throws UnsupportedCharsetException, IOException {
		return httpMessage.getCharsetWrapper().getCharset();
		// it's OK to throw NPE when returned CharsetWrapper is null since calling
		// this method in that case is illegal
	}
	
	abstract <Service extends ProxyService> void callDoChanges(ChunkServiceProvider<?, Service> svcProvider);
	
	@SuppressWarnings("unchecked")
	@Override
	<Service extends ProxyService> void callDoChanges(ServiceProvider<Service> svcProvider) {
		ChunkServiceProvider<?, Service> chunkSvcProvider = (ChunkServiceProvider<?, Service>) svcProvider;
		callDoChanges(chunkSvcProvider);
	}
	
	@Override
	HeaderWrapper getHeaderForPatternMatch() {
		return httpMessage.originalMessage().getHeader();
	}
	
	@Override
	String getText4Logging(LogText type) {
		if (type == LogText.TYPE)
			return "chunk";
		return null;
	}
}
