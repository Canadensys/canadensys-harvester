package net.canadensys.harvester.occurrence.writer;

import java.util.List;

import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.exception.WriterException;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Item writer for OccurrenceModel using Hibernate.
 * 
 * @author canadensys
 * 
 */
public class OccurrenceHibernateWriter implements ItemWriterIF<OccurrenceModel> {

	private static final Logger LOGGER = Logger.getLogger(OccurrenceHibernateWriter.class);

	@Autowired
	@Qualifier(value = "bufferSessionFactory")
	private SessionFactory sessionFactory;

	private StatelessSession session;

	@Override
	public void closeWriter() {
		session.close();
	}

	@Override
	public void openWriter() {
		session = sessionFactory.openStatelessSession();
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void write(List<? extends OccurrenceModel> elementList) throws WriterException{
		Transaction tx = null;
		String lastDwcaId = "";
		try {
			tx = session.beginTransaction();
			for (OccurrenceModel currOccurrence : elementList) {
				lastDwcaId = currOccurrence.getDwcaid();
				session.insert(currOccurrence);
			}
			tx.commit();
		} catch (HibernateException hEx) {
			LOGGER.fatal("Failed to write OccurrenceModel ["+lastDwcaId+"]", hEx);
			if(tx != null){
				tx.rollback();
			}
			throw new WriterException(lastDwcaId,hEx.getMessage());
		}
	}

	@Override
	public void write(OccurrenceModel occModel) throws WriterException {
		Transaction tx = null;
		try {
			Session currSession = sessionFactory.getCurrentSession();
			currSession.setCacheMode(CacheMode.IGNORE);
			tx = currSession.beginTransaction();
			currSession.save(occModel);
			tx.commit();
		} catch (HibernateException hEx) {
			LOGGER.fatal("Failed to write OccurrenceModel ["+occModel.getDwcaid()+"]", hEx);
			if(tx != null){
				tx.rollback();
			}
			throw new WriterException(occModel.getDwcaid(),hEx.getMessage());
		}
	}
}
