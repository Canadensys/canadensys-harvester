package net.canadensys.harvester.config;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.sql.DataSource;

import net.canadensys.dataportal.occurrence.dao.OccurrenceExtensionDAO;
import net.canadensys.dataportal.occurrence.dao.impl.HibernateOccurrenceExtensionDAO;
import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceExtensionModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.controller.VersionController;
import net.canadensys.harvester.jms.JMSConsumer;
import net.canadensys.harvester.jms.control.JMSControlConsumer;
import net.canadensys.harvester.jms.control.JMSControlProducer;
import net.canadensys.harvester.main.ProcessingNodeMain;
import net.canadensys.harvester.occurrence.processor.DwcaLineProcessor;
import net.canadensys.harvester.occurrence.processor.OccurrenceProcessor;
import net.canadensys.harvester.occurrence.processor.ResourceContactProcessor;
import net.canadensys.harvester.occurrence.step.InsertResourceContactStep;
import net.canadensys.harvester.occurrence.step.async.AsyncManageOccurrenceExtensionStep;
import net.canadensys.harvester.occurrence.step.async.ProcessInsertOccurrenceStep;
import net.canadensys.harvester.occurrence.writer.OccurrenceHibernateWriter;
import net.canadensys.harvester.occurrence.writer.RawOccurrenceHibernateWriter;
import net.canadensys.harvester.occurrence.writer.ResourceContactHibernateWriter;
import net.canadensys.harvester.writer.GenericHibernateWriter;

import org.gbif.metadata.eml.Eml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Configuration class using Spring annotations.
 * All the beans are created from here.
 * 
 * @author canadensys
 * 
 */
@Configuration
@EnableTransactionManagement
public class ProcessingNodeConfig {
	
	private static String propertiesFileLocation = "config/harvester-config.properties";
	
	@Value( "${harvester.library.version:?}" )
	private String currentVersion;

	@Value("${database.url}")
	private String dbUrl;

	@Value("${database.driver}")
	private String dbDriverClassName;
	@Value("${database.username}")
	private String username;
	@Value("${database.password}")
	private String password;
	@Value("${hibernate.dialect}")
	private String hibernateDialect;

	@Value("${hibernate.show_sql}")
	private String hibernateShowSql;
	@Value("${hibernate.buffer_schema}")
	private String hibernateBufferSchema;
	@Value("${jms.broker_url}")
	private String jmsBrokerUrl;
	
