package net.canadensys.harvester.occurrence.reader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.canadensys.harvester.ItemMapperIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.UnsupportedArchiveException;

/**
 * Generic reader to read a DarwinCore Archive extension.
 * 
 * @author canadensys
 *
 * @param <T> object that will contain a line of the extension
 */
public class DwcaExtensionReader<T> extends AbstractDwcaReaderSupport implements ItemReaderIF<T>{

	private static final Logger LOGGER = Logger.getLogger(DwcaExtensionReader.class);
	
	private final AtomicBoolean canceled = new AtomicBoolean(false);
	// Should rows that are completely empty (separators are there but no values) be read ?
	private final boolean skipEmptyRows = true;
	private String dwcaExtensionType = null;
	
	private ItemMapperIF<T> occurrenceExtensionMapper;
	
	@Override
	public void openReader(Map<SharedParameterEnum, Object> sharedParameters) {
		dwcaFilePath = (String)sharedParameters.get(SharedParameterEnum.DWCA_PATH);
		dwcaExtensionType = (String)sharedParameters.get(SharedParameterEnum.DWCA_EXTENSION_TYPE);
		
		if(occurrenceExtensionMapper == null){
			throw new IllegalStateException("No mapper defined");
		}
		if(StringUtils.isBlank(dwcaFilePath) || StringUtils.isBlank(dwcaExtensionType)){
			throw new IllegalStateException("sharedParameters missing: DWCA_PATH and DWCA_EXTENSION_TYPE are required.");
		}
		
		File dwcaFile = new File(dwcaFilePath);
		Archive dwcArchive;
		try {
			dwcArchive = ArchiveFactory.openArchive(dwcaFile);
			prepareReader(dwcArchive.getExtension(dwcaExtensionType, true));
		} catch (UnsupportedArchiveException e) {
			LOGGER.fatal("Can't open DwcaExtensionReader", e);
		} catch (IOException e) {
			LOGGER.fatal("Can't open DwcaExtensionReader", e);
		}
	}

	@Override
	public void closeReader() {
		super.closeReader();
	}

	@Override
	public T read() {
		if(canceled.get() || !rowsIt.hasNext()){
			return null;
		}
		
		//ImmutableMap from Google Collections?
		Map<String,Object> properties = new HashMap<String, Object>();
		int i=0;
		String[] data = skipEmptyRows?getNextNonEmptyLine():rowsIt.next();
		
		for(String currHeader : headers){
			properties.put(currHeader, data[i]);
			i++;
		}
		//check if some default values must be handled
		if(defaultValues != null){
			for(String defaultValueCol : defaultValues.keySet()){
				properties.put(defaultValueCol, defaultValues.get(defaultValueCol));
			}
		}
		return occurrenceExtensionMapper.mapElement(properties);
	}
	
	/**
	 * This method will skip rows where all the terms are empty.
	 * @return
	 */
	private String[] getNextNonEmptyLine(){

		String[] data = null;
		while(rowsIt.hasNext()){
			data = rowsIt.next();
			
			for(int i=0;i<data.length;i++){
				if(StringUtils.isNotBlank(data[i])){
					return data;
				}
			}
		}
		return null;
	}
	
	/**
	 * Set the row mapper to use to translate properties into object.
	 * @param mapper
	 */
	public void setMapper(ItemMapperIF<T> mapper){
		this.occurrenceExtensionMapper = mapper;
	}

	@Override
	public void abort() {
		canceled.set(true);
	}
}
