package net.canadensys.harvester.occurrence.job;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.config.ProcessingConfigTest;
import net.canadensys.harvester.jms.JMSConsumer;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;
import net.canadensys.harvester.jms.control.JMSControlConsumer;
import net.canadensys.harvester.jms.control.JMSControlConsumerMessageHandlerIF;
import net.canadensys.harvester.message.ControlMessageIF;
import net.canadensys.harvester.message.control.NodeErrorControlMessage;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.model.JobStatusModel;
import net.canadensys.harvester.occurrence.model.JobStatusModel.JobStatus;

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Test coverage : 
 * -Read a DarwinCore archive from a folder
 * -Send the content as JMS messages
 * -Insert raw data in buffer schema
 * -Process data and insert results in buffer schema
 * -Wait for completion
 * -Move to public schema
 * -Log the import
 * 
 * Not cover by this test :
 * -GetResourceInfoTask
 * -ComputeGISDataTask
 * @author canadensys
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=ProcessingConfigTest.class, loader=AnnotationConfigContextLoader.class)
public class ImportDwcaJobTest implements PropertyChangeListener{
	
	private static final String TEST_BROKER_URL = "vm://localhost?broker.persistent=false";
	private static AtomicBoolean jobComplete = new AtomicBoolean(false);
	private static AtomicBoolean controlMessageReceived = new AtomicBoolean(false);
	private static final int MAX_WAIT = 60000;
	
	private static final int EXPECTED_NUMBER_OF_RESULTS = 11;
	private static final int MAX_NUMBER_OF_ATTEMP = 5;
	
	@Autowired
	@Qualifier(value="bufferSessionFactory")
	private SessionFactory sessionFactory;
	
	@Autowired
	@Qualifier(value="publicTransactionManager")
	private HibernateTransactionManager txManager;
	
	private JMSConsumer reader;
	private JMSControlConsumer controlConsumer;
	
	@Autowired
	private ImportDwcaJob importDwcaJob;
		
	@Autowired
	@Qualifier("processInsertOccurrenceStep")
	private JMSConsumerMessageHandlerIF processInsertOccurrenceStep;
	
	@Autowired
	@Qualifier("insertResourceContactStep")
	private JMSConsumerMessageHandlerIF insertResourceContactStep;
	
	/**
	 * Setup Test Consumer
	 * This consumer will write to the database specified by the sessionFactory bean.
	 */
	@Before
	public void setup(){
		reader = new JMSConsumer(TEST_BROKER_URL);
		reader.registerHandler(processInsertOccurrenceStep);
		reader.registerHandler(insertResourceContactStep);
		
		try {
			((StepIF)processInsertOccurrenceStep).preStep(null);
			((StepIF)insertResourceContactStep).preStep(null);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		reader.open();
		
		controlConsumer = new JMSControlConsumer(TEST_BROKER_URL);
		controlConsumer.registerHandler(new MockControlMessageHandler());
		controlConsumer.open();
	}

	@After
	public void destroy(){
		reader.close();
		controlConsumer.close();
		((StepIF)processInsertOccurrenceStep).postStep();
		((StepIF)insertResourceContactStep).postStep();
	}
	
	@Test
	public void testImport(){
		JdbcTemplate jdbcTemplate = new JdbcTemplate(txManager.getDataSource());
						
		importDwcaJob.addToSharedParameters(SharedParameterEnum.DWCA_PATH, "src/test/resources/dwca-qmor-specimens");
		importDwcaJob.addToSharedParameters(SharedParameterEnum.SOURCE_FILE_ID, "qmor-specimens");
		importDwcaJob.addToSharedParameters(SharedParameterEnum.RESOURCE_UUID, "ada5d0b1-07de-4dc0-83d4-e312f0fb81cb");
		
		JobStatusModel jobStatusModel = new JobStatusModel();
		jobStatusModel.addPropertyChangeListener(this);
		importDwcaJob.doJob(jobStatusModel);
		synchronized (jobComplete) {
			try {
				jobComplete.wait(MAX_WAIT);
				//validate content of the database
				if(jobComplete.get()){
					int count = jdbcTemplate.queryForObject("SELECT count(*) FROM buffer.occurrence", BigDecimal.class).intValue();
					//give a chance to the database to be updated (since it's triggered by a JMS message)
					int nbOfAttemp = 0;
					while(count != EXPECTED_NUMBER_OF_RESULTS && nbOfAttemp < MAX_NUMBER_OF_ATTEMP){
						nbOfAttemp++;
						Thread.sleep(1000);
						count = jdbcTemplate.queryForObject("SELECT count(*) FROM buffer.occurrence", BigDecimal.class).intValue();
					}
					
					String state = jdbcTemplate.queryForObject("SELECT stateprovince FROM buffer.occurrence where dwcaid='3'", String.class);
					assertTrue("Florida".equals(state));
					
					String source = jdbcTemplate.queryForObject("SELECT sourcefileid FROM buffer.occurrence where dwcaid='1'", String.class);
					assertTrue("qmor-specimens".equals(source));
					
					String resource_contact = jdbcTemplate.queryForObject("SELECT name FROM buffer.resource_contact where sourcefileid='qmor-specimens'", String.class);
					assertTrue("Louise Cloutier".equals(resource_contact));

					assertTrue(new Integer(EXPECTED_NUMBER_OF_RESULTS).equals(count));
				}
				else{
					fail();
				}
			} catch (InterruptedException e) {
				fail();
			}
		}
	}
	
	/**
	 * Test the behavior of a failing import.
	 * A common reason for failing is when data can not fit into the defined space in the database.
	 */
	@Test
	public void testFailedImport(){						
		importDwcaJob.addToSharedParameters(SharedParameterEnum.DWCA_PATH, "src/test/resources/dwca-qmor-specimens-broken");
		importDwcaJob.addToSharedParameters(SharedParameterEnum.SOURCE_FILE_ID, "qmor-specimens");
		importDwcaJob.addToSharedParameters(SharedParameterEnum.RESOURCE_UUID, "ada5d0b1-07de-4dc0-83d4-e312f0fb81cb");
		
		JobStatusModel jobStatusModel = new JobStatusModel();
		importDwcaJob.doJob(jobStatusModel);
		synchronized (controlMessageReceived) {
			try {
				controlMessageReceived.wait(MAX_WAIT);
				//validate content of the database
				if(!controlMessageReceived.get()){
					fail();
				}
			} catch (InterruptedException e) {
				fail();
			}
		}
	}
	
	/**
	 * JMSControlConsumerMessageHandlerIF implementation for unit testing.
	 * @author canadensys
	 *
	 */
	private class MockControlMessageHandler implements JMSControlConsumerMessageHandlerIF{

		@Override
		public Class<?> getMessageClass() {
			return NodeErrorControlMessage.class;
		}

		@Override
		public boolean handleMessage(ControlMessageIF message) {
			synchronized (controlMessageReceived) {
				controlMessageReceived.set(true);
				controlMessageReceived.notifyAll();
			}
			return true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pcEvt) {
		if(JobStatusModel.CURRENT_STATUS_PROPERTY.equals(pcEvt.getPropertyName())) {
			JobStatus newStatus = (JobStatus)pcEvt.getNewValue();
			if(JobStatus.DONE == newStatus){
				onSuccess();
			}
			else if(JobStatus.ERROR == newStatus){
				onError();
			}
		}
	}
	
	public void onSuccess() {
		synchronized (jobComplete) {
			jobComplete.set(true);
			jobComplete.notifyAll();
		}
	}

	public void onError() {
		synchronized (jobComplete) {
			jobComplete.set(false);
			jobComplete.notifyAll();
		}
	}
}
