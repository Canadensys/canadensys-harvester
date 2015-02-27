package net.canadensys.harvester.controller;

import net.canadensys.harvester.config.ProcessingNodeConfig;
import net.canadensys.harvester.jms.control.JMSControlConsumer;
import net.canadensys.harvester.jms.control.JMSControlConsumerMessageHandlerIF;
import net.canadensys.harvester.message.ControlMessageIF;
import net.canadensys.harvester.message.control.VersionControlMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This controller will shut down the node if its version is not matching the manager version.
 * @author cgendreau
 *
 */
public class VersionController  implements JMSControlConsumerMessageHandlerIF {
	
	private static final Logger LOGGER = Logger.getLogger(VersionController.class);

	@Autowired
	private JMSControlConsumer controlMessageReceiver;
	
	@Autowired
	private ProcessingNodeConfig processingNodeConfig;

	@Override
	public Class<?> getMessageClass() {
		return VersionControlMessage.class;
	}

	@Override
	public boolean handleMessage(ControlMessageIF message) {
		VersionControlMessage versionCtrlMsg = (VersionControlMessage)message;
		if(!versionCtrlMsg.getCurrentVersion().equalsIgnoreCase(processingNodeConfig.getCurrentVersion())){
			LOGGER.fatal("Shutting down node. Requested version:" + versionCtrlMsg.getCurrentVersion() +", Current version:"+processingNodeConfig.getCurrentVersion());
			System.out.println("Shutting down node due to version mismatch with the manager.");
			System.exit(1);
		}
		return true;
	}

	public void start(){
		controlMessageReceiver.registerHandler(this);
		controlMessageReceiver.open();
	}

}
