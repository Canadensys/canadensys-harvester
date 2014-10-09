package net.canadensys.harvester.occurrence.step.stream;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.step.async.AsyncManageOccurrenceExtensionStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Concrete implementation of GenericStreamProcessingStep for OccurrenceExtensionModel.
 * 
 * @author cgendreau
 *
 */
public class StreamDwcExtensionContentStep extends GenericStreamProcessingStep<OccurrenceExtensionModel,OccurrenceExtensionModel> {
	
	public StreamDwcExtensionContentStep() {
		//set concrete class who will handle the messages sent by this class
		addAsyncReceiverStep(AsyncManageOccurrenceExtensionStep.class);
	}

	/**
	 * dwcaOccurrenceExtensionReader should be declared as a prototype.
	 */
	@Override
	@Qualifier("dwcaOccurrenceExtensionReader")
	@Autowired
	public void setReader(ItemReaderIF<OccurrenceExtensionModel> reader) {
		super.setReader(reader);
	}
	
	@Override
	@Qualifier("jmsWriter")
	@Autowired
	public void setWriter(ItemWriterIF<ProcessingMessageIF> writer) {
		super.setWriter(writer);
	}
	
	@Override
	@Qualifier("extLineProcessor")
	@Autowired
	public void setDwcaLineProcessor(
			ItemProcessorIF<OccurrenceExtensionModel, OccurrenceExtensionModel> lineProcessor) {
		super.setDwcaLineProcessor(lineProcessor);
	}
	
}
