package sk.fiit.peweproxy.services.processing;

import java.util.Map;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.user.UserIdentificationService;

/**
 * Processing plugins toggling service provides a mechanism to enable or disable the processing
 * of the messages of a particular user by a particular processing plugin. Using this service,
 * other plugins can toggle whether the AdaptiveProxy platform will call particular request /
 * response processing plugin to process future request / response messages initiated by the
 * user that initiated the message this service is provided for.<br><br>
 * This service depends on the presence of the realization of the
 * {@link UserIdentificationService} service during message processing. When there's no module
 * that provide user identification service plugged in, or none of such modules is able to
 * identify the user that initiated the message, this service won't be available.
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the five final services and service plugins are not
 * allowed to provide implementations of it. Definition of this service and also
 * its implementation is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface PluginsTogglingService extends ProxyService {
	
	@readonly
	public Map<String, Boolean> getRequestPluginsStatus();
	
	@readonly
	public Map<String, Boolean> getResponsePluginsStatus();
	
	@readonly
	public void setRequestPluginsStatus(Map<String, Boolean> newStatuses);
	
	@readonly
	public void setResponsePluginsStatus(Map<String, Boolean> newStatuses);
	
}
