package net.canadensys.harvester.config;

import java.util.Properties;

import javax.sql.DataSource;

import net.canadensys.dataportal.occurrence.dao.ImportLogDAO;
import net.canadensys.dataportal.occurrence.dao.ResourceDAO;
import net.canadensys.dataportal.occurrence.dao.impl.HibernateResourceDAO;
import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.dataportal.occurrence.model.ResourceModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.config.harvester.HarvesterConfig;
import net.canadensys.harvester.config.harvester.HarvesterConfigIF;
import net.canadensys.harvester.jms.JMSWriter;
import net.canadensys.harvester.jms.control.JMSControlConsumer;
import net.canadensys.harvester.jms.control.JMSControlProducer;
import net.canadensys.harvester.occurrence.controller.NodeStatusController;
import net.canadensys.harvester.occurrence.controller.StepController;
import net.canadensys.harvester.occurrence.controller.StepControllerIF;
import net.canadensys.harvester.occurrence.dao.IPTFeedDAO;
import net.canadensys.harvester.occurrence.job.ComputeUniqueValueJob;
import net.canadensys.harvester.occurrence.job.ImportDwcaJob;
import net.canadensys.harvester.occurrence.job.MoveToPublicSchemaJob;
import net.canadensys.harvester.occurrence.notification.ResourceStatusNotifierIF;
import net.canadensys.harvester.occurrence.processor.DwcaLineProcessor;
import net.canadensys.harvester.occurrence.processor.OccurrenceProcessor;
import net.canadensys.harvester.occurrence.processor.ResourceContactProcessor;
import net.canadensys.harvester.occurrence.reader.DwcaEmlReader;
import net.canadensys.harvester.occurrence.reader.DwcaItemReader;
import net.canadensys.harvester.occurrence.step.InsertResourceContactStep;
import net.canadensys.harvester.occurrence.step.StreamEmlContentStep;
import net.canadensys.harvester.occurrence.step.async.ProcessInsertOccurrenceStep;
import net.canadensys.harvester.occurrence.step.stream.StreamDwcContentStep;
import net.canadensys.harvester.occurrence.task.CheckHarvestingCompletenessTask;
import net.canadensys.harvester.occurrence.task.CleanBufferTableTask;
import net.canadensys.harvester.occurrence.task.ComputeUniqueValueTask;
import net.canadensys.harvester.occurrence.task.PrepareDwcaTask;
import net.canadensys.harvester.occurrence.task.RecordImportTask;
import net.canadensys.harvester.occurrence.task.ReplaceOldOccurrenceTask;
import net.canadensys.harvester.occurrence.view.model.HarvesterViewModel;
import net.canadensys.harvester.occurrence.writer.OccurrenceHibernateWriter;
import net.canadensys.harvester.occurrence.writer.RawOccurrenceHibernateWriter;
import net.canadensys.harvester.occurrence.writer.ResourceContactHibernateWriter;

import org.gbif.metadata.eml.Eml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration("processingConfig")
@EnableTransactionManagement
public class TestConfig {

	@Value("${database.url}")
	private String dbUrl;

	@Value("${database.driver}")
	private String dbDriverClassName;
	@Value("${hibernate.dialect}")
	private String hibernateDialect;

	@Value("${hibernate.show_sql}")
	private String hibernateShowSql;
	@Value("${hibernate.buffer_schema}")
	private String hibernateBufferSchema;
	@Value("${occurrence.idGenerationSQL}")
	private String idGenerationSQL;

	@Value("${jms.broker_url}")
	private String jmsBrokerUrl;

	// optional
	@Value("${ipt.rss:}")
	private String iptRssAddress;

