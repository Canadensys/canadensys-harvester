package net.canadensys.harvester.occurrence.step.stream;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.ProcessingStepIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.DefaultMessage;

import org.apache.log4j.Logger;

/**
 * Generic step to stream user defined object (defined by T).
 * This class will stream DefaultMessage objects with ArrayList<T> as content.
 * 
 * @author canadensys
 *
 * @param <T>
 */
public class GenericStreamStep<T> implements ProcessingStepIF{

	private static final Logger LOGGER = Logger.getLogger(GenericStreamStep.class);
	private static final int DEFAULT_FLUSH_INTERVAL = 100;
	
	private ItemReaderIF<T> reader;
	private ItemWriterIF<ProcessingMessageIF> writer;
	private ItemProcessorIF<T, T> lineProcessor;
	
	//Flush interval, number of OccurrenceRawModel until we flush it (into a JMS message)
	private int flushInterval = DEFAULT_FLUSH_INTERVAL;

	private int numberOfRecords = 0;
	private Map<SharedParameterEnum,Object> sharedParameters;
	
	//List of message handlers that will received the streamed messages
	private List<Class< ? extends JMSConsumerMessageHandlerIF>> targetedMsgHandlerList;
	
	private String stepTitle = "Streaming data using GenericStreamStep";
	
	@Override
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException {
		if(writer == null){
			throw new IllegalStateException("No writer defined");
		}
		if(reader == null){
			throw new IllegalStateException("No reader defined");
		}
		if( targetedMsgHandlerList == null || targetedMsgHandlerList.size() <= 0){
			throw new IllegalStateException("No targeted message handler");
		}
		
		this.sharedParameters = sharedParameters;
		reader.openReader(sharedParameters);
		writer.openWriter();
		
		if(lineProcessor != null){
			lineProcessor.init();
		}
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		reader.closeReader();
		
		if(lineProcessor != null){
			lineProcessor.destroy();
		}
	}

	@Override
	public void doStep() {
		long t= System.currentTimeMillis();
		List<T> objList = new ArrayList<T>();
		
		T currObject = reader.read();
		while(currObject != null){
			if(lineProcessor != null){
				currObject = lineProcessor.process(currObject, sharedParameters);
			}
			
			objList.add(currObject);
			numberOfRecords++;
			if(numberOfRecords % flushInterval == 0){
				writeObjects(objList);
				objList.clear();
			}
			currObject = reader.read();
		}
		//flush remaining content
		if(objList.size() > 0){
			writeObjects(objList);
		}
		System.out.println("Streaming the file took :" + (System.currentTimeMillis()-t) + " ms");
		sharedParameters.put(SharedParameterEnum.NUMBER_OF_RECORDS,numberOfRecords);
	}
	
	/**
	 * Create one message per defined message class
	 * @param obj
	 */
	private void writeObjects(List<T> objList){
		try{
			for(Class<?> currClass : targetedMsgHandlerList){
				DefaultMessage dmsg = new DefaultMessage();
				dmsg.setTimestamp(Calendar.getInstance().getTime().toString());
				dmsg.setMsgHandlerClass(currClass);
				dmsg.setContent(objList);
				dmsg.setContentClass(objList.getClass());
				dmsg.setContentClassGeneric(objList.get(0).getClass());
				writer.write(dmsg);
			}
		}
		catch(WriterException e){
			LOGGER.fatal(e);
		}
	}
	
	public int getNumberOfRecords(){
		return numberOfRecords;
	}
	
	public void setReader(ItemReaderIF<T> reader) {
		this.reader = reader;
	}
	public void setWriter(ItemWriterIF<ProcessingMessageIF> writer) {
		this.writer = writer;
	}
	public void setDwcaLineProcessor(
			ItemProcessorIF<T, T> lineProcessor) {
		this.lineProcessor = lineProcessor;
	}
	
	public List<Class<? extends JMSConsumerMessageHandlerIF>> targetedStepList() {
		return targetedMsgHandlerList;
	}
	public void setMessageClasses(
			List<Class<? extends JMSConsumerMessageHandlerIF>> targetedMsgHandlerList) {
		this.targetedMsgHandlerList = targetedMsgHandlerList;
	}
	
	public void setTitle(String stepTitle) {
		this.stepTitle=stepTitle;
	}
	@Override
	public String getTitle() {
		return stepTitle;
	}
}
