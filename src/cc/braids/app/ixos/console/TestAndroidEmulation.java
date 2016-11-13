package cc.braids.app.ixos.console;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cc.braids.util.junit.BTestCase;

public class TestAndroidEmulation extends BTestCase {

	public void setUp() {
	}
	
	public void tearDown() {
		
	}
	
	public final void test_stringResources() throws ParserConfigurationException, SAXException, IOException {
		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+"<resources>"
		+ "  <string name=\"key1\">value1</string>"
		+ "  <string name=\"key2\">value2</string>"
		+ "</resources>";

		byte[] xmlByteArray = xmlString.getBytes();

		ByteArrayInputStream inStream = new ByteArrayInputStream(xmlByteArray);

		Resources resources = new Resources();
		resources.loadFrom(inStream);
		
		assertEquals("key1", "value1", resources.get("key1"));
		assertEquals("key2", "value2", resources.get("key2"));
	}
	
}