	@Bean
	public static PropertyPlaceholderConfigurer properties() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setLocation(new FileSystemResource(propertiesFileLocation));
		return ppc;
	}
	
	@Bean(name = "datasource")
	public DataSource dataSource() {
		ComboPooledDataSource ds = new ComboPooledDataSource();
		try {
			ds.setDriverClass(dbDriverClassName);
		}
		catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		ds.setJdbcUrl(dbUrl);
		ds.setUser(username);
		ds.setPassword(password);
		return ds;
	}

	@Bean(name = {"bufferSessionFactory","sessionFactory"})
	public LocalSessionFactoryBean bufferSessionFactory() {
		LocalSessionFactoryBean sb = new LocalSessionFactoryBean();
		sb.setDataSource(dataSource());
		sb.setAnnotatedClasses(new Class[] { OccurrenceRawModel.class, OccurrenceModel.class, ImportLogModel.class,
				OccurrenceExtensionModel.class, ResourceContactModel.class });

		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty("hibernate.dialect", hibernateDialect);
		hibernateProperties.setProperty("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.setProperty("hibernate.default_schema", hibernateBufferSchema);
		hibernateProperties.setProperty("hibernate.connection.autocommit", "false");
		hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
		sb.setHibernateProperties(hibernateProperties);
		return sb;
	}
	
	//test
	@Bean
	public OccurrenceExtensionDAO occurrenceExtensionDAO() {
		HibernateOccurrenceExtensionDAO occurrenceExtensionDAO = new HibernateOccurrenceExtensionDAO();
		//occurrenceExtensionDAO.setSessionFactory(bufferSessionFactory().getObject());
		return occurrenceExtensionDAO;
	}
	
	@Bean(name = "bufferTransactionManager")
	public HibernateTransactionManager hibernateTransactionManager() {
		HibernateTransactionManager htmgr = new HibernateTransactionManager();
		htmgr.setSessionFactory(bufferSessionFactory().getObject());
		return htmgr;
	}
	
	@Bean
	public ProcessingNodeMain processingNodeMain(){
		return new ProcessingNodeMain();
	}
	
	@Bean(name = "publicTransactionManager")
	public HibernateTransactionManager publicHibernateTransactionManager() {
		HibernateTransactionManager htmgr = new HibernateTransactionManager();
		htmgr.setSessionFactory(publicSessionFactory().getObject());
		return htmgr;
	}

	@Bean(name = "publicSessionFactory")
	public LocalSessionFactoryBean publicSessionFactory() {
		LocalSessionFactoryBean sb = new LocalSessionFactoryBean();
		sb.setDataSource(dataSource());
		sb.setAnnotatedClasses(new Class[] { OccurrenceRawModel.class, OccurrenceModel.class, ImportLogModel.class });

		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty("hibernate.dialect", hibernateDialect);
		hibernateProperties.setProperty("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
		sb.setHibernateProperties(hibernateProperties);
		return sb;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	// ---JOB---
	// Nodes should not initiate jobs

	// ---JMS---
	@Bean
	public JMSControlProducer errorReporter() {
		return new JMSControlProducer(jmsBrokerUrl);
	}

	@Bean(name = "jmsConsumer")
	public JMSConsumer jmsConsumer() {
		return new JMSConsumer(jmsBrokerUrl);
	}
	
	@Bean(destroyMethod="close")
	public JMSControlConsumer controlMessageReceiver(){
		return new JMSControlConsumer(jmsBrokerUrl);
	}

	// ---PROCESSOR wiring---
	@Bean(name = "lineProcessor")
	public ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor() {
		return new DwcaLineProcessor();
	}

	@Bean(name = "occurrenceProcessor")
	public ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> occurrenceProcessor() {
		return new OccurrenceProcessor();
	}


	// ---STEP---
	@Bean(name = "processInsertOccurrenceStep")
	public StepIF processInsertOccurrenceStep() {
		return new ProcessInsertOccurrenceStep();
	}
	
	@Bean(name = "insertResourceContactStep")
	public StepIF insertResourceContactStep() {
		return new InsertResourceContactStep();
	}
	
	@Bean
	public StepIF asyncManageOccurrenceExtensionStep() {
		return new AsyncManageOccurrenceExtensionStep();
	}

	// ---WRITER---
	@Bean(name = "occurrenceWriter")
	public ItemWriterIF<OccurrenceModel> occurrenceWriter() {
		return new OccurrenceHibernateWriter();
	}
	
	@Bean(name = "rawOccurrenceWriter")
	public ItemWriterIF<OccurrenceRawModel> rawOccurrenceWriter() {
		return new RawOccurrenceHibernateWriter();
	}

	@Bean(name = "resourceContactWriter")
	public ItemWriterIF<ResourceContactModel> resourceContactHibernateWriter() {
		return new ResourceContactHibernateWriter();
	}
	
	@Bean(name="occurrenceExtensionWriter")
	public ItemWriterIF<OccurrenceExtensionModel> occurrenceExtensionWriter(){
		return new GenericHibernateWriter<OccurrenceExtensionModel>();
	}

	@Bean(name = "resourceContactProcessor")
	public ItemProcessorIF<Eml, ResourceContactModel> resourceContactProcessor() {
		return new ResourceContactProcessor();
	}
	
	@Bean
	public VersionController versionController(){
		return new VersionController();
	}

	@Bean(name="currentVersion")
	public String getCurrentVersion() {
		return currentVersion;
	}
}
