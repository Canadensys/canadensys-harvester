package net.canadensys.harvester.occurrence.message;

import net.canadensys.harvester.message.ProcessingMessageIF;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Generic message implementation.
 * @author canadensys
 *
 */
public class DefaultMessage implements ProcessingMessageIF{
	
	private String timestamp;
	
	//Which class should handle this message
	private Class<?> msgHandlerClass;
	
	private Object content;
	private Class<?> contentClass;
	private Class<?> contentClassGeneric;
	
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String when) {
		this.timestamp = when;
	}
	
	public Class<?> getMsgHandlerClass() {
		return msgHandlerClass;
	}
	public void setMsgHandlerClass(Class<?> msgHandlerClass) {
		this.msgHandlerClass = msgHandlerClass;
	}
	
	public Object getContent() {
		return content;
	}
	public void setContent(Object content) {
		this.content = content;
	}
	
	public Class<?> getContentClass() {
		return contentClass;
	}
	public void setContentClass(Class<?> contentClass) {
		this.contentClass = contentClass;
	}
	
	public Class<?> getContentClassGeneric() {
		return contentClassGeneric;
	}
	/**
	 * Set the class of the content when the content is a generic. (e.g. List<>)
	 * @param contentClassGeneric
	 */
	public void setContentClassGeneric(Class<?> contentClassGeneric) {
		this.contentClassGeneric = contentClassGeneric;
	}
	
	@Override
	public String toString(){
		return new ToStringBuilder(this).
			       append("timestamp", timestamp).
			       append("msgHandlerClass", msgHandlerClass).
			       append("contentClass", contentClass).
			       append("content", content).
			       toString();
	}
}
