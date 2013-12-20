package net.canadensys.processing.occurrence.reader;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.processing.ItemReaderIF;
import net.canadensys.processing.occurrence.SharedParameterEnum;

import org.junit.Test;

/**
 * Test the reading of a DarwinCore Archive file and get a object back.
 * Ensure the default values are read
 * @author canadensys
 *
 */
public class DwcaReaderTest {
	
	@Test
	public void testDwcaItemReader(){
		Map<SharedParameterEnum,Object> sharedParameters = new HashMap<SharedParameterEnum, Object>();
		sharedParameters.put(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-qmor-specimens");
		sharedParameters.put(SharedParameterEnum.DATASET_SHORTNAME,"qmor-specimens");
		
		ItemReaderIF<OccurrenceRawModel> dwcaItemReader = new DwcaItemReader();
		dwcaItemReader.openReader(sharedParameters);
		
		OccurrenceRawModel rawModel = dwcaItemReader.read();
		//ensure that we read default values
		assertEquals("PreservedSpecimen", rawModel.getBasisofrecord());
		assertEquals("Rigaud", rawModel.getMunicipality());
	}
}
