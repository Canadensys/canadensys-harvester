package net.canadensys.harvester.occurrence.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.canadensys.harvester.AbstractProcessingJob;
import net.canadensys.harvester.ItemMapperIF;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.jms.JMSConsumer;
import net.canadensys.harvester.jms.JMSWriter;
import net.canadensys.harvester.mapper.DefaultBeanMapper;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.mock.MockHabitObject;
import net.canadensys.harvester.occurrence.mock.MockProcessedHabitObject;
import net.canadensys.harvester.occurrence.mock.processor.MockHabitProcessor;
import net.canadensys.harvester.occurrence.mock.writer.MockObjectWriter;
import net.canadensys.harvester.occurrence.reader.DwcaExtensionReader;
import net.canadensys.harvester.occurrence.step.async.GenericAsyncProcessingStep;
import net.canadensys.harvester.occurrence.step.async.GenericAsyncStep;
import net.canadensys.harvester.occurrence.step.stream.AbstractStreamStep;
import net.canadensys.harvester.occurrence.step.stream.GenericStreamStep;

import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Test aiming to test a job create with and for user defined object.
 * MockHabitObject is used as the user defined object.
 * 
 * Sequence:
 * -Read data from a DarwinCore extension file
 * -Map it to MockHabitObject
 * -Stream the MockHabitObject on the Java Messaging System (JMS) to 2 different targets
 * -Create a node(mocked) that would receive the message through a GenericAsyncStep step
 * -Create another node(mocked) that would receive the message through a GenericAsyncProcessingStep step
 * -Rebuild the MockHabitObject from the received message (and process it in GenericAsyncProcessingStep)
 * -Write the objects
 * @author canadensys
 *
 */
public class UserDefinedJobTest{
	
	private static final String TEST_BROKER_URL = "vm://localhost?broker.persistent=false";
	private static AtomicBoolean step1Completed = new AtomicBoolean(false);
	private static AtomicBoolean step2Completed = new AtomicBoolean(false);
	
	//Job initiator related variables
	private InnerUserDefinedJob userDefinedJob;
	
	private DwcaExtensionReader<MockHabitObject> itemReader;
	private GenericStreamStep<MockHabitObject> streamStep;
	private JMSWriter jmsWriter;
	
	//Node related variables
	private GenericAsyncStep<MockHabitObject> asyncStep;
	private GenericAsyncProcessingStep<MockHabitObject,MockProcessedHabitObject> asyncStepWithProcessing;
	
	private JMSConsumer jmsReader;
	private MockObjectWriter<MockHabitObject> itemWriter;
	private MockObjectWriter<MockProcessedHabitObject> processedItemWriter;
	private ItemProcessorIF<MockHabitObject, MockProcessedHabitObject> itemProcessor;
	
	@Test
	public void testEntireLoop(){
		
		//Build a item reader
		itemReader = new DwcaExtensionReader<MockHabitObject>();
		
		//Build a mapper to control how DarwinCore properties are mapped to user defined object
		ItemMapperIF<MockHabitObject> itemMapper = new DefaultBeanMapper<MockHabitObject>(MockHabitObject.class);
		
		//Link the reader with our mapper
		itemReader.setMapper(itemMapper);
		
		//Create a JMS writer
		jmsWriter = new JMSWriter(TEST_BROKER_URL);
		
		//Create and configure the stream step
		streamStep = new GenericStreamStep<MockHabitObject>();
		streamStep.setReader(itemReader);
		streamStep.setWriter(jmsWriter);
		//Declare the message handler classes. Which class(es) on the node(s) should handle the messages.
		//When more than one class is provided, each classes will receive the message.
		streamStep.addAsyncReceiverStep(GenericAsyncStep.class);
		streamStep.addAsyncReceiverStep(GenericAsyncProcessingStep.class);

		//Create our job
		userDefinedJob = new InnerUserDefinedJob();
		
		//Add our step to the job
		userDefinedJob.setGenericStreamStep(streamStep);
		
		jmsReader = new JMSConsumer(TEST_BROKER_URL);
		
		//add a local consumers to test the entire loop
		setupMessageConsumerUsingGenericAsyncStep(2);
		setupMessageConsumerUsingGenericAsyncProcessingStep(2);
		
		//Start listening
		jmsReader.open();
						
		userDefinedJob.addToSharedParameters(SharedParameterEnum.DWCA_PATH,"src/test/resources/dwca-vascan-checklist");
		userDefinedJob.addToSharedParameters(SharedParameterEnum.DWCA_EXTENSION_TYPE,"description");

		userDefinedJob.doJob();
		
		waitAndValidateNodeResult();
	}
	
