package cc.braids.app.ixos.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class AndroidEmulation {
	private static AndroidEmulation defaultInstance = new AndroidEmulation(Locale.getDefault());

	private Resources stringResources;
	
	/** Returns the resources as if in Android OS. */
	public static AndroidEmulation getResources() {
		return defaultInstance;
	}
	
	
	public AndroidEmulation(Locale locale)  
	{
		stringResources = new Resources();
		
		try (InputStream inStream = 
				getClass().getResourceAsStream("/values/strings.xml"))
		{
			stringResources.loadFrom(inStream);
		}
		catch (IOException exn) {
			throw new RuntimeException(exn);
		}
		catch (ParserConfigurationException exn) {
			throw new RuntimeException(exn);
		}
		catch (SAXException exn) {
			throw new RuntimeException(exn);
		}
	}


	public String getString(String string) {
		return stringResources.get(string);
	}
	
}
