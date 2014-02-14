package net.canadensys.harvester.occurrence.writer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.sql.DataSource;

import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.config.ProcessingConfigTest;
import net.canadensys.harvester.exception.WriterException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=ProcessingConfigTest.class, loader=AnnotationConfigContextLoader.class)
public class GenericHibernateWriterTest {
	
	@Autowired
	private ItemWriterIF<ResourceContactModel> genericResourceContactWriter;
	private JdbcTemplate jdbcTemplate;
	
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
	@Test
	public void testInsertContact(){
		ResourceContactModel testContact = new ResourceContactModel();
		testContact.setName("Test Contact");
		try {
			genericResourceContactWriter.write(testContact);
		} catch (WriterException e) {
			fail();
		}
		Number contactId = jdbcTemplate.queryForObject("SELECT id FROM buffer.resource_contact WHERE name ='Test Contact'", Number.class);
		assertNotNull(contactId);
	} 

}
