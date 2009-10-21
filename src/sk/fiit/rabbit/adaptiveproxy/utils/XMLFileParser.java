package sk.fiit.rabbit.adaptiveproxy.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class XMLFileParser {
	private static final Logger log = Logger.getLogger(XMLFileParser.class);
	private static final DocumentBuilder builder;
	
	static {
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			String msg = "No XML document builder available";
			log.error(msg, e);
			throw new RuntimeException(msg);
		}
	}
	
	public static Document parseFile(File xmlFile) {
		try {
			return builder.parse(xmlFile);
		} catch (SAXException e) {
			log.warn("Error during parsing of config file "+xmlFile.getAbsolutePath(),e);
		} catch (IOException e) {
			log.warn("Error during reading of config file "+xmlFile.getAbsolutePath(),e);
		}
		return null;
	}
}