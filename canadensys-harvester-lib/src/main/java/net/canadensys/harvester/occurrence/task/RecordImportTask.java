package net.canadensys.harvester.occurrence.task;

import java.util.Date;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task to record(save) the import for traceability
 * @author canadensys
 *
 */
public class RecordImportTask implements ItemTaskIF{
	private static final String CURRENT_USER = System.getProperty("user.name");

	@Autowired
	@Qualifier(value="publicSessionFactory")
	private SessionFactory sessionFactory;
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(RecordImportTask.class);
	
	/**
	 * @param sharedParameters SharedParameterEnum.NUMBER_OF_RECORDS, SharedParameterEnum.SOURCE_FILE_ID required
	 */
	@Transactional("publicTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum,Object> sharedParameters){
		Session session = sessionFactory.getCurrentSession();
		ImportLogModel importLogModel = new ImportLogModel();
		String sourceFileId = (String)sharedParameters.get(SharedParameterEnum.SOURCE_FILE_ID);
		Integer numberOfRecords = (Integer)sharedParameters.get(SharedParameterEnum.NUMBER_OF_RECORDS);
		
		if(sourceFileId == null || numberOfRecords == null){
			LOGGER.fatal("Misconfigured task : needs  sourceFileId and numberOfRecords");
			throw new TaskExecutionException("Misconfigured task");
		}
		importLogModel.setSourcefileid(sourceFileId);
		importLogModel.setRecord_quantity(numberOfRecords);
		importLogModel.setUpdated_by(CURRENT_USER);
		importLogModel.setEvent_end_date_time(new Date());
		session.save(importLogModel);
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public String getTitle() {
		return "Recording import";
	}
}
