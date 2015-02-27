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
 * Task to move all the records for a specific sourcefileid from buffer to public database schema
 * @author canadensys
 *
 */
public class ReplaceOldOccurrenceTask implements ItemTaskIF{
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(ReplaceOldOccurrenceTask.class);

	@Autowired
	@Qualifier(value="publicSessionFactory")
	private SessionFactory sessionFactory;
	
	/**
	 * @param sharedParameters SharedParameterEnum.SOURCE_FILE_ID, SharedParameterEnum.RESOURCE_UUID required
	 */
	@Transactional("publicTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum,Object> sharedParameters){
		Session session = sessionFactory.getCurrentSession();
		
		String sourceFileId = (String)sharedParameters.get(SharedParameterEnum.SOURCE_FILE_ID);
		String resourceUUID = (String)sharedParameters.get(SharedParameterEnum.RESOURCE_UUID);

		if(sourceFileId == null || resourceUUID == null){
			LOGGER.fatal("Misconfigured task : sourceFileId and resourceUUID are required");
			throw new TaskExecutionException("Misconfigured task");
		}
		
		try{
			//delete old records
			SQLQuery query = session.createSQLQuery("DELETE FROM occurrence WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("DELETE FROM occurrence_raw WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("DELETE FROM resource_contact WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("DELETE FROM occurrence_extension WHERE resource_uuid=?");
			query.setString(0, resourceUUID);
			query.executeUpdate();
			
			//copy records from buffer
			query = session.createSQLQuery("INSERT INTO occurrence (SELECT * FROM buffer.occurrence WHERE sourcefileid=?)");
			query.setString(0, sourceFileId);
			int numberOfRecords = query.executeUpdate();
			query = session.createSQLQuery("INSERT INTO occurrence_raw (SELECT * FROM buffer.occurrence_raw WHERE sourcefileid=?)");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("INSERT INTO resource_contact (SELECT * FROM buffer.resource_contact WHERE sourcefileid=?)");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("INSERT INTO occurrence_extension (SELECT * FROM buffer.occurrence_extension WHERE resource_uuid=?)");
			query.setString(0, resourceUUID);
			query.executeUpdate();
			
			//empty buffer schema for this sourcefileid
			query = session.createSQLQuery("DELETE FROM buffer.occurrence WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("DELETE FROM buffer.occurrence_raw WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("DELETE FROM buffer.resource_contact WHERE sourcefileid=?");
			query.setString(0, sourceFileId);
			query.executeUpdate();
			query = session.createSQLQuery("DELETE FROM buffer.occurrence_extension WHERE resource_uuid=?");
			query.setString(0, resourceUUID);
			query.executeUpdate();
			
			sharedParameters.put(SharedParameterEnum.NUMBER_OF_RECORDS, numberOfRecords);
		}
		catch(HibernateException hEx){
			LOGGER.fatal("Can't replace previous records in public schema.",hEx);
			throw new TaskExecutionException("Can't replace previous records in public schema.");
		}
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public String getTitle() {
		return "Replacing old occurrences";
	}
}
