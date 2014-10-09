package net.canadensys.harvester.occurrence.job;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.config.ProcessingConfigTest;
import net.canadensys.harvester.occurrence.SharedParameterEnum;
import net.canadensys.harvester.occurrence.model.JobStatusModel;
import net.canadensys.harvester.occurrence.step.SynchronousProcessEmlContentStep;
import net.canadensys.harvester.occurrence.step.SynchronousProcessOccurrenceStep;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Test coverage : 
 * -Read a DarwinCore archive from a folder
 * -Insert raw data in buffer schema
 * -Process data and insert results in buffer schema
 * 
 * @author canadensys
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=SynchronousImportDwcaJobTest.SynchronousProcessingConfigTest.class, loader=AnnotationConfigContextLoader.class)
public class SynchronousImportDwcaJobTest {
	
	private static final int EXPECTED_NUMBER_OF_RESULTS = 11;
	
	@Autowired
	@Qualifier(value="bufferSessionFactory")
	private SessionFactory sessionFactory;
	
	@Autowired
	@Qualifier(value="bufferTransactionManager")
	private HibernateTransactionManager txManager;
	
	@Autowired
	private ImportDwcaJob importDwcaJob;
	
	@Configuration
	@Import(ProcessingConfigTest.class)
	public static class SynchronousProcessingConfigTest extends ProcessingConfigTest{
		
		@Override
		@Bean(name="streamEmlContentStep")
		public StepIF streamEmlContentStep(){
			return new SynchronousProcessEmlContentStep();
		}
		@Override
		@Bean(name="streamDwcContentStep")
		public StepIF streamDwcContentStep(){
			return new SynchronousProcessOccurrenceStep();
		}
	}
	
	@Test
	public void testImport(){
		JdbcTemplate jdbcTemplate = new JdbcTemplate(txManager.getDataSource());
						
		importDwcaJob.addToSharedParameters(SharedParameterEnum.DWCA_PATH, "src/test/resources/dwca-qmor-specimens");
		importDwcaJob.addToSharedParameters(SharedParameterEnum.SOURCE_FILE_ID, "qmor-specimens");
		importDwcaJob.addToSharedParameters(SharedParameterEnum.RESOURCE_UUID, "ada5d0b1-07de-4dc0-83d4-e312f0fb81cb");
		
		JobStatusModel jobStatusModel = new JobStatusModel();
		importDwcaJob.doJob(jobStatusModel);

		int count = jdbcTemplate.queryForObject("SELECT count(*) FROM buffer.occurrence", BigDecimal.class).intValue();

		String state = jdbcTemplate.queryForObject("SELECT stateprovince FROM buffer.occurrence where dwcaid='3'", String.class);
		assertTrue("Florida".equals(state));
		
		String source = jdbcTemplate.queryForObject("SELECT sourcefileid FROM buffer.occurrence where dwcaid='1'", String.class);
		assertTrue("qmor-specimens".equals(source));
		
		String resource_contact = jdbcTemplate.queryForObject("SELECT name FROM buffer.resource_contact where sourcefileid='qmor-specimens'", String.class);
		assertTrue("Louise Cloutier".equals(resource_contact));

		assertTrue(new Integer(EXPECTED_NUMBER_OF_RESULTS).equals(count));
	}
	
	/**
	 * Test the behavior of a failing import.
	 * A common reason for failing is when data can not fit into the defined space in the database.
	 */
//	@Test
//	public void testFailedImport(){						
//		importDwcaJob.addToSharedParameters(SharedParameterEnum.DWCA_PATH, "src/test/resources/dwca-qmor-specimens-broken");
//		importDwcaJob.addToSharedParameters(SharedParameterEnum.DATASET_SHORTNAME, "qmor-specimens");
//		
//		JobStatusModel jobStatusModel = new JobStatusModel();
//		importDwcaJob.doJob(jobStatusModel);
//		synchronized (controlMessageReceived) {
//			try {
//				controlMessageReceived.wait(MAX_WAIT);
//				//validate content of the database
//				if(!controlMessageReceived.get()){
//					fail();
//				}
//			} catch (InterruptedException e) {
//				fail();
//			}
//		}
//	}

}
