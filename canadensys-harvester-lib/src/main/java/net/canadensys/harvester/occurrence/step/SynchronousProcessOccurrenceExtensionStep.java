package net.canadensys.harvester.occurrence.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.StepResult;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

/**
 * Read, process and write all occurrence extension(s) data from an archive.
 * @author cgendreau
 *
 */
public class SynchronousProcessOccurrenceExtensionStep implements StepIF {
	
	private static final int DEFAULT_FLUSH_INTERVAL = 250;
	
	@Autowired
	private ApplicationContext appContext;
	
	@Autowired
	@Qualifier("dwcaExtensionInfoReader")
	private ItemReaderIF<String> dwcaInfoReader;
	
	@Autowired
	@Qualifier("extLineProcessor")
	private ItemProcessorIF<OccurrenceExtensionModel, OccurrenceExtensionModel> extLineProcessor;
	
	@Autowired
	@Qualifier("occurrenceExtensionWriter")
	private ItemWriterIF<OccurrenceExtensionModel> writer;

	private Map<SharedParameterEnum,Object> sharedParameters;

	@Override
	public void preStep(Map<SharedParameterEnum, Object> sharedParameters)
			throws IllegalStateException {
		if(dwcaInfoReader == null){
			throw new IllegalStateException("No Dwc-A info reader defined");
		}
		if(extLineProcessor == null){
			throw new IllegalStateException("No line processor defined");
		}
		if(writer == null){
			throw new IllegalStateException("No writer defined");
		}
		this.sharedParameters = sharedParameters;
		
		dwcaInfoReader.openReader(sharedParameters);
		writer.openWriter();
		extLineProcessor.init();
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		dwcaInfoReader.closeReader();
		extLineProcessor.destroy();
	}

	@Override
	public StepResult doStep() {
		List<OccurrenceExtensionModel> occExtList = new ArrayList<OccurrenceExtensionModel>(DEFAULT_FLUSH_INTERVAL);
		int numberOfRecords = 0;
		
		String currExtension = dwcaInfoReader.read();
		while(currExtension != null){
			Term extTerm = TermFactory.instance().findTerm(currExtension);
			//TODO if there is more than one extension maybe trigger on thread per extension?
			//create a reader
			ItemReaderIF<OccurrenceExtensionModel> extReader = (ItemReaderIF<OccurrenceExtensionModel>)appContext.getBean("dwcaOccurrenceExtensionReader");
			
			//tricky part, shallow copy(not a deep copy) sharedParameters to indicate each readers which extension to use
			//this is probably not the best way to achieve that
			Map<SharedParameterEnum, Object> innerSharedParameters = new HashMap<SharedParameterEnum, Object>(sharedParameters);
			
			innerSharedParameters.put(SharedParameterEnum.DWCA_EXTENSION_TYPE, extTerm.simpleName());
			extReader.openReader(innerSharedParameters);
			
			try {
				OccurrenceExtensionModel currExtModel = extReader.read();
				while(currExtModel != null){
					currExtModel = extLineProcessor.process(currExtModel, innerSharedParameters);
					occExtList.add(currExtModel);
					
					numberOfRecords++;
					
					if(numberOfRecords % DEFAULT_FLUSH_INTERVAL == 0){
						writer.write(occExtList);
						occExtList.clear();
					}
					currExtModel = extReader.read();
				}
				//flush remaining content
				if(!occExtList.isEmpty()){
					writer.write(occExtList);
					occExtList.clear();
				}
			}
			catch(WriterException we){
				//noop, writer will write to the log
			}
			
			extReader.closeReader();
			currExtension = dwcaInfoReader.read();
		}
		return new StepResult(numberOfRecords);
	}
	
	@Override
	public String getTitle() {
		return "SynchronousProcessOccurrenceExtensionStep";
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}
