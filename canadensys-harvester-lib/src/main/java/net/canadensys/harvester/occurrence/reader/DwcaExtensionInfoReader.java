package net.canadensys.harvester.occurrence.reader;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveFile;
import org.gbif.dwc.text.UnsupportedArchiveException;

/**
 * ItemReader to read the name(rowType) of all extensions included in a DarwinCore Archive.
 * 
 * @author cgendreau
 *
 */
public class DwcaExtensionInfoReader implements ItemReaderIF<String>{
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(DwcaExtensionInfoReader.class);
	
	private Iterator<ArchiveFile> extIt;

	@Override
	public void openReader(Map<SharedParameterEnum, Object> sharedParameters) {
		String dwcaFilePath = (String)sharedParameters.get(SharedParameterEnum.DWCA_PATH);
		if(StringUtils.isBlank(dwcaFilePath)){
			throw new IllegalStateException("sharedParameters missing: DWCA_PATH is required.");
		}
		
		File dwcaFile = new File(dwcaFilePath);
		Archive dwcArchive;
		try {
			dwcArchive = ArchiveFactory.openArchive(dwcaFile);
			if(dwcArchive.getExtensions() != null && !dwcArchive.getExtensions().isEmpty()){
				extIt = dwcArchive.getExtensions().iterator();
			}
		} catch (UnsupportedArchiveException e) {
			LOGGER.fatal("Can't open ExtensionInfoReader", e);
		} catch (IOException e) {
			LOGGER.fatal("Can't open ExtensionInfoReader", e);
		}
	}

	@Override
	public void closeReader() {
		// noop
	}

	@Override
	public void abort() {
		// noop
	}

	@Override
	public String read() {
		if(extIt != null && extIt.hasNext()){
			return extIt.next().getRowType();
		}
		return null;
	}
}
