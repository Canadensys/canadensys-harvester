package net.canadensys.harvester.occurrence.controller;

import java.util.List;

import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.dataportal.occurrence.model.ResourceModel;
import net.canadensys.harvester.ItemProgressListenerIF;
import net.canadensys.harvester.occurrence.model.IPTFeedModel;

public interface StepControllerIF {

	public List<IPTFeedModel> getIPTFeed();
	public List<ResourceModel> getResourceToHarvest();
	
	public List<ResourceModel> getResourceModelList();

	public List<ImportLogModel> getSortedImportLogModelList();

	public void importDwcA(Integer resourceId);

	/**
	 * This function should be used very carefully since the 'sourcefileid' will be determined from
	 * the file name. This could lead to unwanted behavior if 2 different resource have the same name.
	 * Using importDwcA(Integer resourceId) is always preferable.
	 * @param dwcaPath
	 */
	public void importDwcAFromLocalFile(String dwcaPath);

	public void moveToPublicSchema(String datasetShortName, String resourceUUID);

	public void registerProgressListener(ItemProgressListenerIF progressListener);

	/**
	 * Insert or update a ResourceModel.
	 * 
	 * @param resourceModel
	 * @return
	 */
	public boolean updateResourceModel(ResourceModel resourceModel);

	public void onNodeError();
}
