package net.canadensys.harvester.main;

import java.util.ArrayList;
import java.util.List;

import net.canadensys.dataportal.occurrence.dao.OccurrenceExtensionDAO;
import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.config.ProcessingNodeConfig;
import net.canadensys.harvester.controller.VersionController;
import net.canadensys.harvester.jms.JMSConsumer;
import net.canadensys.harvester.jms.JMSConsumerMessageHandlerIF;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Processing node main class.
 * 
 * @author canadensys
 * 
 */
@Component
public class ProcessingNodeMain {
	// TODO move port to config
	private static final String IP = "tcp://%s:61616";

	@Autowired
	private JMSConsumer jmsConsumer;
	
	@Autowired
	private VersionController versionController;
	
	@Autowired
	private ProcessingNodeConfig nodeConfig;
	
	@Autowired
	private StepIF processInsertOccurrenceStep;

	@Autowired
	private StepIF insertResourceContactStep;
	
	@Autowired
	OccurrenceExtensionDAO occurrenceExtensionDAO;
	
	@Autowired
	private StepIF asyncManageOccurrenceExtensionStep;

	private final List<StepIF> registeredSteps;
	private final List<JMSConsumerMessageHandlerIF> registeredMsgHandlers;

	public ProcessingNodeMain() {
		registeredMsgHandlers = new ArrayList<JMSConsumerMessageHandlerIF>();
		registeredSteps = new ArrayList<StepIF>();
	}

	/**
	 * Initiate the node and start listening for messages.
	 * 
	 * @param brokerURL
	 *            a new broker URL or null to use the default one
	 * @param additionalMessageHandler
	 *            user defined message handler (optional)
	 */
	public <T extends JMSConsumerMessageHandlerIF & StepIF> void initiate(
			String brokerURL, List<T> additionalMessageHandler) {
		// check if we need to set a new broker URL
		if (StringUtils.isNotBlank(brokerURL)) {
			jmsConsumer.setBrokerURL(brokerURL);
		}

		System.out.println("Broker location : " + jmsConsumer.getBrokerUrl());
		System.out.println("Database location : " + nodeConfig.getDbUrl());

		// Declare step handlers (maybe this should be configurable?)
		registeredMsgHandlers.add((JMSConsumerMessageHandlerIF) processInsertOccurrenceStep);
		registeredSteps.add(processInsertOccurrenceStep);

		registeredMsgHandlers.add((JMSConsumerMessageHandlerIF) insertResourceContactStep);
		registeredSteps.add(insertResourceContactStep);
		
		registeredMsgHandlers.add((JMSConsumerMessageHandlerIF) asyncManageOccurrenceExtensionStep);
		registeredSteps.add(asyncManageOccurrenceExtensionStep);
		

		if (additionalMessageHandler != null) {
			registeredMsgHandlers.addAll(additionalMessageHandler);
			registeredSteps.addAll(additionalMessageHandler);
		}

		// Register all message handlers to the JMS consumer
		for (JMSConsumerMessageHandlerIF currMsgHandler : registeredMsgHandlers) {
			jmsConsumer.registerHandler(currMsgHandler);
		}

		// Then, init all the steps
		for (StepIF currStep : registeredSteps) {
			// due to the async behavior, we do not use any sharedParameters (at
			// least for now)
			currStep.preStep(null);
		}

		// TODO register postStep calls

		jmsConsumer.open();
		versionController.start();
	}

	/**
	 * Processing node entry point
	 * 
	 * @param newBrokerIp
	 */
	public static void main(String newBrokerIp) {
		String newBrokerUrl = null;
		if (StringUtils.isNotBlank(newBrokerIp)) {
			newBrokerUrl = String.format(IP, newBrokerIp);
		}

		// prepare to allow additional (user defined) messageHandler defined as
		// step
		List<MessageHandlerStep> additionalMessageHandler = null;
		System.out.println("Starting Harvester node ...");
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ProcessingNodeConfig.class);
		ProcessingNodeMain processingNodeBean = ctx
				.getBean(ProcessingNodeMain.class);
		processingNodeBean.initiate(newBrokerUrl, additionalMessageHandler);
	}

	/**
	 * Inner class to satisfy the 2 interfaces list of ProcessingNodeMain
	 * initiate method. TODO: Not very elegant, maybe define a real interface.
	 * 
	 * @author canadensys
	 * 
	 */
	private interface MessageHandlerStep extends JMSConsumerMessageHandlerIF,
			StepIF {
	}
}
