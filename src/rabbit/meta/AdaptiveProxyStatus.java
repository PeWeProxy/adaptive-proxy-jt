package rabbit.meta;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import rabbit.http.HttpHeader;
import rabbit.proxy.Connection;
import rabbit.proxy.HtmlPage;
import rabbit.util.SProperties;
import rabbit.util.TrafficLogger;
import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.plugins.PluginHandler;
import sk.fiit.peweproxy.plugins.PluginHandler.PluginInstance;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.plugins.events.ConnectionEventPlugin;
import sk.fiit.peweproxy.plugins.events.FailureEventPlugin;
import sk.fiit.peweproxy.plugins.events.TimeoutEventPlugin;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.services.ProxyService;

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

	
	@SuppressWarnings("unchecked")
	@Override
	protected PageCompletion addPageInformation(StringBuilder sb) {
		List<PluginInstance> plugins = pluginHandler.getAllPlugins();
		addPluginsPart(plugins,sb);
		
		sb.append("<table width=\"100%\">\n<tr>\n<td align=\"center\">");
		sb.append("<form action=\"");
		if (!reloadURL)
			sb.append("AdaptiveProxyStatus/reload");
		sb.append("\" method=\"get\">\n<input type=\"submit\" value=\"Reload plugins\"/>\n</form><br>\n");
		sb.append("</td>\n</tr>\n</table>");

		addProcessingPluginsPart(filterPlugins(plugins, RequestProcessingPlugin.class,ResponseProcessingPlugin.class),sb);
		addModulesPart(filterPlugins(plugins, RequestServiceModule.class,ResponseServiceModule.class),sb);
		addEventPluginsPart(filterPlugins(plugins, ConnectionEventPlugin.class,FailureEventPlugin.class,TimeoutEventPlugin.class),sb);
		addLogPart(sb);
		return PageCompletion.PAGE_DONE;
	}
	
	private void addPluginsPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Loaded proxy plugins</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"20%\">Plugin name</th>");
		sb.append ("<th width=\"60%\">Plugin class</th>");
		sb.append ("<th width=\"20%\">Plugin types</th>\n");
		for (PluginInstance plgInstance : plugins) {
			sb.append ("<tr><td>");
			sb.append(plgInstance.getName());
			sb.append ("</td>\n<td>");
			sb.append(plgInstance.getPluginClass().getName());
			sb.append ("</td>\n<td>\n");
			for (Class<?> pluginType : plgInstance.getTypes()) {
				sb.append(pluginType.getSimpleName());
				sb.append("<br>\n");
			}
			sb.append ("</td></tr>\n");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private List<PluginInstance> filterPlugins(List<PluginInstance> list, Class<? extends ProxyPlugin> ... types) {
		List<PluginInstance> retVal = new LinkedList<PluginInstance>();
		for (PluginInstance plgInstance : list) {
			Set<Class<? extends ProxyPlugin>> plgTypes = plgInstance.getTypes();
			for (Class<? extends ProxyPlugin> plgType : types) {
				if (plgTypes.contains(plgType)) {
					retVal.add(plgInstance);
					break;
				}
			}
		}
		return retVal;
	}
	
	private void addModulesPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Service modules summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"20%\">Plugin name</th>");
		sb.append ("<th width=\"70%\">Provided services</th>\n");
		sb.append ("<th width=\"5%\">RQ</th>\n");
		sb.append ("<th width=\"5%\">RP</th>\n");
		for (PluginInstance plgInstance : plugins) {
			sb.append ("<tr><td>");
			sb.append(plgInstance.getName());
			sb.append ("</td>\n<td>\n");
			sb.append ("<b>Services for requests:</b><br>\n");
			boolean rq = (plgInstance.getTypes().contains(RequestServiceModule.class));
			boolean rp = (plgInstance.getTypes().contains(ResponseServiceModule.class));
			if (rq) {
				for (Class<? extends ProxyService> svcClass : adaptiveEngine.getModulesManager().getProvidedRequestServices((RequestServiceModule)plgInstance.getInstance())) {
					sb.append(svcClass.getName());
					sb.append("<br>\n");
				}
			}
			sb.append ("<b>Services for responses:</b><br>\n");
			if (rp) {
				for (Class<? extends ProxyService> svcClass : adaptiveEngine.getModulesManager().getProvidedResponseServices((ResponseServiceModule)plgInstance.getInstance())) {
					sb.append(svcClass.getName());
					sb.append("<br>\n");
				}
			}
			sb.append ("</td>\n<td align=\"center\">\n");
			if (rq)
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td>\n<td align=\"center\">\n");
			if (rp)
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td></tr>");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addProcessingPluginsPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Processing plugins summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"90%\">Plugin name</th>");
		sb.append ("<th width=\"5%\">RQ</th>\n");
		sb.append ("<th width=\"5%\">RP</th>\n");
		for (PluginInstance plgInstance : plugins) {
			sb.append ("<tr><td>");
			sb.append(plgInstance.getName());
			sb.append ("</td>\n<td align=\"center\">\n");
			if (plgInstance.getTypes().contains(RequestProcessingPlugin.class))
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td>\n<td align=\"center\">\n");
			if (plgInstance.getTypes().contains(ResponseProcessingPlugin.class))
				sb.append ("<b>X</b>");
			else
				sb.append ("&nbsp");
			sb.append ("</td></tr>");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addEventPluginsPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Event plugins summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"70%\">Plugin name</th>");
		sb.append ("<th width=\"10%\">Connection</th>\n");
		sb.append ("<th width=\"10%\">Timeout</th>\n");
		sb.append ("<th width=\"10%\">Failure</th>\n");
		synchronized (pluginHandler) {
			for (PluginInstance plgInstance : plugins) {
				sb.append ("<tr><td>");
				sb.append(plgInstance.getName());
				sb.append ("</td>\n<td align=\"center\">\n");
				if (plgInstance.getTypes().contains(ConnectionEventPlugin.class))
					sb.append ("<b>X</b>");
				else
					sb.append ("&nbsp");
				sb.append ("</td>\n<td align=\"center\">\n");
				if (plgInstance.getTypes().contains(TimeoutEventPlugin.class))
					sb.append ("<b>X</b>");
				else
					sb.append ("&nbsp");
				sb.append ("</td>\n<td align=\"center\">\n");
				if (plgInstance.getTypes().contains(FailureEventPlugin.class))
					sb.append ("<b>X</b>");
				else
					sb.append ("&nbsp");
				sb.append ("</td></tr>");
			}
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addLogPart(StringBuilder sb) {
		sb.append("<p><h2>Plugins loading log</h2></p>\n");
		//sb.append("<table border=\"1\" style=\"max-width:100%;\">\n");
		//sb.append("<tr>\n<td>\n");
		sb.append("<div height=\"200px\" width=\"100%\" style=\"overflow:scroll; white-space:nowrap; border-style:solid;\">\n");
		Scanner sc = new Scanner(pluginHandler.getLoadingLogText());
		while (sc.hasNextLine())
			sb.append(colorLogLine(sc.nextLine())+"<br>\n");
		sb.append("</div>\n");
		//sb.append("</td>\n</tr>\n</table>\n");
	}
	
	private String colorLogLine(String lineText) {
		//13:52:48,797 INFO   - Can not read variables file ...
		String logLvl = lineText.substring(13, lineText.indexOf(' ', 13));
		String style = "black";
		if ("TRACE".equals(logLvl))
			style = "yellow";
		else if ("DEBUG".equals(logLvl))
			style = "green";
		else if ("INFO".equals(logLvl))
			style = "blue";
		else if ("WARN".equals(logLvl))
			style = "red; font-weight:bold";
		else if ("ERROR".equals(logLvl))
			style = "purple; font-weight:bold";
		else if ("FATAL".equals(logLvl))
			style = "black; font-weight:bold";
		lineText = lineText.replaceAll(" ", "&nbsp;");
		lineText = "<code style=\"color:" + style + ";\">" + lineText + "</code>";
		return lineText;
	}
	@Override
	protected String getPageHeader() {
		return "AdaptiveProxy status page";
	}

}
