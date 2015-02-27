package net.canadensys.harvester.occurrence.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

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
		sharedParameters.put(SharedParameterEnum.SOURCE_FILE_ID,"qmor-specimens");
		
		ItemReaderIF<OccurrenceRawModel> dwcaItemReader = new DwcaItemReader();
		dwcaItemReader.openReader(sharedParameters);
		
		OccurrenceRawModel rawModel = dwcaItemReader.read();
		//ensure that we read default values
		assertEquals("PreservedSpecimen", rawModel.getBasisofrecord());
		assertEquals("Rigaud", rawModel.getMunicipality());
	}
	
	/**
	 * Test an archive where the id column is also a field (e.g. catalogNumber).
	 */
	@Test
	public void testDwcaItemReaderIdColumn(){
		Map<SharedParameterEnum,Object> sharedParameters = new HashMap<SharedParameterEnum, Object>();
		sharedParameters.put(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-qmor-specimens-id");
		sharedParameters.put(SharedParameterEnum.SOURCE_FILE_ID,"qmor-specimens");
		
		ItemReaderIF<OccurrenceRawModel> dwcaItemReader = new DwcaItemReader();
		dwcaItemReader.openReader(sharedParameters);
		
		OccurrenceRawModel rawModel = dwcaItemReader.read();
		//ensure that we read default values
		assertEquals("1", rawModel.getCatalognumber());
		assertEquals("Gomphus", rawModel.getGenus());
	}
	
	@Test
	public void testDwcaItemReaderAbort(){
		Map<SharedParameterEnum,Object> sharedParameters = new HashMap<SharedParameterEnum, Object>();
		sharedParameters.put(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-qmor-specimens");
		sharedParameters.put(SharedParameterEnum.SOURCE_FILE_ID,"qmor-specimens");
		
		ItemReaderIF<OccurrenceRawModel> dwcaItemReader = new DwcaItemReader();
		dwcaItemReader.openReader(sharedParameters);
		
		OccurrenceRawModel rawModel = dwcaItemReader.read();
		assertNotNull(rawModel);
		//cancel the reader
		dwcaItemReader.abort();
		rawModel = dwcaItemReader.read();
		assertNull(rawModel);
	}
}
