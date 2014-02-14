package net.canadensys.harvester.occurrence.task;

import java.util.Date;
import java.util.Map;

import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.model.ImportLogModel;

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
	 * @param sharedParameters in:SharedParameterEnum.DATASET_SHORTNAME,SharedParameterEnum.NUMBER_OF_RECORDS
	 */
	@Transactional("publicTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum,Object> sharedParameters){
		Session session = sessionFactory.getCurrentSession();
		ImportLogModel importLogModel = new ImportLogModel();
		String datasetShortname = (String)sharedParameters.get(SharedParameterEnum.DATASET_SHORTNAME);
		Integer numberOfRecords = (Integer)sharedParameters.get(SharedParameterEnum.NUMBER_OF_RECORDS);
		
		if(datasetShortname == null || numberOfRecords == null){
			LOGGER.fatal("Misconfigured task : needs  datasetShortname and numberOfRecords");
			throw new TaskExecutionException("Misconfigured task");
		}
		importLogModel.setSourcefileid(datasetShortname);
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
