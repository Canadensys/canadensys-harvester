package net.canadensys.harvester.occurrence.task;

import java.util.Map;

import net.canadensys.dataportal.occurrence.model.ResourceModel;
import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task to get info based on a resource id.
 * The info will be shared using the sharedParameters map.
 * @author canadensys
 *
 */
public class GetResourceInfoTask implements ItemTaskIF{
	
	@Autowired
	@Qualifier(value="publicSessionFactory")
	private SessionFactory sessionFactory;
	
	@Transactional("publicTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum, Object> sharedParameters)
			throws TaskExecutionException {
		Integer resourceId = (Integer)sharedParameters.get(SharedParameterEnum.RESOURCE_ID);

		Criteria searchCriteria = sessionFactory.getCurrentSession().createCriteria(ResourceModel.class);
		searchCriteria.add(Restrictions.eq("id", resourceId));
		ResourceModel resourceModel = (ResourceModel)searchCriteria.uniqueResult();
		
		if(resourceModel == null){
			throw new TaskExecutionException("ResourceID " + resourceId + " not found");
		}
		sharedParameters.put(SharedParameterEnum.DWCA_URL, resourceModel.getArchive_url());
		sharedParameters.put(SharedParameterEnum.SOURCE_FILE_ID, resourceModel.getSourcefileid());
		sharedParameters.put(SharedParameterEnum.RESOURCE_UUID, resourceModel.getResource_uuid());
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public String getTitle() {
		return "Getting resource info";
	}
}
