package net.canadensys.harvester.jms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import net.canadensys.harvester.message.ProcessingMessageIF;
import net.canadensys.harvester.occurrence.message.DefaultMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Java Messaging System message consumer.
 * The routing of the message is done using the getMessageClass() of JMSConsumerMessageHandler.
 * For DefaultMessage, the getMsgHandlerClass() will also be used to find the proper handler and another one will be added soon.
 * @author canadensys
 *
 */
public class JMSConsumer{
	private static final Logger LOGGER = Logger.getLogger(JMSConsumer.class);
	private static final int DEFAUT_PREFETCH_QUEUE = 100;
	
	public String brokerURL;
	private boolean isOpen = false;
	
	private Connection connection;
	private MessageConsumer consumer;
	
	private List<JMSConsumerMessageHandlerIF> registeredHandlers;
	
	//Jackson Mapper to map JSON into Java object
	private ObjectMapper om;
	
	public JMSConsumer(String brokerURL){
		this.brokerURL = brokerURL;
		registeredHandlers = new ArrayList<JMSConsumerMessageHandlerIF>();
	}
	
	public void setBrokerURL(String brokerURL){
		if(isOpen){
			throw new IllegalStateException("Can not set broker URL if the connection is started.");
		}
		this.brokerURL = brokerURL;
	}
	
	public String getBrokerUrl(){
		return brokerURL;
	}
	
	/**
	 * Register a handler to notify when we receive a message
	 * @param handler
	 */
	public void registerHandler(JMSConsumerMessageHandlerIF handler){
		registeredHandlers.add(handler);
	}
	
	public void open() {
		om = new ObjectMapper();
		BasicConfigurator.configure();
		// Getting JMS connection from the server
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerURL);
		
		ActiveMQPrefetchPolicy app = new ActiveMQPrefetchPolicy();
		app.setQueuePrefetch(DEFAUT_PREFETCH_QUEUE);
		connectionFactory.setPrefetchPolicy(app);
		
		try{
			connection = connectionFactory.createConnection();
			connection.start();
	
			// Creating session for sending messages
			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);
	
			JMSMessageListener msgListener = new JMSMessageListener();
			
			// Get the queue
			Queue queue = session.createQueue(JMSProducer.QUEUE_NAME);
	
			// MessageConsumer is used for receiving (consuming) messages
			consumer = session.createConsumer(queue);
			consumer.setMessageListener(msgListener);
		}
		catch(JMSException jmsEx){
			LOGGER.fatal("Can not initialize JMSConsumer", jmsEx);
		}
	}

	public void close() {
		try {
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private class JMSMessageListener implements MessageListener{
		@Override
		public void onMessage(Message message) {
			// Producer sent us a TextMessage
			// so we must cast to it to get access to its .getText()
			// method.
			if (message instanceof TextMessage) {
				TextMessage msg = (TextMessage) message;
				JsonNode rootObj;
				try {
					Class<?> msgClass = Class.forName(ObjectUtils.defaultIfNull(msg.getStringProperty("MessageClass"), Object.class.getCanonicalName()));
					//validate if we can instantiate
					for(JMSConsumerMessageHandlerIF currMsgHandler : registeredHandlers){
						if(DefaultMessage.class.equals(msgClass)){
							DefaultMessage dmsg = om.readValue(msg.getText(), DefaultMessage.class);
							if(currMsgHandler.getClass().equals(dmsg.getMsgHandlerClass())){
								//since the content is defined as an Object, we need to explicitly rebuild it
								//TODO write a DefaultMessage deserializer that would handle that
								rootObj= om.readTree(msg.getText());
								
								//if the received type is a generic
								if(dmsg.getContentClassGeneric() == null){
									dmsg.setContent(om.readValue(rootObj.get("content").toString(),dmsg.getContentClass()));
								}
								else{
									JavaType type = om.getTypeFactory().constructParametricType(dmsg.getContentClass(), dmsg.getContentClassGeneric());
									dmsg.setContent(om.readValue(rootObj.get("content").toString(),type));
								}
								
								//TODO use contentClass to route to the right handler (generic handlers may be there more than once)
								currMsgHandler.handleMessage(dmsg);
								break;
							}
						}
						else{
							if(currMsgHandler.getMessageClass().equals(msgClass)){
								ProcessingMessageIF processingMessage = (ProcessingMessageIF)om.readValue(msg.getText(), msgClass);
								if(!currMsgHandler.handleMessage(processingMessage)){
									//throw new RuntimeException("Error while handling the message");
								}
								break;
							}
						}
						
						//TODO : add support for ControlMessageIF
						//TODO : raise error if no handler can process it
					}
				} catch (JMSException e) {
					LOGGER.fatal("Can not consume message ", e);
				} catch (ClassNotFoundException e) {
					LOGGER.fatal("Can not consume message ", e);
				} catch (JsonParseException e) {
					LOGGER.fatal("Can not consume message ", e);
				} catch (JsonMappingException e) {
					LOGGER.fatal("Can not consume message ", e);
				} catch (IOException e) {
					LOGGER.fatal("Can not consume message ", e);
				}
			}
		}
	}
}