	/**
	 * This message consumer will write using a MockObjectWriter so we can get the written objects back.
	 */
	private void setupMessageConsumerUsingGenericAsyncStep(int numberOfRecord){
		
		//Create our async step that would run on a node
		asyncStep = new GenericAsyncStep<MockHabitObject>(MockHabitObject.class);
		
		//Create and set our writer
		itemWriter = new MockObjectWriter<MockHabitObject>();
		//This callback mechanism is for testing purpose only
		itemWriter.addCallback(new InnerCallback(step1Completed), numberOfRecord);
		asyncStep.setWriter(itemWriter);
		
		//Register our step as an handler (JMSConsumerMessageHandler)
		jmsReader.registerHandler(asyncStep);
		
		try {
			((StepIF)asyncStep).preStep(null);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This consumer will write using a MockObjectWriter so we can get the written objects back.
	 */
	private void setupMessageConsumerUsingGenericAsyncProcessingStep(int numberOfRecord){

		//Create our async step that would run on a node
		asyncStepWithProcessing = new GenericAsyncProcessingStep<MockHabitObject, MockProcessedHabitObject>(MockHabitObject.class);
		
		//Create and set our writer
		processedItemWriter = new MockObjectWriter<MockProcessedHabitObject>();
		itemProcessor = new MockHabitProcessor();
		
		//This callback mechanism is for testing purpose only
		processedItemWriter.addCallback(new InnerCallback(step2Completed), numberOfRecord);
		asyncStepWithProcessing.setWriter(processedItemWriter);
		asyncStepWithProcessing.setItemProcessor(itemProcessor);
		
		//Register our step as an handler (JMSConsumerMessageHandler)
		jmsReader.registerHandler(asyncStepWithProcessing);
		
		try {
			((StepIF)asyncStepWithProcessing).preStep(null);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wait after steps completion and validate the result.
	 */
	private void waitAndValidateNodeResult(){
		//wait and validate result of Step 1 (GenericAsyncStep)
		synchronized (step1Completed) {
			try {
				step1Completed.wait(10000);
				
				if(step1Completed.get()){
					List<MockHabitObject> objectWritten = itemWriter.getContent();
					assertEquals("1941",objectWritten.get(0).getId());
				}
				else{
					fail();
				}
			} catch (InterruptedException e) {
				fail();
			}
		}
		
		//wait and validate result of Step 2 (GenericAsyncProcessingStep)
		synchronized (step2Completed) {
			try {
				step2Completed.wait(10000);
				
				if(step2Completed.get()){
					List<MockProcessedHabitObject> objectWritten = processedItemWriter.getContent();
					assertEquals(1941,objectWritten.get(0).getId());
				}
				else{
					fail();
				}
			}
			catch (InterruptedException e) {
				fail();
			}
		}
		
		if(jmsReader != null){
			jmsReader.close();
		}
	}

	/**
	 * Inner class to handle callbacks with a synchronized object.
	 * @author canadensys
	 *
	 */
	private class InnerCallback implements FutureCallback<Void>{
		private AtomicBoolean notifyObject;
		public InnerCallback(AtomicBoolean notifyObject){
			this.notifyObject = notifyObject;
		}
		
		@Override
		public void onSuccess(Void result) {
			synchronized (notifyObject) {
				notifyObject.set(true);
				notifyObject.notifyAll();
			}
		}

		@Override
		public void onFailure(Throwable t) {
			fail();
		}
	}
	
	/**
	 * User defined job declared as inner class.
	 * @author canadensys
	 *
	 */
	private class InnerUserDefinedJob extends AbstractProcessingJob{
		private AbstractStreamStep genericStreamStep;
		
		public InnerUserDefinedJob(){
			sharedParameters = new HashMap<SharedParameterEnum, Object>();
		}
		
		public void setGenericStreamStep(AbstractStreamStep genericStreamStep) {
			this.genericStreamStep = genericStreamStep;
		}
		
		public void doJob(){
			genericStreamStep.preStep(sharedParameters);
			genericStreamStep.doStep();
			genericStreamStep.postStep();
		}

		@Override
		public void cancel() {}
	}
}
