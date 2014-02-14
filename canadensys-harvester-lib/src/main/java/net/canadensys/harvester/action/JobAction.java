package net.canadensys.harvester.action;

/**
 * Job action is an action executed by a Job (e.g. step, task).
 * @author canadensys
 *
 */
public interface JobAction {
	
	/**
	 * Get step string title use to display what is currently running.
	 */
	public String getTitle();
	
	//not ready yet
	//public void accept(JobActionVisitor visitor);

}
