package net.canadensys.harvester.occurrence.step;

import java.util.Calendar;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.ProcessingStepIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.message.ProcessOccurrenceMessage;
import net.canadensys.harvester.occurrence.message.SaveRawOccurrenceMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Step reading a DarwinCore line, process the line, writing the processed lines at a fixed interval as ProcessingMessageIF.
 * NOT thread safe
 * @author canadensys
 *
 */
public class StreamDwcContentStep implements ProcessingStepIF{
	
	private static final Logger LOGGER = Logger.getLogger(StreamDwcContentStep.class);
	private static final int DEFAULT_FLUSH_INTERVAL = 100;
	
	@Autowired
	@Qualifier("dwcItemReader")
	private ItemReaderIF<OccurrenceRawModel> reader;
	
	@Autowired
	@Qualifier("jmsWriter")
	private ItemWriterIF<ProcessingMessageIF> writer;
	
	@Autowired
	@Qualifier("lineProcessor")
	private ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor;
	
	private int numberOfRecords = 0;
	private Map<SharedParameterEnum,Object> sharedParameters;
	
	//Flush interval, number of OccurrenceRawModel until we flush it (into a JMS message)
	private int flushInterval = DEFAULT_FLUSH_INTERVAL;
	
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
		reader.openReader(sharedParameters);
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
	public void doStep() {
		try{
			SaveRawOccurrenceMessage rom = new SaveRawOccurrenceMessage();
			ProcessOccurrenceMessage com = new ProcessOccurrenceMessage();
			
			long t= System.currentTimeMillis();
			OccurrenceRawModel currRawModel = reader.read();
			while(currRawModel != null){
				currRawModel = lineProcessor.process(currRawModel, sharedParameters);
	
				//should be done by ChunkSplitter
				rom.addRawModel(currRawModel);
				rom.setWhen(Calendar.getInstance().getTime().toString());
				com.addRawModel(currRawModel);
				com.setWhen(Calendar.getInstance().getTime().toString());
				
				currRawModel = reader.read();
				numberOfRecords++;
				
				if(numberOfRecords % flushInterval == 0){
					writer.write(rom);
					writer.write(com);
					rom = new SaveRawOccurrenceMessage();
					rom.setWhen(Calendar.getInstance().getTime().toString());
					com = new ProcessOccurrenceMessage();
					com.setWhen(Calendar.getInstance().getTime().toString());
				}
			}
			//flush remaining content
			if(rom.getRawModelList().size() > 0){
				writer.write(rom);
				writer.write(com);
			}
			
			System.out.println("Streaming the file took :" + (System.currentTimeMillis()-t) + " ms");
			
			sharedParameters.put(SharedParameterEnum.NUMBER_OF_RECORDS,numberOfRecords);
		}
		catch(WriterException e){
			LOGGER.fatal(e);
		}
	}
	
	public int getNumberOfRecords(){
		return numberOfRecords;
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
	
//	@Override
//	public void accept(JobActionVisitor visitor) {
//		visitor.visit(this);
//	}
}
