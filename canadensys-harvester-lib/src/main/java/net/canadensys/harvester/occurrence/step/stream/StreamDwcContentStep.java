package net.canadensys.harvester.occurrence.step.stream;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepResult;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.ProcessOccurrenceMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step reading a DarwinCore line, process the line, writing the processed lines at a fixed interval as ProcessingMessageIF.
 * NOT thread safe
 * @author canadensys
 *
 */
public class StreamDwcContentStep extends AbstractStreamStep {
	
	private static final Logger LOGGER = Logger.getLogger(StreamDwcContentStep.class);
	private static final int DEFAULT_FLUSH_INTERVAL = 250;
	
	//Fields from OccurrenceRawModel that are not DarwinCore fields but should be included in messages.
	private static List<String> NON_DWC_FIELD_USED = new ArrayList<String>();
	static{
		NON_DWC_FIELD_USED.add("auto_id");
		NON_DWC_FIELD_USED.add("dwcaid");
		NON_DWC_FIELD_USED.add("sourcefileid");
	}
	
	@Autowired
	@Qualifier("dwcItemReader")
	private ItemReaderIF<OccurrenceRawModel> reader;
	
	@Autowired
	@Qualifier("jmsWriter")
	private ItemWriterIF<ProcessingMessageIF> writer;
	
	@Autowired
	@Qualifier("lineProcessor")
	private ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor;
	
	private Map<SharedParameterEnum,Object> sharedParameters;
	
	//Flush interval, number of OccurrenceRawModel until we flush it (into a JMS message)
	private int flushInterval = DEFAULT_FLUSH_INTERVAL;
	
	private List<String> usedFields;
	
	@SuppressWarnings("unchecked")
	@Override
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException {
		if(writer == null){
			throw new IllegalStateException("No writer defined");
		}
		if(lineProcessor == null){
			throw new IllegalStateException("No processor defined");
		}
		if(reader == null){
			throw new IllegalStateException("No reader defined");
		}
		this.sharedParameters = sharedParameters;
		
		//the reader should fill DWCA_USED_TERMS
		reader.openReader(sharedParameters);
		if(sharedParameters.get(SharedParameterEnum.DWCA_USED_TERMS) == null){
			throw new IllegalStateException("sharedParameters doesn't contained DwcA used terms");
		}
		//copy the list because we want to add elements to it
		usedFields = new ArrayList<String>((List<String>)sharedParameters.get(SharedParameterEnum.DWCA_USED_TERMS));
		usedFields.addAll(NON_DWC_FIELD_USED);
		
		writer.openWriter();
		lineProcessor.init();
	}

	@Override
	public void postStep() {
		writer.closeWriter();
		lineProcessor.destroy();
		reader.closeReader();
	}

	@Override
	public StepResult doStep() {
		int numberOfRecords = 0;
		try{
			ProcessOccurrenceMessage occMsg = new ProcessOccurrenceMessage(usedFields);
			
			long t= System.currentTimeMillis();
			OccurrenceRawModel currRawModel = reader.read();
			while(currRawModel != null){
				currRawModel = lineProcessor.process(currRawModel, sharedParameters);
	
				//should be done by ChunkSplitter
				occMsg.addRawModel(currRawModel);
				occMsg.setWhen(Calendar.getInstance().getTime().toString());
				
				currRawModel = reader.read();
				numberOfRecords++;
				
				if(numberOfRecords % flushInterval == 0){
					writer.write(occMsg);
					
					occMsg = new ProcessOccurrenceMessage(usedFields);
					occMsg.setWhen(Calendar.getInstance().getTime().toString());
				}
			}
			//flush remaining content
			if(occMsg.getBulkRawModel().getData().size() > 0){
				writer.write(occMsg);
			}
			System.out.println("Streaming the file took :" + (System.currentTimeMillis()-t) + " ms");
		}
		catch(WriterException e){
			LOGGER.fatal(e);
		}
		return new StepResult(numberOfRecords);
	}
	
	public void setReader(ItemReaderIF<OccurrenceRawModel> reader) {
		this.reader = reader;
	}
	
	public void setWriter(ItemWriterIF<ProcessingMessageIF> writer) {
		this.writer = writer;
	}
	
	public void setDwcaLineProcessor(
			ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor) {
		this.lineProcessor = lineProcessor;
	}
	
	public void setFlushInterval(int flushInterval){
		this.flushInterval = flushInterval;
	}

	@Override
	public String getTitle() {
		return "Streaming DwcA content";
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
	
}
