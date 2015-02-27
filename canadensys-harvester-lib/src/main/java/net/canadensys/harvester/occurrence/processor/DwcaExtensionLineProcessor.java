package net.canadensys.harvester.occurrence.processor;

import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.exception.ProcessException;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Processing each line read from an Darwin Core extension.
 * Assign an unique id and set the sourceFileId.
 * NOT thread safe
 * @author canadenys
 *
 */
public class DwcaExtensionLineProcessor implements ItemProcessorIF<OccurrenceExtensionModel, OccurrenceExtensionModel>{
	
	@Autowired
	@Qualifier(value="bufferSessionFactory")
	private SessionFactory sessionFactory;
	
	private StatelessSession session;
	private SQLQuery sqlQuery;
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(DwcaExtensionLineProcessor.class);
	
	private String idGenerationSQL = "SELECT 1";

	//we take id by batch of 100 to reduce the number of calls
	private Long nextId = null;
	private List<Number> idPoll = null;
	
	@Override
	public void init(){
		try{
			session = sessionFactory.openStatelessSession();
			session.beginTransaction();
			sqlQuery = session.createSQLQuery(idGenerationSQL);
		}
		catch(HibernateException hEx){
			LOGGER.fatal("Can't initialize DwcaLineProcessor", hEx);
		}
	}
	
	@Override
	public void destroy(){
		session.getTransaction().commit();
	}

	@Override
	public OccurrenceExtensionModel process(OccurrenceExtensionModel data, Map<SharedParameterEnum, Object> sharedParameters)
			throws ProcessException {
		
		String sourceFileId = (String)sharedParameters.get(SharedParameterEnum.SOURCE_FILE_ID);
		String resourceUUID = (String)sharedParameters.get(SharedParameterEnum.RESOURCE_UUID);
		String extensionType = (String)sharedParameters.get(SharedParameterEnum.DWCA_EXTENSION_TYPE);
		
        if(sourceFileId == null){
			LOGGER.fatal("Misconfigured processor : sourceFileId required");
			throw new TaskExecutionException("Misconfigured DwcaExtensionLineProcessor");
		}
        if(resourceUUID == null){
			LOGGER.fatal("Misconfigured processor : resourceUUID required");
			throw new TaskExecutionException("Misconfigured DwcaExtensionLineProcessor");
		}
        if(extensionType == null){
			LOGGER.fatal("Misconfigured processor : extensionType required");
			throw new TaskExecutionException("Misconfigured DwcaExtensionLineProcessor");
		}
        
        data.setSourcefileid(sourceFileId);
        data.setResource_uuid(resourceUUID);
        data.setExt_type(extensionType);
        if(nextId == null || idPoll.isEmpty()){
			try{
				idPoll = (List<Number>)sqlQuery.list();
			}
			catch(HibernateException hEx){
				LOGGER.fatal("Can't get ID from sequence", hEx);
			}
			catch (ClassCastException ccEx) {
				LOGGER.fatal("The call for the sequence must return a List of Number", ccEx);
			}
		}
		nextId = idPoll.remove(0).longValue();
		data.setAuto_id(nextId.intValue());
		return data;
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public void setIdGenerationSQL(String idGenerationSQL) {
		this.idGenerationSQL = idGenerationSQL;
	}

}
