package sk.fiit.peweproxy.plugins;

public class PluginConfigurationException extends Exception {
	private static final long serialVersionUID = 7440127413239028146L;
	private final String text;

	public PluginConfigurationException(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
}
