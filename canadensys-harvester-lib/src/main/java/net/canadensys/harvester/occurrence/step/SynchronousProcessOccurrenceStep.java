package net.canadensys.harvester.occurrence.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.StepResult;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.model.JobStatusModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * This step allows to read from a DarwinCore archive, process the data, insert the processed data and the raw data to a writer.
 * This step is synchronous, it will block until the whole archive is written to the writer. May not be suitable for large Dwc-A.
 * 
 * @author cgendreau
 *
 */
public class SynchronousProcessOccurrenceStep implements StepIF {
	
	private static final int DEFAULT_FLUSH_INTERVAL = 250;
	
	@Autowired
	@Qualifier("dwcItemReader")
	private ItemReaderIF<OccurrenceRawModel> reader;
	
	@Autowired
	@Qualifier("lineProcessor")
	private ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor;
	
	@Autowired
	@Qualifier("occurrenceProcessor")
	private ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> processor;
	
	@Autowired
	@Qualifier("occurrenceWriter")
	private ItemWriterIF<OccurrenceModel> writer;
	
	@Autowired
	@Qualifier("rawOccurrenceWriter")
	private ItemWriterIF<OccurrenceRawModel> rawWriter;
	
	private Map<SharedParameterEnum,Object> sharedParameters;

	@Override
	public String getTitle() {
		return "SynchronousProcessOccurrenceStep";
	}

	@Override
	public void preStep(Map<SharedParameterEnum, Object> sharedParameters)
			throws IllegalStateException {
		if(reader == null){
			throw new IllegalStateException("No reader defined");
		}
		if(writer == null || rawWriter == null){
			throw new IllegalStateException("No writer defined");
		}
		if(processor == null || lineProcessor == null){
			throw new IllegalStateException("No processor defined");
		}

		this.sharedParameters = sharedParameters;
		
		writer.openWriter();
		rawWriter.openWriter();
		
		lineProcessor.init();
		processor.init();
		
		reader.openReader(sharedParameters);
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		rawWriter.closeWriter();
		
		reader.closeReader();
		
		lineProcessor.destroy();
		processor.destroy();
	}

	@Override
	public StepResult doStep() {
		int numberOfRecords = 0;
		List<OccurrenceModel> occList = new ArrayList<OccurrenceModel>(DEFAULT_FLUSH_INTERVAL);
		List<OccurrenceRawModel> occRawList = new ArrayList<OccurrenceRawModel>(DEFAULT_FLUSH_INTERVAL);
		
		JobStatusModel jobStatus = (JobStatusModel)sharedParameters.get(SharedParameterEnum.JOB_STATUS_MODEL);
		
		try {
			OccurrenceRawModel currRawModel = reader.read();
			while(currRawModel != null){
				currRawModel = lineProcessor.process(currRawModel, sharedParameters);
				
				occRawList.add(currRawModel);
				occList.add(processor.process(currRawModel, sharedParameters));
	
				currRawModel = reader.read();
				numberOfRecords++;
				
				if(numberOfRecords % DEFAULT_FLUSH_INTERVAL == 0){
					rawWriter.write(occRawList);
					writer.write(occList);
					
					occRawList.clear();
					occList.clear();
					jobStatus.setCurrentJobProgress(numberOfRecords + " records");
				}
			}
			//flush remaining content
			if(occList.size() > 0){
				rawWriter.write(occRawList);
				writer.write(occList);
				
				occRawList.clear();
				occList.clear();
				jobStatus.setCurrentJobProgress(numberOfRecords + " records");
			}
		}
		catch(WriterException wEx){
			wEx.printStackTrace();
		}
		return new StepResult(numberOfRecords);
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

}
