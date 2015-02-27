package net.canadensys.harvester.occurrence.notification.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.dao.ImportLogDAO;
import net.canadensys.dataportal.occurrence.dao.ResourceDAO;
import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.dataportal.occurrence.model.ResourceModel;
import net.canadensys.harvester.occurrence.dao.IPTFeedDAO;
import net.canadensys.harvester.occurrence.model.IPTFeedModel;
import net.canadensys.harvester.occurrence.notification.ResourceStatusNotifierIF;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of ResourceStatusNotifierIF that is using the IPT RSS feed to get the last publication date.
 * The date is then used to compare with the last import event of each resources.
 * @author cgendreau
 *
 */
public class DefaultResourceStatusNotifier implements ResourceStatusNotifierIF {
	private static final Logger LOGGER = Logger.getLogger(DefaultResourceStatusNotifier.class);
	private static final String IPT_RSS_SUFFIX = "/rss.do";
	
	//very simple cache to avoid parsing the RSS feed on each resources
	private Map<String,List<IPTFeedModel>> feedModelByIPTAddress;
	
	@Autowired
	private ResourceDAO resourceDAO;
	
	@Autowired
	private IPTFeedDAO iptFeedDAO;
	
	@Autowired
	private ImportLogDAO importLogDAO;
	
	public DefaultResourceStatusNotifier(){
		feedModelByIPTAddress = new HashMap<String, List<IPTFeedModel>>();
	}
	
	@Override
	@Transactional("publicTransactionManager")
	public List<ResourceModel> getHarvestRequiredList() {
		
		List<ResourceModel> resourceToHarvest = new ArrayList<ResourceModel>();
		List<ResourceModel> resourcesList = resourceDAO.loadResources();
		
		String iptAddress;
		URL iptURL;
		for(ResourceModel currResource : resourcesList){
			//we deduce the RSS feed address from the archive URL. This may become a problem in the future.
			iptAddress = StringUtils.substringBeforeLast(currResource.getArchive_url(), "/") + IPT_RSS_SUFFIX;
			if(!feedModelByIPTAddress.containsKey(iptAddress)){
				try {
					iptURL = new URL(iptAddress);
					feedModelByIPTAddress.put(iptAddress, iptFeedDAO.getIPTFeed(iptURL));
				} catch (MalformedURLException e) {
					LOGGER.error("Can't build IPT RSS feed address", e);
				}
			}
			
			List<IPTFeedModel> feedEntryList = feedModelByIPTAddress.get(iptAddress);
			String resourceKey;
			ImportLogModel lastImportLog;
			if(StringUtils.isNotBlank(currResource.getResource_uuid())){
				for(IPTFeedModel currFeed : feedEntryList){
					//strip the version from the URI 
					resourceKey = StringUtils.substringBeforeLast(currFeed.getUri(),"/");
					
					if(currResource.getResource_uuid().equals(resourceKey)){
						lastImportLog = importLogDAO.loadLastFrom(currResource.getSourcefileid());
						if(lastImportLog != null){
							//compare date
							if(currFeed.getPublishedDate().after(lastImportLog.getEvent_end_date_time())){
								resourceToHarvest.add(currResource);
							}
						}
						//if it was never imported, add it to the list
						else{
							resourceToHarvest.add(currResource);
						}
					}
				}
			}
			else{
				LOGGER.warn("Resource ["+currResource.getSourcefileid()+"] doesn't have a key. Can't validate status.");
			}
		}
		return resourceToHarvest;
	}

}
