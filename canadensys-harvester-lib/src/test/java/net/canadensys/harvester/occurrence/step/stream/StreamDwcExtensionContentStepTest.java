package net.canadensys.harvester.occurrence.step.stream;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.config.ProcessingConfigTest;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.mapper.OccurrenceExtensionMapper;
import net.canadensys.harvester.occurrence.message.DefaultMessage;
import net.canadensys.harvester.occurrence.mock.writer.MockMessageWriter;
import net.canadensys.harvester.occurrence.reader.DwcaExtensionReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Test StreamDwcExtensionContentStep
 * @author cgendreau
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=ProcessingConfigTest.class, loader=AnnotationConfigContextLoader.class)
public class StreamDwcExtensionContentStepTest {
	
	@Autowired
	@Qualifier("extLineProcessor")
	private ItemProcessorIF<OccurrenceExtensionModel, OccurrenceExtensionModel> extLineProcessor;
	
	@Test
	public void testStreamDwcExtensionContentStep(){
		StreamDwcExtensionContentStep streamExtStep = new StreamDwcExtensionContentStep();
		
		MockMessageWriter<ProcessingMessageIF> mockMessageWriter = new MockMessageWriter<ProcessingMessageIF>();
		DwcaExtensionReader<OccurrenceExtensionModel> extReader = new DwcaExtensionReader<OccurrenceExtensionModel>();
		extReader.setMapper(new OccurrenceExtensionMapper());
		
		//create a mock writer
		streamExtStep.setWriter(mockMessageWriter);
		streamExtStep.setReader(extReader);
		streamExtStep.setDwcaLineProcessor(extLineProcessor);
		
		Map<SharedParameterEnum,Object> sharedParameters = new HashMap<SharedParameterEnum, Object>();
		sharedParameters.put(SharedParameterEnum.SOURCE_FILE_ID, "dwca-vascan-checklist");
		sharedParameters.put(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-vascan-checklist");
		sharedParameters.put(SharedParameterEnum.DWCA_EXTENSION_TYPE,"description");
		sharedParameters.put(SharedParameterEnum.RESOURCE_UUID,"1");
		
		streamExtStep.preStep(sharedParameters);
		streamExtStep.doStep();
		streamExtStep.postStep();
		
		//The list of object that would have been written to the database
		List<ProcessingMessageIF> objList = mockMessageWriter.getContent();
		DefaultMessage firstObj = (DefaultMessage)objList.get(0);
		
		assertEquals(OccurrenceExtensionModel.class, ((ArrayList<?>)firstObj.getContent()).get(0).getClass());
	}

}
