package net.canadensys.harvester.occurrence.step;

import java.util.Calendar;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepResult;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.SaveResourceContactMessage;
import net.canadensys.harvester.occurrence.step.stream.AbstractStreamStep;

import org.gbif.metadata.eml.Eml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step reading an EML file from a DarwinCore archive, process it, writing the result as a ProcessingMessageIF.
 * NOT thread safe
 * @author canadensys
 *
 */
public class StreamEmlContentStep extends AbstractStreamStep {

	@Autowired
	@Qualifier("dwcaEmlReader")
	private ItemReaderIF<Eml> reader;
	
	@Autowired
	@Qualifier("jmsWriter")
	private ItemWriterIF<ProcessingMessageIF> writer;
	
	@Autowired
	@Qualifier("resourceContactProcessor")
	private ItemProcessorIF<Eml, ResourceContactModel> resourceContactProcessor;
	
	private Map<SharedParameterEnum,Object> sharedParameters;
	
	@Override
	public void preStep(Map<SharedParameterEnum, Object> sharedParameters){
		if(writer == null){
			throw new IllegalStateException("No writer defined");
		}
		if(resourceContactProcessor == null){
			throw new IllegalStateException("No processor defined");
		}
		if(reader == null){
			throw new IllegalStateException("No reader defined");
		}
		this.sharedParameters = sharedParameters;
		reader.openReader(sharedParameters);
		writer.openWriter();
		resourceContactProcessor.init();
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		resourceContactProcessor.destroy();
		reader.closeReader();
	}

	@Override
	public StepResult doStep() {
		//For now, we only read and stream the resource contact
		SaveResourceContactMessage srcm = new SaveResourceContactMessage();
		srcm.setWhen(Calendar.getInstance().getTime().toString());
		
		Eml emlModel = reader.read();
		ResourceContactModel resourceContactModel = resourceContactProcessor.process(emlModel, sharedParameters);
		
		srcm.setResourceContactModel(resourceContactModel);

		try {
			writer.write(srcm);
		} catch (WriterException e) {
			e.printStackTrace();
		}
		return new StepResult(1);
	}

	@Override
	public String getTitle() {
		return "Streaming EML";
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}
