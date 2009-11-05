package rabbit.meta;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rabbit.http.HttpHeader;
import rabbit.proxy.Connection;
import rabbit.proxy.HtmlPage;
import rabbit.util.SProperties;
import rabbit.util.TrafficLogger;
import sk.fiit.rabbit.adaptiveproxy.AdaptiveEngine;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginHandler;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.events.CloseEventPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.events.FailureEventPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.events.TimeoutEventPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicePlugin;

public class AdaptiveProxyStatus extends BaseMetaHandler {
	AdaptiveEngine adaptiveEngine = null;
	PluginHandler pluginHandler = null;
	boolean reloadURL = false;
	
	@Override
	public void handle(HttpHeader request, SProperties htab, Connection con,
			TrafficLogger tlProxy, TrafficLogger tlClient) throws IOException {
		adaptiveEngine = con.getProxy().getAdaptiveEngine();
		pluginHandler = adaptiveEngine.getPluginHandler();
		if (htab.getProperty("argstring", "").endsWith("reload")) {
			reloadURL = true;
			adaptiveEngine.reloadPlugins();
		}
		super.handle(request, htab, con, tlProxy, tlClient);
	}

	
	@Override
	protected PageCompletion addPageInformation(StringBuilder sb) {
		addPluginsPart(sb);
		
		sb.append("<table width=\"100%\">\n<tr>\n<td align=\"center\">");
		sb.append("<form action=\"");
		if (!reloadURL)
			sb.append("AdaptiveProxyStatus/reload");
		sb.append("\" method=\"get\">\n<input type=\"submit\" value=\"Reload plugins\"/>\n</form><br>\n");
		sb.append("</td>\n</tr>\n</table>");

		addModulesPart(sb);
		addProcessingPluginsPart(sb);
		addEventPluginsPart(sb);
		return PageCompletion.PAGE_DONE;
	}
	
	private void addPluginsPart(StringBuilder sb) {
		sb.append ("<p><h2>Loaded proxy plugins</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"20%\">Plugin name</th>");
		sb.append ("<th width=\"60%\">Plugin class</th>");
		sb.append ("<th width=\"20%\">Plugin types</th>\n");
		List<String> pluginNames = pluginHandler.getLoadedPluginNames();
		for (String pluginName : pluginNames) {
			ProxyPlugin plugin = pluginHandler.getPlugin(pluginName, ProxyPlugin.class);
			sb.append ("<tr><td>");
			sb.append(pluginName);
			sb.append ("</td>\n<td>");
			sb.append(plugin.getClass().getName());
			sb.append ("</td>\n<td>\n");
			List<String> pluginTypes = pluginHandler.getTypesOfPlugin(plugin);
			for (String pluginType : pluginTypes) {
				sb.append(pluginType);
				sb.append("<br>\n");
			}
			sb.append ("</td></tr>\n");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addModulesPart(StringBuilder sb) {
		sb.append ("<p><h2>Modules summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"20%\">Plugin name</th>");
		sb.append ("<th width=\"70%\">Provided services</th>\n");
		sb.append ("<th width=\"5%\">RQ</th>\n");
		sb.append ("<th width=\"5%\">RP</th>\n");
		List<RequestServicePlugin> rqServicePlugins = RequestServiceHandleImpl.getLoadedModules();
		List<ResponseServicePlugin> rpServicePlugins = ResponseServiceHandleImpl.getLoadedModules();
		Set<ServicePlugin> loadedModules = new LinkedHashSet<ServicePlugin>();
		loadedModules.addAll(rqServicePlugins);
		loadedModules.addAll(rpServicePlugins);
		for (ServicePlugin plugin : loadedModules) {
			sb.append ("<tr><td>");
			sb.append(pluginHandler.getPluginName(plugin));
			sb.append ("</td>\n<td>\n");
			Set<Class<? extends ProxyService>> svcs = plugin.getProvidedServices();
			for (Class<? extends ProxyService> svcClass : svcs) {
				sb.append(svcClass.getName());
				sb.append("<br>\n");
			}
			sb.append ("</td>\n<td align=\"center\">\n");
			if (rqServicePlugins.contains(plugin))
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td>\n<td align=\"center\">\n");
			if (rpServicePlugins.contains(plugin))
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td></tr>");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addProcessingPluginsPart(StringBuilder sb) {
		sb.append ("<p><h2>Processing plugins summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"90%\">Plugin name</th>");
		sb.append ("<th width=\"5%\">RQ</th>\n");
		sb.append ("<th width=\"5%\">RP</th>\n");
		List<RequestProcessingPlugin> rqPlugins = adaptiveEngine.getLoadedRequestPlugins();
		List<ResponseProcessingPlugin> rpPlugins = adaptiveEngine.getLoadedResponsePlugins();
		Set<ProxyPlugin> loadedPlugins = new LinkedHashSet<ProxyPlugin>();
		loadedPlugins.addAll(rqPlugins);
		loadedPlugins.addAll(rpPlugins);
		for (ProxyPlugin plugin : loadedPlugins) {
			sb.append ("<tr><td>");
			sb.append(pluginHandler.getPluginName(plugin));
			sb.append ("</td>\n<td align=\"center\">\n");
			if (rqPlugins.contains(plugin))
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td>\n<td align=\"center\">\n");
			if (rpPlugins.contains(plugin))
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td></tr>");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addEventPluginsPart(StringBuilder sb) {
		sb.append ("<p><h2>Event plugins summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"70%\">Plugin name</th>");
		sb.append ("<th width=\"10%\">Close</th>\n");
		sb.append ("<th width=\"10%\">Timeout</th>\n");
		sb.append ("<th width=\"10%\">Failed</th>\n");
		List<CloseEventPlugin> cePlugins = adaptiveEngine.getEventsHandler().getLoadedCloseEventPlugins();
		List<TimeoutEventPlugin> toPlugins = adaptiveEngine.getEventsHandler().getLoadedTimeoutEventPlugins();
		List<FailureEventPlugin> flPlugins = adaptiveEngine.getEventsHandler().getLoadedFailureEventPlugins();
		Set<ProxyPlugin> loadedPlugins = new LinkedHashSet<ProxyPlugin>();
		loadedPlugins.addAll(cePlugins);
		loadedPlugins.addAll(toPlugins);
		loadedPlugins.addAll(flPlugins);
		synchronized (pluginHandler) {
			for (ProxyPlugin plugin : loadedPlugins) {
				sb.append ("<tr><td>");
				sb.append(pluginHandler.getPluginName(plugin));
				sb.append ("</td>\n<td align=\"center\">\n");
				if (cePlugins.contains(plugin))
					sb.append ("<b>X</b>");
				else
					sb.append ("&nbsp");
				sb.append ("</td>\n<td align=\"center\">\n");
				if (toPlugins.contains(plugin))
					sb.append ("<b>X</b>");
				else
					sb.append ("&nbsp");
				sb.append ("</td>\n<td align=\"center\">\n");
				if (flPlugins.contains(plugin))
					sb.append ("<b>X</b>");
				else
					sb.append ("&nbsp");
				sb.append ("</td></tr>");
			}
		}
		sb.append ("</table>\n<br>\n");
	}

	@Override
	protected String getPageHeader() {
		return "AdaptiveProxy status page";
	}

}
