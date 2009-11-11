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
	private static final Logger log = Logger.getLogger(XMLFileParser.class.getName());
	private static final DocumentBuilder builder;
	
	static {
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			String msg = "No XML document builder available";
			log.warn(msg+"\n"+e.toString());
			throw new RuntimeException(msg);
		}
	}
	
	public static Document parseFile(File xmlFile) {
		try {
			return builder.parse(xmlFile);
		} catch (SAXException e) {
			log.info("Error during parsing of config file "+xmlFile.getAbsolutePath()+"\n"+e.toString());
		} catch (IOException e) {
			log.info("Error during reading of config file "+xmlFile.getAbsolutePath()+"\n"+e.toString());
		}
		return null;
	}
}