	@Bean
	public static PropertyPlaceholderConfigurer properties() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ClassPathResource[] resources = new ClassPathResource[] { new ClassPathResource(
				"test-harvester-config.properties") };
		ppc.setLocations(resources);
		return ppc;
	}

	@Bean(name = "bufferSessionFactory")
	public LocalSessionFactoryBean bufferSessionFactory() {
		LocalSessionFactoryBean sb = new LocalSessionFactoryBean();
		sb.setDataSource(dataSource());
		sb.setAnnotatedClasses(new Class[] { OccurrenceRawModel.class,
				OccurrenceModel.class, ImportLogModel.class,
				ResourceContactModel.class });

		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty("hibernate.dialect", hibernateDialect);
		hibernateProperties.setProperty("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.setProperty("hibernate.default_schema",
				hibernateBufferSchema);
		hibernateProperties.setProperty("javax.persistence.validation.mode",
				"none");
		sb.setHibernateProperties(hibernateProperties);
		return sb;
	}
	
	@Bean
	public StepControllerIF stepController(){
		return new StepController();
	}
	
	@Bean
	public NodeStatusController nodeStatusController(){
		return new NodeStatusController();
	}

	@Bean
	public ItemTaskIF checkProcessingCompletenessTask() {
		return new CheckHarvestingCompletenessTask();
	}

	@Bean
	public ItemTaskIF cleanBufferTableTask() {
		return new CleanBufferTableTask();
	}

	@Bean
	public ItemTaskIF computeGISDataTask() {
		return null;
	}

	@Bean
	public ComputeUniqueValueJob computeUniqueValueJob() {
		return new ComputeUniqueValueJob();
	}

	@Bean
	public ItemTaskIF computeUniqueValueTask() {
		return new ComputeUniqueValueTask();
	}

	@Bean(name = "datasource")
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				// comes from lib project
				.addScript("classpath:h2/h2setup.sql")
				// those 2 scripts are loaded from canadensys-data-access
				.addScript("/script/occurrence/create_occurrence_tables.sql")
				.addScript("/script/occurrence/create_occurrence_tables_buffer_schema.sql")
				.build();
	}

	@Bean
	public ItemReaderIF<Eml> dwcaEmlReader() {
		return new DwcaEmlReader();
	}

	// ---READER wiring---
	@Bean
	public ItemReaderIF<OccurrenceRawModel> dwcItemReader() {
		return new DwcaItemReader();
	}

	@Bean
	public ItemTaskIF getResourceInfoTask() {
		return null;
	}

	// ---Config---
	@Bean
	public HarvesterConfigIF harvesterConfig() {
		HarvesterConfig hc = new HarvesterConfig();
		hc.setIptRssAddress(iptRssAddress);
		return hc;
	}
	
	//---DAO---
	@Bean
	public IPTFeedDAO iptFeedDAO(){
		return null;
	}
	@Bean
	public ResourceDAO resourceDAO(){
		return new HibernateResourceDAO();
	}
	@Bean
	public ImportLogDAO importLogDAO(){
		return null;
	}
	@Bean
	public ResourceStatusNotifierIF resourceStatusNotifierIF(){
		return null;
	}

	// ---VIEW MODEL---
	@Bean
	public HarvesterViewModel harvesterViewModel() {
		HarvesterViewModel hvm = new HarvesterViewModel();
		hvm.setDatabaseLocation(dbUrl);
		return hvm;
	}

	@Bean(name = "bufferTransactionManager")
	public HibernateTransactionManager hibernateTransactionManager() {
		HibernateTransactionManager htmgr = new HibernateTransactionManager();
		htmgr.setSessionFactory(bufferSessionFactory().getObject());
		return htmgr;
	}

	// ---JOB---
	@Bean
	public ImportDwcaJob importDwcaJob() {
		return new ImportDwcaJob();
	}

	// ---TASK wiring---

	@Bean(name = "insertResourceContactStep")
	public StepIF insertResourceContactStep() {
		return new InsertResourceContactStep();
	}

	/**
	 * Always return a new instance. We do not want to share JMS Writer
	 * instance.
	 * 
	 * @return
	 */
	@Bean
	@Scope("prototype")
	public JMSWriter jmsWriter() {
		return new JMSWriter(jmsBrokerUrl);
	}

	// ---PROCESSOR wiring---
	@Bean(name = "lineProcessor")
	public ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor() {
		DwcaLineProcessor dwcaLineProcessor = new DwcaLineProcessor();
		dwcaLineProcessor.setIdGenerationSQL(idGenerationSQL);
		return dwcaLineProcessor;
	}

	@Bean
	public MoveToPublicSchemaJob moveToPublicSchemaJob() {
		return new MoveToPublicSchemaJob();
	}

	@Bean(name = "occurrenceProcessor")
	public ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> occurrenceProcessor() {
		return new OccurrenceProcessor();
	}

	@Bean(name = "occurrenceWriter")
	public ItemWriterIF<OccurrenceModel> occurrenceWriter() {
		return new OccurrenceHibernateWriter();
	}

	@Bean
	public ItemTaskIF prepareDwcaTask() {
		return new PrepareDwcaTask();
	}

	@Bean(name = "processInsertOccurrenceStep")
	public StepIF processInsertOccurrenceStep() {
		return new ProcessInsertOccurrenceStep();
	}

	@Bean(name = "publicTransactionManager")
	public HibernateTransactionManager publicHibernateTransactionManager() {
		HibernateTransactionManager htmgr = new HibernateTransactionManager();
		htmgr.setSessionFactory(publicSessionFactory().getObject());
		return htmgr;
	}

	@Bean(name = {"publicSessionFactory","sessionFactory"})
	public LocalSessionFactoryBean publicSessionFactory() {
		LocalSessionFactoryBean sb = new LocalSessionFactoryBean();
		sb.setDataSource(dataSource());
		sb.setAnnotatedClasses(new Class[] { OccurrenceRawModel.class,
				OccurrenceModel.class, ImportLogModel.class,
				ResourceModel.class });

		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty("hibernate.dialect", hibernateDialect);
		hibernateProperties.setProperty("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.setProperty("javax.persistence.validation.mode",
				"none");
		sb.setHibernateProperties(hibernateProperties);
		return sb;
	}

	// ---WRITER wiring---
	@Bean(name = "rawOccurrenceWriter")
	public ItemWriterIF<OccurrenceRawModel> rawOccurrenceWriter() {
		return new RawOccurrenceHibernateWriter();
	}

	@Bean
	public ItemTaskIF recordImportTask() {
		return new RecordImportTask();
	}

	@Bean
	public ItemTaskIF replaceOldOccurrenceTask() {
		return new ReplaceOldOccurrenceTask();
	}

	@Bean(name = "resourceContactWriter")
	public ItemWriterIF<ResourceContactModel> resourceContactHibernateWriter() {
		return new ResourceContactHibernateWriter();
	}

	@Bean(name = "resourceContactProcessor")
	public ItemProcessorIF<Eml, ResourceContactModel> resourceContactProcessor() {
		return new ResourceContactProcessor();
	}

	@Bean(name = "streamDwcContentStep")
	public StepIF streamDwcContentStep() {
		return new StreamDwcContentStep();
	}

	// ---STEP---
	@Bean(name = "streamEmlContentStep")
	public StepIF streamEmlContentStep() {
		return new StreamEmlContentStep();
	}

	@Bean
	public JMSControlConsumer errorReceiver(){
		return new JMSControlConsumer(jmsBrokerUrl);
	}
	@Bean
	public JMSControlProducer errorReporter(){
		return null;
	}
	
	@Bean(name="currentVersion")
	public String currentVersion() {
		return "test-version";
	}
}
