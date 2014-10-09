package net.canadensys.harvester.occurrence.task;

import java.util.Map;

import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task to delete all entries from the occurrence and occurrence_raw tables for a specific sourcefileid.
 * @author canadensys
 *
 */
public class CleanBufferTableTask implements ItemTaskIF {
	
	@Autowired
	@Qualifier(value="bufferSessionFactory")
	private SessionFactory sessionFactory;
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(CleanBufferTableTask.class);
	
	/**
	 * @param sharedParameters SharedParameterEnum.SOURCE_FILE_ID required
	 */
	@Transactional("bufferTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum,Object> sharedParameters){
		String sourceFileId = (String)sharedParameters.get(SharedParameterEnum.SOURCE_FILE_ID);
		String resourceUUID = (String)sharedParameters.get(SharedParameterEnum.RESOURCE_UUID);
		
		Session session = sessionFactory.getCurrentSession();
		
		if(sourceFileId == null || resourceUUID == null){
			LOGGER.fatal("Misconfigured task : sourceFileId and resourceUUID required");
			throw new TaskExecutionException("Misconfigured task");
		}
		try{
			SQLQuery query = session.createSQLQuery("DELETE FROM buffer.occurrence_raw WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			
			query = session.createSQLQuery("DELETE FROM buffer.occurrence WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			
			query = session.createSQLQuery("DELETE FROM buffer.occurrence_extension WHERE resource_uuid=?");
			query.setString(0, resourceUUID);
			query.executeUpdate();
			
			query = session.createSQLQuery("DELETE FROM buffer.resource_contact WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
		}
		catch(HibernateException hEx){
			LOGGER.fatal("Can't remove previous records from the database.",hEx);
			throw new TaskExecutionException("Can't remove previous records from the database.");
		}
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public String getTitle() {
		return "Cleaning buffer tables";
	}
	
//	@Transactional("bufferTransactionManager")
//	@Override
//	public void accept(JobActionVisitor visitor) {
//		visitor.visit(this);
//	}
}
