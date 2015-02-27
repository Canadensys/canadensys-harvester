package net.canadensys.harvester.occurrence.mapper;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;

import org.junit.Test;

/**
 * Test the mapping from a map of properties to an OccurrenceExtensionModel instance.
 * @author cgendreau
 *
 */
public class OccurrenceExtensionMapperTest {
	
	@Test
	public void testOccurrenceExtensionMapper(){

		OccurrenceExtensionMapper occurrenceExtensionMapper = new OccurrenceExtensionMapper();
		
		Map<String,Object> properties = new HashMap<String, Object>();
		properties.put("id", "i18");
		properties.put("weather", "sunny");
		OccurrenceExtensionModel model = occurrenceExtensionMapper.mapElement(properties);
		
		assertEquals("i18", model.getDwcaid());
		assertEquals("sunny", model.getExt_data().get("weather"));
	}
}
