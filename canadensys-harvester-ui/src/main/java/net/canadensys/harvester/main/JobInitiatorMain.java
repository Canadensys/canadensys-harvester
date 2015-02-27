package net.canadensys.harvester.main;

import net.canadensys.harvester.config.UIConfig;
import net.canadensys.harvester.jms.JMSProducer;
import net.canadensys.harvester.occurrence.view.OccurrenceHarvesterMainView;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class JobInitiatorMain{
	
	@Autowired
	private OccurrenceHarvesterMainView occurrenceHarvesterMainView;
	
	@Autowired
	private JMSProducer jmsProducer;

	public void initiateApp(String brokerURL){
		// check if we need to set a new broker URL
		if (StringUtils.isNotBlank(brokerURL)) {
			jmsProducer.setBrokerURL(brokerURL);
		}
		occurrenceHarvesterMainView.initView();
	}
	
	/**
	 * JobInitiator Entry point
	 * @param args
	 */
	public static void main(String newBrokerIp) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(UIConfig.class);
		JobInitiatorMain jim = ctx.getBean(JobInitiatorMain.class);
		jim.initiateApp(newBrokerIp);
	}
}
