package net.canadensys.harvester.occurrence.processor;

import java.util.List;
import java.util.Map;

import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.harvester.ItemProcessorIF;
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
 * Processing each line read from a Darwin Core Archive.
 * Assign an unique id to link the raw and processed model together.
 * NOT thread safe
 * @author canadenys
 *
 */
public class DwcaLineProcessor implements ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel>{

	@Autowired
	@Qualifier(value="bufferSessionFactory")
	private SessionFactory sessionFactory;
	
	private StatelessSession session;
	private SQLQuery sqlQuery;
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(DwcaLineProcessor.class);
	
	private String idGenerationSQL = "SELECT nextval('buffer.occurrence_raw_auto_id_seq') FROM generate_series(1,100)";

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
	
	/**
	 * @return same instance of OccurrenceRawModel with modified values
	 */
	@SuppressWarnings("unchecked")
	@Override
	public OccurrenceRawModel process(OccurrenceRawModel occModel, Map<SharedParameterEnum,Object> sharedParameters) {
		//TODO could be done at init phase?
		String sourceFileId = (String)sharedParameters.get(SharedParameterEnum.SOURCE_FILE_ID);
        
        if(sourceFileId == null){
			LOGGER.fatal("Misconfigured processor : needs  sourceFileId");
			throw new TaskExecutionException("Misconfigured processor");
		}
		occModel.setSourcefileid(sourceFileId);

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

		occModel.setAuto_id(nextId.intValue());
		
		//TODO maybe check the uniqueness of occModel.getId()
		return occModel;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public void setIdGenerationSQL(String idGenerationSQL) {
		this.idGenerationSQL = idGenerationSQL;
	}
}
