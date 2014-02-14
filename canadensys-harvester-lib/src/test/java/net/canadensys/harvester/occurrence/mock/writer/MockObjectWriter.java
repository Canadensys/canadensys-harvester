package net.canadensys.harvester.occurrence.mock.writer;

import java.util.ArrayList;
import java.util.List;

import net.canadensys.harvester.ItemWriterIF;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Used in unit test to accumulate T objects
 * @author canadensys
 *
 * @param <T>
 */
public class MockObjectWriter<T> implements ItemWriterIF<T>{
	
	private List<T> content;
	
	private FutureCallback<Void> callback = null;
	private int numberOfElementBeforeCallback;
	
	/**
	 * For testing purpose
	 * Set a callback function to be called when X number of elements are written.
	 * @param callback
	 * @param numberOfElement
	 */
	public void addCallback(FutureCallback<Void> callback, int numberOfElement){
		this.callback = callback;
		numberOfElementBeforeCallback = numberOfElement;
	}
	
	@Override
	public void openWriter() {
		content = new ArrayList<T>();
	}

	@Override
	public void closeWriter() {}

	@Override
	public void write(List<? extends T> elementList) {
		content.addAll(elementList);
		if(callback != null){
			if(content.size() == numberOfElementBeforeCallback){
				callback.onSuccess(null);
			}
		}
	}

	@Override
	public void write(T element) {
		content.add(element);
		if(callback != null){
			if(content.size() == numberOfElementBeforeCallback){
				callback.onSuccess(null);
			}
		}
	}

	public List<T> getContent(){
		return content;
	}
}
