package net.canadensys.harvester.occurrence.mock.writer;

import java.util.ArrayList;
import java.util.List;

import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.exception.WriterException;
import net.canadensys.harvester.message.ProcessingMessageIF;

/**
 * Mock writer to replace JMS writer to simplify unit testing.
 * @author cgendreau
 *
 * @param <T>
 */
public class MockMessageWriter<T extends ProcessingMessageIF> implements ItemWriterIF<T>{

	private List<T> content = new ArrayList<T>();
	
	@Override
	public void openWriter() {
		//no op
	}

	@Override
	public void closeWriter() {
		// no op
	}

	@Override
	public void write(List<? extends T> elementList) throws WriterException {
		content.addAll(elementList);
	}

	@Override
	public void write(T element) throws WriterException {
		content.add(element);
	}
	
	public List<T> getContent(){
		return content;
	}

}
