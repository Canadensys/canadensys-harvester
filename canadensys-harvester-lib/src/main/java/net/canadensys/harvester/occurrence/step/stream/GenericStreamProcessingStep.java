package net.canadensys.harvester.occurrence.step.stream;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepResult;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.DefaultMessage;
import net.canadensys.harvester.occurrence.step.async.AbstractReceiverStep;

import org.apache.log4j.Logger;

/**
 * Generic step to stream user defined object (defined by S).
 * Read object of type T, process in type S.
 * This class will stream DefaultMessage objects with ArrayList<S> as content.
 * TODO too similar to GenericStreamStep<T>.
 * 
 * @author canadensys
 *
 * @param <T,S>
 */
public class GenericStreamProcessingStep<T,S> extends AbstractStreamStep {

	private static final Logger LOGGER = Logger.getLogger(GenericStreamProcessingStep.class);
	private static final int DEFAULT_FLUSH_INTERVAL = 100;
	
	private ItemReaderIF<T> reader;
	private ItemWriterIF<ProcessingMessageIF> writer;
	private ItemProcessorIF<T, S> lineProcessor;
	
	//Flush interval, number of OccurrenceRawModel until we flush it (into a JMS message)
	private int flushInterval = DEFAULT_FLUSH_INTERVAL;

	private Map<SharedParameterEnum,Object> sharedParameters;
	
	private String stepTitle = "Streaming data using GenericStreamStep";
	
	@Override
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException {
		if(writer == null){
			throw new IllegalStateException("No writer defined");
		}
		if(reader == null){
			throw new IllegalStateException("No reader defined");
		}
		if(lineProcessor == null){
			throw new IllegalStateException("No lineProcessor defined");
		}
		if( asyncReceivers == null || asyncReceivers.isEmpty()){
			throw new IllegalStateException("No targeted AbstractAsyncReceiverStep");
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

	public StepResult doStep() {
		int numberOfRecords = 0;
		long t= System.currentTimeMillis();
		List<S> objList = new ArrayList<S>();
		
		T readObject = reader.read();
		S processedObject;
		while(readObject != null){

			processedObject = lineProcessor.process(readObject, sharedParameters);

			objList.add(processedObject);
			numberOfRecords++;
			if(numberOfRecords % flushInterval == 0){
				writeObjects(objList);
				objList.clear();
			}
			readObject = reader.read();
		}
		//flush remaining content
		if(objList.size() > 0){
			writeObjects(objList);
		}
		System.out.println("Streaming the file took :" + (System.currentTimeMillis()-t) + " ms");
		return new StepResult(numberOfRecords);
	}
	
	/**
	 * Create one message per defined message class
	 * @param obj
	 */
	private void writeObjects(List<S> objList){
		try{
			for(Class<? extends AbstractReceiverStep> currAsyncReceiver : asyncReceivers){
				DefaultMessage dmsg = new DefaultMessage();
				dmsg.setTimestamp(Calendar.getInstance().getTime().toString());
				dmsg.setMsgHandlerClass(currAsyncReceiver);
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
	
	public void setReader(ItemReaderIF<T> reader) {
		this.reader = reader;
	}
	public void setWriter(ItemWriterIF<ProcessingMessageIF> writer) {
		this.writer = writer;
	}
	public void setDwcaLineProcessor(
			ItemProcessorIF<T, S> lineProcessor) {
		this.lineProcessor = lineProcessor;
	}
	
	public void setTitle(String stepTitle) {
		this.stepTitle=stepTitle;
	}
	@Override
	public String getTitle() {
		return stepTitle;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}
