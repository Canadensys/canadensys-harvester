package net.canadensys.harvester.occurrence.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.ProcessingStepIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;
import net.canadensys.harvester.jms.control.JMSControlProducer;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.message.control.NodeErrorControlMessage;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.ProcessOccurrenceMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step taking a ProcessOccurrenceMessage from JMS message, process a Occurrence Raw object list, writing the result.
 * NOT thread safe
 * @author canadensys
 *
 */
public class ProcessInsertOccurrenceStep implements ProcessingStepIF,JMSConsumerMessageHandlerIF{
	private static final Logger LOGGER = Logger.getLogger(ProcessInsertOccurrenceStep.class);
	
	@Autowired
	@Qualifier("occurrenceProcessor")
	private ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> processor;
	
	@Autowired
	@Qualifier("occurrenceWriter")
	private ItemWriterIF<OccurrenceModel> writer;
	
	@Autowired
	private JMSControlProducer errorReporter;
	
	@Override
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException {
		if(writer == null){
			throw new IllegalStateException("No writer defined");
		}
		if(processor == null){
			throw new IllegalStateException("No processor defined");
		}
		if(errorReporter == null){
			throw new IllegalStateException("No errorReporter defined");
		}
		writer.openWriter();
		errorReporter.open();
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		errorReporter.close();
	}
	
	/**
	 * No implemented, async step
	 */
	@Override
	public void doStep() {};
	
	@Override
	public Class<?> getMessageClass() {
		return ProcessOccurrenceMessage.class;
	}

	@Override
	public boolean handleMessage(ProcessingMessageIF message) {
		List<OccurrenceRawModel> occRawList = ((ProcessOccurrenceMessage)message).getRawModelList();
		List<OccurrenceModel> occList = new ArrayList<OccurrenceModel>();
		
		for(OccurrenceRawModel currRawModel : occRawList){
			occList.add(processor.process(currRawModel, null));
		}
		try {
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
}
