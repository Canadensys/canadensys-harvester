package net.canadensys.harvester.occurrence.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.canadensys.harvester.ItemProgressListenerIF;
import net.canadensys.harvester.LongRunningTaskIF;
import net.canadensys.harvester.exception.TaskExecutionException;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Task to check and wait for processing completion.
 * Notification will be sent using the ItemProgressListenerIF listener.
 * @author canadensys
 *
 */
public class CheckHarvestingCompletenessTask implements LongRunningTaskIF{

	private static final int MAX_WAITING_SECONDS = 10;
	private static final Logger LOGGER = Logger.getLogger(CheckHarvestingCompletenessTask.class);
	
	@Autowired
	@Qualifier(value="bufferSessionFactory")
	private SessionFactory sessionFactory;
	
	private List<ItemProgressListenerIF> itemListenerList;
	private int secondsWaiting = 0;
	private AtomicBoolean taskCanceled = new AtomicBoolean(false);
	
	/**
	 * @param sharedParameters get BatchConstant.NUMBER_OF_RECORDS and BatchConstant.DWCA_IDENTIFIER_TAG
	 */
	@Override
	public void execute(Map<SharedParameterEnum, Object> sharedParameters) {
		final Integer numberOfRecords = (Integer)sharedParameters.get(SharedParameterEnum.NUMBER_OF_RECORDS);
		final String datasetShortname = (String)sharedParameters.get(SharedParameterEnum.DATASET_SHORTNAME);
		if(numberOfRecords == null || datasetShortname == null){
			LOGGER.fatal("Misconfigured task : needs numberOfRecords, datasetShortname");
			throw new TaskExecutionException("Misconfigured task");
		}
		
		Thread checkThread = new Thread(new Runnable() {
			private int previousCount = 0;
			@Override
			public void run() {
				Session session = sessionFactory.openSession();
				SQLQuery query = session.createSQLQuery("SELECT count(*) FROM buffer.occurrence_raw WHERE sourcefileid=?");
				query.setString(0, datasetShortname);
				try{
					Number currNumberOfResult = (Number)query.uniqueResult();
					while(!taskCanceled.get() && (currNumberOfResult.intValue() < numberOfRecords)){
						currNumberOfResult = (Number)query.uniqueResult();
						//make sure we don't get stuck here is something goes wrong with the clients
						if(previousCount == currNumberOfResult.intValue()){
							secondsWaiting++;
							if(secondsWaiting == MAX_WAITING_SECONDS){
								break;
							}
						}
						else{
							secondsWaiting = 0;
						}
						previousCount = currNumberOfResult.intValue();
						notifyListeners("occurrence_raw",currNumberOfResult.intValue(),numberOfRecords);
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
				}catch(HibernateException hEx){
					notifyListenersOnFailure(hEx);
				}
				session.close();
				
				if(taskCanceled.get()){
					notifyListenersOnCancel();
				}
				else{
					if(secondsWaiting < MAX_WAITING_SECONDS){
						notifyListenersOnSuccess();
					}
					else{
						notifyListenersOnFailure(new TimeoutException("No progress made in more than " + MAX_WAITING_SECONDS + " seconds."));
					}
				}
			}
		});
		checkThread.start();
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private void notifyListeners(String context,int current,int total){
		if(itemListenerList != null){
			for(ItemProgressListenerIF currListener : itemListenerList){
				currListener.onProgress(context,current, total);
			}
		}
	}
	private void notifyListenersOnSuccess(){
		if(itemListenerList != null){
			for(ItemProgressListenerIF currListener : itemListenerList){
				currListener.onSuccess();
			}
		}
	}
	private void notifyListenersOnCancel(){
		if(itemListenerList != null){
			for(ItemProgressListenerIF currListener : itemListenerList){
				currListener.onCancel();
			}
		}
	}
	private void notifyListenersOnFailure(Throwable t){
		if(itemListenerList != null){
			for(ItemProgressListenerIF currListener : itemListenerList){
				currListener.onError(t);
			}
		}
	}
	
	public void addItemProgressListenerIF(ItemProgressListenerIF listener){
		if(itemListenerList == null){
			itemListenerList = new ArrayList<ItemProgressListenerIF>();
		}
		itemListenerList.add(listener);
	}

	@Override
	public void cancel() {
		taskCanceled.set(true);
	}
	
	@Override
	public String getTitle() {
		return "Waiting for completion";
	}
}
