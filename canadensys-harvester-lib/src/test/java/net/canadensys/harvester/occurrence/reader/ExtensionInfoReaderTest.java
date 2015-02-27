package net.canadensys.harvester.occurrence.reader;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.junit.Test;

/**
 * Test the reading of the name(rowType) of all extensions included in a DarwinCore Archive.
 *  
 * @author canadensys
 *
 */
public class ExtensionInfoReaderTest {
	
	@Test
	public void testDwcaExtensionInfoReader(){
		Map<SharedParameterEnum,Object> sharedParameters = new HashMap<SharedParameterEnum, Object>();
		sharedParameters.put(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-qmor-specimens");
		
		ItemReaderIF<String> extInfoReader = new DwcaExtensionInfoReader();
		extInfoReader.openReader(sharedParameters);
		
		String providedExtension = extInfoReader.read();
		assertEquals("http://rs.gbif.org/terms/1.0/Multimedia", providedExtension);
	}
}
