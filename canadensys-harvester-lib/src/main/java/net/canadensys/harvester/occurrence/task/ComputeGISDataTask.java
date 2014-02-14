package net.canadensys.harvester.occurrence.task;

import java.util.Map;

import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task to compute all GIS related data in our PostGIS enabled table.
 * To ensure maximum performance we run this once at the end of the import
 * @author canadensys
 *
 */
public class ComputeGISDataTask implements ItemTaskIF{
	
	//get log4j handler
	private static final Logger LOGGER = Logger.getLogger(ComputeGISDataTask.class);
	
	//we work with public sessionFactory but we update the buffer schema
	@Autowired
	@Qualifier(value="publicSessionFactory")
	private SessionFactory sessionFactory;
	
	/**
	 * @param sharedParameters in:DATASET_SHORTNAME
	 */
	@Transactional("publicTransactionManager")
	@Override
	public void execute(Map<SharedParameterEnum,Object> sharedParameters){
		String datasetShortname = (String)sharedParameters.get(SharedParameterEnum.DATASET_SHORTNAME);
		Session session = sessionFactory.getCurrentSession();
		
		if(datasetShortname == null){
			LOGGER.fatal("Misconfigured task : needs  datasetShortname");
			throw new TaskExecutionException("Misconfigured task");
		}
		//update the_geom
		SQLQuery query = session.createSQLQuery("UPDATE buffer.occurrence SET the_geom = st_geometryfromtext('POINT('||decimallongitude||' '|| decimallatitude ||')',4326) " +
				"WHERE sourcefileid=? AND decimallatitude IS NOT NULL AND decimallongitude IS NOT NULL");
		query.setString(0, datasetShortname);
		query.executeUpdate();
		
		//update the_geom_webmercator
		query = session.createSQLQuery("UPDATE buffer.occurrence SET the_geom_webmercator = st_transform_null(the_geom,3857) WHERE sourcefileid=? AND the_geom IS NOT NULL");
		query.setString(0, datasetShortname);
		query.executeUpdate();
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public String getTitle() {
		return "Computing GIS info";
	}
}
