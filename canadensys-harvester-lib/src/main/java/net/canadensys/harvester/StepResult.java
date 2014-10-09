package net.canadensys.harvester;

public class StepResult {
	private final int numberOfRecords;
	
	public StepResult(int numberOfRecords){
		this.numberOfRecords = numberOfRecords;
	}
	
	/**
	 * Get the number of records handled by this step
	 * @return
	 */
	public int getNumberOfRecord(){
		return numberOfRecords;
	}

}
