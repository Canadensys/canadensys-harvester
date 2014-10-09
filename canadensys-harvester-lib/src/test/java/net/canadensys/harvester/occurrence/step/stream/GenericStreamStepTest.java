package net.canadensys.harvester.occurrence.step.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.canadensys.harvester.mapper.DefaultBeanMapper;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.DefaultMessage;
import net.canadensys.harvester.occurrence.mock.MockHabitObject;
import net.canadensys.harvester.occurrence.mock.writer.MockObjectWriter;
import net.canadensys.harvester.occurrence.reader.DwcaExtensionReader;
import net.canadensys.harvester.occurrence.step.async.GenericAsyncStep;

import org.junit.Test;

/**
 * Unit test for GenericStreamStep
 * @author cgendreau
 *
 */
public class GenericStreamStepTest {
	
	@Test
	public void testGenericStreamStep(){
		GenericStreamStep<MockHabitObject> streamHabitStep = new GenericStreamStep<MockHabitObject>();
		MockObjectWriter<ProcessingMessageIF> writer = new MockObjectWriter<ProcessingMessageIF>();
		Map<SharedParameterEnum,Object> sharedParameters = new HashMap<SharedParameterEnum, Object>();
		sharedParameters.put(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-vascan-checklist");
		sharedParameters.put(SharedParameterEnum.DWCA_EXTENSION_TYPE,"description");
		sharedParameters.put(SharedParameterEnum.RESOURCE_UUID,"1");
		
		//setup reader
		DwcaExtensionReader<MockHabitObject> extReader = new DwcaExtensionReader<MockHabitObject>();
		extReader.setMapper(new DefaultBeanMapper<MockHabitObject>(MockHabitObject.class));
		
		streamHabitStep.addAsyncReceiverStep(GenericAsyncStep.class);
		streamHabitStep.setReader(extReader);
		streamHabitStep.setWriter(writer);
		
		//run the step
		streamHabitStep.preStep(sharedParameters);
		streamHabitStep.doStep();
		streamHabitStep.postStep();
		
		//get the message that would have been wrote to JMS
		List<ProcessingMessageIF> messages = writer.getContent();
		DefaultMessage firstMessage = (DefaultMessage)messages.get(0);
		
		//ensure class are preserved
		assertEquals(GenericAsyncStep.class.getName(),firstMessage.getMsgHandlerClass().getName());
		assertEquals(ArrayList.class.getName(),firstMessage.getContentClass().getName());
		assertEquals(MockHabitObject.class,(((List<?>)firstMessage.getContent()).get(0)).getClass());
		
		//ensure that we can cast the content into the specified class
		try{
			firstMessage.getContentClass().cast(firstMessage.getContent());
		}
		catch(ClassCastException ccEx){
			fail("Can not cast the message object into the declared class");
		}
	}
}
