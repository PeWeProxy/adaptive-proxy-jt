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
import sk.fiit.peweproxy.utils.Statistics.PluginStats;
import sk.fiit.peweproxy.utils.Statistics.ProcessStats;
import sk.fiit.peweproxy.utils.Statistics.ProcessType;

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
	
	private String formatStats(ProcessStats stats) {
		StringBuilder sb = new StringBuilder("<td align=\"center\">");
		if (stats == null) {
			sb.append("-</td>");
			return sb.toString();
		}
		sb.append(Math.round(stats.getAverage()));
		sb.append(" ms<br><span style=\"font-size:10px\">");
		sb.append(stats.getCount());
		sb.append(" #, min:");
		sb.append(stats.getMin());
		sb.append(", max ");
		sb.append(stats.getMax());
		sb.append("</span></td>");
		return sb.toString();
	}
	
	private void addPluginsPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append("<p><h2>Loaded proxy plugins</h2></p>\n");
		sb.append(HtmlPage.getTableHeader (100, 1));
		sb.append(HtmlPage.getTableTopicRow ());
		sb.append("<th rowspan=\"2\" width=\"20%\">Plugin name</th>");
		sb.append("<th rowspan=\"2\" width=\"49%\">Plugin class</th>");
		sb.append("<th colspan=\"7\" width=\"21%\">Plugin types</th>\n");
		sb.append("<th colspan=\"2\" width=\"10%\">Times</th></tr>\n");
		sb.append(HtmlPage.getTableTopicRow ());
		sb.append("<th width=\"3%\" title=\"");
		sb.append(RequestProcessingPlugin.class.getSimpleName());
		sb.append("\">P&uarr;</th><th width=\"3%\" title=\"");
		sb.append(ResponseProcessingPlugin.class.getSimpleName());
		sb.append("\">P&darr;</th>");
		sb.append("<th width=\"3%\" title=\"");
		sb.append(RequestServiceModule.class.getSimpleName());
		sb.append("\">M&uarr;</th><th width=\"3%\" title=\"");
		sb.append(ResponseServiceModule.class.getSimpleName());
		sb.append("\">M&darr;</th>");
		sb.append("<th width=\"3%\" title=\"");
		sb.append(ConnectionEventPlugin.class.getSimpleName());
		sb.append("\">C</th><th width=\"3%\" title=\"");
		sb.append(TimeoutEventPlugin.class.getSimpleName());
		sb.append("\">T</th><th width=\"3%\" title=\"");
		sb.append(FailureEventPlugin.class.getSimpleName());
		sb.append("\">F</th>");
		sb.append("<th width=\"5%\" title=\"calls to start()\">start()</th><th width=\"5%\" title=\"calls to start()\">stop()</th></tr>\n");
		for (PluginInstance plgInstance : plugins) {
			PluginStats plgStats = adaptiveEngine.getStatistics().getPluginsStatistics(plgInstance.getInstance());
			sb.append ("<tr><td>");
			sb.append(plgInstance.getName());
			sb.append ("</td>\n<td>");
			sb.append(plgInstance.getPluginClass().getName());
			/*sb.append ("</td>\n<td>\n");
			for (Class<?> pluginType : plgInstance.getTypes()) {
				sb.append(pluginType.getSimpleName());
				sb.append("<br>\n");
			}*/
			sb.append("</td>");
			Set<Class< ? extends ProxyPlugin>> types = plgInstance.getTypes();
			sb.append(addIsTypeCell(types, RequestProcessingPlugin.class));
			sb.append(addIsTypeCell(types, ResponseProcessingPlugin.class));
			sb.append(addIsTypeCell(types, RequestServiceModule.class));
			sb.append(addIsTypeCell(types, ResponseServiceModule.class));
			sb.append(addIsTypeCell(types, ConnectionEventPlugin.class));
			sb.append(addIsTypeCell(types, TimeoutEventPlugin.class));
			sb.append(addIsTypeCell(types, FailureEventPlugin.class));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.PLUGIN_START)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.PLUGIN_STOP)));
			sb.append ("</tr>\n");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private String addIsTypeCell(Set<Class< ? extends ProxyPlugin>> types, Class<? extends ProxyPlugin> type) {
		if (types.contains(type))
			return "<td align=\"center\" title=\"is a "+type.getSimpleName()+"\"><b>X</b></td>";
		else
			return "<td align=\"center\" title=\"is not a "+type.getSimpleName()+"\">&nbsp</td>";
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
	
	private void addProcessingPluginsPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Processing plugins summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th rowspan=\"2\" width=\"55%\">Plugin name</th>");
		sb.append ("<th colspan=\"5\" width=\"25%\">Requests processing times</th>\n");
		sb.append ("<th colspan=\"4\" width=\"20%\">Responses processing times</th></tr>\n");
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"5%\" title=\"calls to desiredRequestServices()\">DS</th>");
		sb.append ("<th width=\"5%\" title=\"calls to processRequest()\">RTP</th>");
		sb.append ("<th width=\"5%\" title=\"calls to processTransferedRequest(HttpRequest)()\">LTP</th>");
		sb.append ("<th width=\"5%\" title=\"calls to getNewRequest()\">GRQ</th>");
		sb.append ("<th width=\"5%\" title=\"calls to getResponse()\">GRP</th>");
		sb.append ("<th width=\"5%\" title=\"calls to desiredResponseServices()\">DS</th>");
		sb.append ("<th width=\"5%\" title=\"calls to processResponse()\">RLP</th>");
		sb.append ("<th width=\"5%\" title=\"calls to processTransferedResponse()\">LTP</th>");
		sb.append ("<th width=\"5%\" title=\"calls to getNewResponse()\">GRP</th></tr>\n");
		for (PluginInstance plgInstance : plugins) {
			sb.append ("<tr><td>");
			sb.append(plgInstance.getName());
			sb.append ("</td>");
			PluginStats plgStats = adaptiveEngine.getStatistics().getPluginsStatistics(plgInstance.getInstance());
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_DESIRED_SERVICES)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_PROCESSING)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_LATE_PROCESSING)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_CONSTRUCTION)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_CONSTRUCTION_REPONSE)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_DESIRED_SERVICES)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_PROCESSING)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_LATE_PROCESSING)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_CONSTRUCTION)));
			sb.append ("</tr>\n");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addModulesPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Service modules summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th rowspan=\"2\" width=\"20%\">Plugin name</th>");
		sb.append ("<th rowspan=\"2\" width=\"40%\">Provided services</th>\n");
		sb.append ("<th colspan=\"4\" width=\"20%\">Requests processing times</th>\n");
		sb.append ("<th colspan=\"4\" width=\"20%\">Responses processing times</th></tr>\n");
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"5%\" title=\"calls to desiredRequestServices()\">DS</th>");
		sb.append ("<th width=\"5%\" title=\"calls to provideRequestService()\">PVS</th>");
		sb.append ("<th width=\"5%\" title=\"calls to doChanges()\">DCH</th>");
		sb.append ("<th width=\"5%\" title=\"calls to service methods\">SM</th>");
		sb.append ("<th width=\"5%\" title=\"calls to desiredResponseServices()\">DS</th>");
		sb.append ("<th width=\"5%\" title=\"calls to provideResponseService()\">PVS</th>");
		sb.append ("<th width=\"5%\" title=\"calls to doChanges()\">DCH</th>");
		sb.append ("<th width=\"5%\" title=\"calls to service methods\">SM</th></tr>\n");
		for (PluginInstance plgInstance : plugins) {
			sb.append ("<tr><td>");
			sb.append(plgInstance.getName());
			sb.append ("</td>\n<td>\n");
			sb.append ("<b>Services for requests:</b><br>\n");
			Set<Class< ? extends ProxyPlugin>> types = plgInstance.getTypes();
			PluginStats plgStats = adaptiveEngine.getStatistics().getPluginsStatistics(plgInstance.getInstance());
			if (types.contains(RequestServiceModule.class)) {
				for (Class<? extends ProxyService> svcClass : adaptiveEngine.getModulesManager().getProvidedRequestServices((RequestServiceModule)plgInstance.getInstance())) {
					sb.append(svcClass.getName());
					sb.append("<br>\n");
				}
			}
			sb.append ("<b>Services for responses:</b><br>\n");
			if (types.contains(ResponseServiceModule.class)) {
				for (Class<? extends ProxyService> svcClass : adaptiveEngine.getModulesManager().getProvidedResponseServices((ResponseServiceModule)plgInstance.getInstance())) {
					sb.append(svcClass.getName());
					sb.append("<br>\n");
				}
			}
			sb.append ("</td>\n");
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_DESIRED_SERVICES)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_PROVIDE_SERVICE)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_SERVICE_COMMIT)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.REQUEST_SERVICE_METHOD)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_DESIRED_SERVICES)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_PROVIDE_SERVICE)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_SERVICE_COMMIT)));
			sb.append(formatStats(plgStats.getProcessStats(ProcessType.RESPONSE_SERVICE_METHOD)));
			sb.append ("</tr>\n");
		}
		sb.append ("</table>\n<br>\n");
	}
	
	private void addEventPluginsPart(List<PluginInstance> plugins, StringBuilder sb) {
		sb.append ("<p><h2>Event plugins summary</h2></p>\n");
		sb.append (HtmlPage.getTableHeader (100, 1));
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th rowspan=\"2\" width=\"70%\">Plugin name</th>");
		sb.append ("<th colspan=\"6\" width=\"30%\">Times</th>\n");
		sb.append (HtmlPage.getTableTopicRow ());
		sb.append ("<th width=\"5%\" title=\"calls to clientMadeConnection()\">C-M</th>");
		sb.append ("<th width=\"5%\" title=\"calls to clientClosedConnection() and proxyClosedConnection()\">C-C</th>");
		sb.append ("<th width=\"5%\" title=\"calls to requestReadTimeout() and responseReadTimeout()\">T-R</th>");
		sb.append ("<th width=\"5%\" title=\"calls to requestDeliveryTimeout() and responseDeliveryTimeout()\">T-D</th>");
		sb.append ("<th width=\"5%\" title=\"calls to requestReadFailed() and responseReadFailed()\">F-R</th>");
		sb.append ("<th width=\"5%\" title=\"calls to requestDeliveryFailed() and responseDeliveryFailed()\">F-D</th></tr>\n");
		synchronized (pluginHandler) {
			for (PluginInstance plgInstance : plugins) {
				sb.append ("<tr><td>");
				sb.append(plgInstance.getName());
				sb.append ("</td>\n");
				PluginStats plgStats = adaptiveEngine.getStatistics().getPluginsStatistics(plgInstance.getInstance());
				sb.append(formatStats(plgStats.getProcessStats(ProcessType.CONNECTION_CREATE)));
				sb.append(formatStats(plgStats.getProcessStats(ProcessType.CONNECTION_CLOSED)));
				sb.append(formatStats(plgStats.getProcessStats(ProcessType.READ_TIMEOUT)));
				sb.append(formatStats(plgStats.getProcessStats(ProcessType.DELIVERY_TIMEOUT)));
				sb.append(formatStats(plgStats.getProcessStats(ProcessType.READ_FAIL)));
				sb.append(formatStats(plgStats.getProcessStats(ProcessType.DELIVERY_FAIL)));
				sb.append ("</tr>\n");
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
	
	private String logStyle;
	
	private String colorLogLine(String lineText) {
		//13:52:48,797 INFO   - Can not read variables file ...
		if (lineText.matches("\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d.*")) {
			String logLvl = lineText.substring(13, lineText.indexOf(' ', 13));
			if ("TRACE".equals(logLvl))
				logStyle = "grey";
			else if ("DEBUG".equals(logLvl))
				logStyle = "green";
			else if ("INFO".equals(logLvl))
				logStyle = "blue";
			else if ("WARN".equals(logLvl))
				logStyle = "red; font-weight:bold";
			else if ("ERROR".equals(logLvl))
				logStyle = "purple; font-weight:bold";
			else if ("FATAL".equals(logLvl))
				logStyle = "black; font-weight:bold";
		}
		lineText = lineText.replaceAll(" ", "&nbsp;");
		lineText = "<code style=\"color:" + logStyle + ";\">" + lineText + "</code>";
		return lineText;
	}
	@Override
	protected String getPageHeader() {
		return "AdaptiveProxy status page";
	}

}
