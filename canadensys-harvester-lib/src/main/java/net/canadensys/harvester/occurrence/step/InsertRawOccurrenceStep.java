package net.canadensys.harvester.occurrence.step;

import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.ProcessingStepIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;
import net.canadensys.harvester.jms.control.JMSControlProducer;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.message.control.NodeErrorControlMessage;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.SaveRawOccurrenceMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step taking a SaveRawOccurrenceMessage from JMS and writing a Occurrence Raw object list to a writer
 * NOT thread safe
 * @author canadensys
 *
 */
public class InsertRawOccurrenceStep implements ProcessingStepIF,JMSConsumerMessageHandlerIF{
	
	@Autowired
	@Qualifier("rawOccurrenceWriter")
	private ItemWriterIF<OccurrenceRawModel> writer;
	
	@Autowired
	private JMSControlProducer errorReporter;

	@Override
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException{
		if(writer == null){
			throw new IllegalStateException("No writer defined");
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
	
	@Override
	public Class<?> getMessageClass() {
		return SaveRawOccurrenceMessage.class;
	}

	@Override
	public boolean handleMessage(ProcessingMessageIF message) {
		long t = System.currentTimeMillis();
		List<OccurrenceRawModel> occRawList = ((SaveRawOccurrenceMessage)message).getRawModelList();
		try {
			writer.write(occRawList);
		} catch (WriterException e) {
			errorReporter.publish(new NodeErrorControlMessage(e));
			return false;
		}
		System.out.println("Reading msg + Writing raw :" + ( System.currentTimeMillis()-t) + "ms");
		return true;
	}
	
	/**
	 * No implemented, async step
	 */
	@Override
	public void doStep() {};
	
	public void setWriter(ItemWriterIF<OccurrenceRawModel> writer){
		this.writer = writer;
	}
	
	@Override
	public String getTitle() {
		return "Inserting raw occurrence data";
	}
}
