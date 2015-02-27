package net.canadensys.harvester.occurrence.mapper;

import java.util.HashMap;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.harvester.ItemMapperIF;

/**
 * Map properties into OccurrenceExtensionModel.
 * Set the dwcaid using the "id" property.
 * 
 * @author cgendreau
 *
 */
public class OccurrenceExtensionMapper implements ItemMapperIF<OccurrenceExtensionModel> {

	@Override
	public OccurrenceExtensionModel mapElement(Map<String, Object> properties) {
		
		OccurrenceExtensionModel occExtModel = new OccurrenceExtensionModel();
		Map<String,String> extData = new HashMap<String, String>();
		
		for(String currKey : properties.keySet()){
			if(currKey.equalsIgnoreCase("id")){
				occExtModel.setDwcaid((String)properties.get(currKey));
			}
			else{
				extData.put(currKey, (String)properties.get(currKey));
			}
		}
		occExtModel.setExt_data(extData);
		
		return occExtModel;
	}

}
