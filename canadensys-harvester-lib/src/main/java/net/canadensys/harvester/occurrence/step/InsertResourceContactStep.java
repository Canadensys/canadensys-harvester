package net.canadensys.harvester.occurrence.step;

import java.util.Map;

import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.ProcessingStepIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;
import net.canadensys.harvester.jms.control.JMSControlProducer;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.message.control.NodeErrorControlMessage;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.SaveResourceContactMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step taking a SaveResourceContactMessage from JMS and writing a ResourceContactModel to a writer
 * NOT thread safe
 * @author canadensys
 *
 */
public class InsertResourceContactStep implements ProcessingStepIF,JMSConsumerMessageHandlerIF{

	@Autowired
	@Qualifier("resourceContactWriter")
	private ItemWriterIF<ResourceContactModel> writer;
	
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
		return SaveResourceContactMessage.class;
	}

	@Override
	public boolean handleMessage(ProcessingMessageIF message) {
		long t = System.currentTimeMillis();
		ResourceContactModel rcm = ((SaveResourceContactMessage)message).getResourceContactModel();
		try {
			writer.write(rcm);
		} catch (WriterException e) {
			errorReporter.publish(new NodeErrorControlMessage(e));
			return false;
		}
		System.out.println("Reading msg + Writing Resource Contact :" + ( System.currentTimeMillis()-t) + "ms");
		return true;
	}
	
	/**
	 * No implemented, async step
	 */
	@Override
	public void doStep() {};
	
	public void setWriter(ItemWriterIF<ResourceContactModel> writer){
		this.writer = writer;
	}
	
	@Override
	public String getTitle() {
		return "Inserting resource contact data";
	}
}
