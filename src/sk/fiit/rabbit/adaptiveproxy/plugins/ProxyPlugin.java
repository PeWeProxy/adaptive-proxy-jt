package sk.fiit.rabbit.adaptiveproxy.plugins;

public interface ProxyPlugin {
	boolean supportsReconfigure(PluginProperties newProps);
	
	boolean start(PluginProperties props);
	
	void stop();
}
