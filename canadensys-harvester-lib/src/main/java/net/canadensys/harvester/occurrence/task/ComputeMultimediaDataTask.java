package net.canadensys.harvester.occurrence.task;

import java.util.Map;

import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.log4j.Logger;
import org.gbif.dwc.terms.GbifTerm;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task used to set the Occurrence 'hasmedia' flag when all the occurrence multimedia extension records are saved.
 * @author cgendreau
 *
 */
public class ComputeMultimediaDataTask implements ItemTaskIF{
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(ComputeMultimediaDataTask.class);
	
	//we work with public sessionFactory but we update the buffer schema
	@Autowired
	@Qualifier(value="publicSessionFactory")
	private SessionFactory sessionFactory;
	
	/**
	 * @param @param sharedParameters SharedParameterEnum.RESOURCE_UUID required
	 */
	@Transactional("publicTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum,Object> sharedParameters){
		String sourceFileId = (String)sharedParameters.get(SharedParameterEnum.SOURCE_FILE_ID);
		String resourceUUID = (String)sharedParameters.get(SharedParameterEnum.RESOURCE_UUID);
		Session session = sessionFactory.getCurrentSession();
		
		if(resourceUUID == null || sourceFileId == null){
			LOGGER.fatal("Misconfigured task : sourceFileId and resourceUUID required");
			throw new TaskExecutionException("Misconfigured task");
		}
		
		//only set hasmedia to true if we can do the inner join
		SQLQuery query = session.createSQLQuery(
		"UPDATE buffer.occurrence AS bocc SET hasmedia = true WHERE"
		+ " EXISTS(SELECT dwcaid from buffer.occurrence_extension WHERE dwcaid = bocc.dwcaid AND resource_uuid = ? AND ext_type = ?)"
		+ " AND sourcefileid = ?");
		query.setString(0, resourceUUID);
		query.setString(1, GbifTerm.Multimedia.simpleName());
		query.setString(2, sourceFileId);
		query.executeUpdate();
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public String getTitle() {
		return "Computing Multimedia info";
	}

}
