package net.canadensys.harvester;

/**
 * Interface used to be notified of the progress of a task.
 * @author cgendreau
 *
 */
public interface ItemProgressListenerIF {
	
	/**
	 * 
	 * @param context representing the context of the progress. Allowing to have the same listener on different context.
	 * @param current
	 * @param total
	 */
	public void onProgress(String context, int current, int total);
	
	public void onSuccess(String context);
	public void onCancel(String context);
	public void onError(String context, Throwable t);
}
