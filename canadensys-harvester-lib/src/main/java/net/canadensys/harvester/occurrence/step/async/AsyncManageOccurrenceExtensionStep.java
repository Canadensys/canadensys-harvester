package net.canadensys.harvester.occurrence.step.async;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.harvester.ItemWriterIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * No processor for now but it will come
 * @author cgendreau
 *
 */
public class AsyncManageOccurrenceExtensionStep extends GenericAsyncStep<OccurrenceExtensionModel> {
	
	//@Autowired
	//private JMSControlProducer errorReporter;

	public AsyncManageOccurrenceExtensionStep() {
		super(OccurrenceExtensionModel.class);
	}
	
	@Autowired
	@Qualifier("occurrenceExtensionWriter")
	public void setWriter(ItemWriterIF<OccurrenceExtensionModel> writer) {
		super.setWriter(writer);
	}

}
