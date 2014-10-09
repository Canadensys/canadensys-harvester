package net.canadensys.harvester.occurrence.step;

import java.util.Map;

import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.StepResult;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.gbif.metadata.eml.Eml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step reading an EML file from a DarwinCore archive, process it, writing the result.
 * NOT thread safe
 * @author canadensys
 *
 */
public class SynchronousProcessEmlContentStep implements StepIF {

	@Autowired
	@Qualifier("dwcaEmlReader")
	private ItemReaderIF<Eml> reader;
	
	@Autowired
	@Qualifier("resourceContactWriter")
	private ItemWriterIF<ResourceContactModel> writer;
	
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
		Eml emlModel = reader.read();
		ResourceContactModel resourceContactModel = resourceContactProcessor.process(emlModel, sharedParameters);
		
		try {
			writer.write(resourceContactModel);
		} catch (WriterException e) {
			e.printStackTrace();
		}
		return new StepResult(1);
	}

	@Override
	public String getTitle() {
		return "Process EML";
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}
