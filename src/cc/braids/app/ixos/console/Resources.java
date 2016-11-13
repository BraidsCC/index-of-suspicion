package cc.braids.app.ixos.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


public class Resources {
	private HashMap<String,String> map = new HashMap<>();

	public String get(String key) {
		return map.get(key);
	}

	public void loadFrom(InputStream inStream) 
			throws ParserConfigurationException, SAXException, IOException 
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(inStream);

		doc.getDocumentElement().normalize();

		NodeList nodeList = doc.getElementsByTagName("string");

		for (int nodeIx = 0; nodeIx < nodeList.getLength(); nodeIx++) {

			Node node = nodeList.item(nodeIx);

			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element element = (Element) node;
				
				String key = element.getAttribute("name");
				String value = node.getTextContent();

				map.put(key, value);
			}
		}
		
	}
}
