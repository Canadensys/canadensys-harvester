package net.canadensys.harvester.occurrence.step.async;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.canadensys.harvester.occurrence.message.DefaultMessage;
import net.canadensys.harvester.occurrence.mock.MockHabitObject;
import net.canadensys.harvester.occurrence.mock.writer.MockObjectWriter;

import org.junit.Test;

/**
 * Unit test for GenericAsyncStep
 * 
 * @author canadensys
 *
 */
public class GenericAsyncStepTest {
	
	@Test
	public void testGenericAsyncStepWithList(){
		GenericAsyncStep<MockHabitObject> asyncStep = new GenericAsyncStep<MockHabitObject>(MockHabitObject.class);
		//create a mock writer
		MockObjectWriter<MockHabitObject> writer = new MockObjectWriter<MockHabitObject>();
		asyncStep.setWriter(writer);
		
		//Build the object
		List<MockHabitObject> mhoList = new ArrayList<MockHabitObject>();
		MockHabitObject mho = new MockHabitObject();
		mho.setId("1");
		mho.setDescription("description");
		mhoList.add(mho);
		
		//Build mock DefaultMessage
		DefaultMessage dmsg = new DefaultMessage();
		dmsg.setMsgHandlerClass(GenericAsyncStep.class);
		dmsg.setContent(mhoList);
		dmsg.setContentClass(mhoList.getClass());
		
		asyncStep.preStep(null);
		asyncStep.handleMessage(dmsg);
		asyncStep.postStep();
		
		//The list of object that would have been written to the database
		List<MockHabitObject> objList = writer.getContent();
		System.out.println(objList.get(0).getClass());
		MockHabitObject firstObj = objList.get(0);
		
		//ensure class name are preserved
		assertEquals(firstObj.getId(), mho.getId());
		assertEquals(firstObj.getDescription(), mho.getDescription());
	}
	
	@Test
	public void testGenericAsyncStepWithoutList(){
		GenericAsyncStep<MockHabitObject> asyncStep = new GenericAsyncStep<MockHabitObject>(MockHabitObject.class);
		//create a mock writer
		MockObjectWriter<MockHabitObject> writer = new MockObjectWriter<MockHabitObject>();
		asyncStep.setWriter(writer);
		
		//Build the object
		MockHabitObject mho = new MockHabitObject();
		mho.setId("1");
		mho.setDescription("description");

		//Build mock DefaultMessage
		DefaultMessage dmsg = new DefaultMessage();
		dmsg.setMsgHandlerClass(GenericAsyncStep.class);
		dmsg.setContent(mho);
		dmsg.setContentClass(mho.getClass());
		
		asyncStep.preStep(null);
		asyncStep.handleMessage(dmsg);
		asyncStep.postStep();
		
		//The list of object that would have been written to the database
		List<MockHabitObject> objList = writer.getContent();
		System.out.println(objList.get(0).getClass());
		MockHabitObject firstObj = objList.get(0);
		
		//ensure class name are preserved
		assertEquals(firstObj.getId(), mho.getId());
		assertEquals(firstObj.getDescription(), mho.getDescription());
	}
}
