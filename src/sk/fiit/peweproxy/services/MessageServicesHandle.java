package sk.fiit.peweproxy.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.plugins.ProxyPlugin;

public abstract class MessageServicesHandle<ModuleType extends ProxyPlugin> extends
		ServicesHandleBase<ModuleType> {
	
	public MessageServicesHandle(HttpMessageImpl<?> httpMessage, List<ModuleType> modules, ModulesManager manager) {
		super(httpMessage, modules, manager);
	}
	
	@Override
	boolean dataAccessible() {
		return httpMessage.bodyAccessible();
	}

	@Override
	public boolean isReadOnly() {
		return httpMessage.isReadOnly();
	}
	
	void setReadOnly() {
		httpMessage.setReadOnly();
	}
	
	@Override
	public byte[] getData() {
		return httpMessage.getData();
	}
	
	@Override
	public void setData(byte[] data) {
		httpMessage.setData(data);
	}
	
	@Override
	String getText4Logging(LogText type) {
		if (type == LogText.TYPE)
			return "message";
		return null;
	}
	
	private <E> boolean overlapSets(Set<E> set1, Set<E> set2) {
		for (E element : set1) {
			if (set2.contains(element))
				return true;
		}
		return false;
	}
	
	public boolean needContent(Set<Class<? extends ProxyService>> desiredServices, boolean conChunking) {
		/*if (contentNeeded(desiredServices))
			return true;*/
		for (ListIterator<ModuleType> iterator = modules.listIterator(modules.size()); iterator.hasPrevious();) {
			ModuleType module = iterator.previous();
			if (overlapSets(desiredServices, getProvidedSvcs(module))) {
				Set<Class<? extends ProxyService>> plgDesiredSvcs = new HashSet<Class<? extends ProxyService>>();
				try {
					discoverDesiredServices(module,plgDesiredSvcs,conChunking);
				} catch (Throwable t) {
					log.info(getLogTextHead()+"Throwable raised while obtaining set of desired services from "
								+getLogTextCapital()+"ServiceModule of class '"+module.getClass()+"'",t);
				}
				desiredServices.addAll(plgDesiredSvcs);
				if (contentNeeded(desiredServices)) {
					if (log.isDebugEnabled())
						log.debug(getLogTextHead()+"Service module "+module+" wants "
								+"content modifying service for "+getLogTextNormal());
					return true;
				}
			}
		}
		return false;
	}
	
	abstract void discoverDesiredServices(ModuleType plugin,
			Set<Class<? extends ProxyService>> desiredServices, boolean conChunking) throws Throwable;
	
	@Override
	public void ceaseData(byte[] data) {
		throw new IllegalStateException("Calling ceaseContent() is illegal for full-message services handle");
	}
	
	@Override
	HeaderWrapper getHeaderForPatternMatch() {
		return httpMessage.getHeader();
	}
	
	@Override
	public Charset getCharset() throws UnsupportedCharsetException, IOException {
		return CharsetUtils.detectCharset(httpMessage.getHeader(), httpMessage.getData(), false);
	}
}
