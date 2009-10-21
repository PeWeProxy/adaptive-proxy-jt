package sk.fiit.rabbit.adaptiveproxy.plugins;

public interface ProxyPlugin {
	boolean setup(PluginProperties props);
	
	boolean supportsReconfigure();
	
	void start();
	
	void stop();
}
