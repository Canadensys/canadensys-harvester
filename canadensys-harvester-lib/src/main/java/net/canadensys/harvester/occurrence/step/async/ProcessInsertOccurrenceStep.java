package net.canadensys.harvester.occurrence.step.async;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;
import net.canadensys.harvester.jms.control.JMSControlProducer;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.message.control.NodeErrorControlMessage;
import net.canadensys.harvester.model.BulkDataObject;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.ProcessOccurrenceMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step taking a ProcessOccurrenceMessage from JMS message, process a Occurrence Raw object list, writing the result as well as the origin data (OccurrenceRaw).
 * NOT thread safe
 * @author canadensys
 *
 */
public class ProcessInsertOccurrenceStep extends AbstractReceiverStep implements JMSConsumerMessageHandlerIF{
	private static final Logger LOGGER = Logger.getLogger(ProcessInsertOccurrenceStep.class);
	
	@Autowired
	@Qualifier("occurrenceProcessor")
	private ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> processor;
	
	@Autowired
	@Qualifier("occurrenceWriter")
	private ItemWriterIF<OccurrenceModel> writer;
	
	@Autowired
	@Qualifier("rawOccurrenceWriter")
	private ItemWriterIF<OccurrenceRawModel> rawWriter;
	
	@Autowired
	private JMSControlProducer errorReporter;
	
	@Override
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException {
		if(writer == null || rawWriter == null){
			throw new IllegalStateException("No writer defined");
		}
		if(processor == null){
			throw new IllegalStateException("No processor defined");
		}
		if(errorReporter == null){
			throw new IllegalStateException("No errorReporter defined");
		}
		writer.openWriter();
		rawWriter.openWriter();
		errorReporter.open();
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		rawWriter.closeWriter();
		errorReporter.close();
	}
	
	@Override
	public Class<?> getMessageClass() {
		return ProcessOccurrenceMessage.class;
	}

	@Override
	public boolean handleMessage(ProcessingMessageIF message) {
		BulkDataObject<OccurrenceRawModel> bulkDataObject = ((ProcessOccurrenceMessage)message).getBulkRawModel();
		int numberOfData = bulkDataObject.getData().size();
				
		List<OccurrenceModel> occList = new ArrayList<OccurrenceModel>(numberOfData);
		List<OccurrenceRawModel> occRawList = new ArrayList<OccurrenceRawModel>(numberOfData);
		OccurrenceRawModel extractedRawModel = null;
		for(int idx=0;idx<numberOfData;idx++){
			extractedRawModel = bulkDataObject.retrieveObject(idx, new OccurrenceRawModel());
			occRawList.add(extractedRawModel);
			occList.add(processor.process(extractedRawModel, null));
		}
		try {
			rawWriter.write(occRawList);
			writer.write(occList);
		} catch (WriterException e) {
			errorReporter.publish(new NodeErrorControlMessage(e));
			return false;
		}
		return true;
	}
	
	public void setProcessor(ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> processor){
		this.processor = processor;
	}
	
	public void setWriter(ItemWriterIF<OccurrenceModel> writer){
		this.writer = writer;
	}
	
	@Override
	public String getTitle() {
		return "Inserting and processing occurrence data";
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}
