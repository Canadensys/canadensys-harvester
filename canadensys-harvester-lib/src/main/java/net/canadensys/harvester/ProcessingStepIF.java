package net.canadensys.harvester;

import java.util.Map;

import net.canadensys.harvester.action.JobAction;
import net.canadensys.harvester.occurrence.SharedParameterEnum;

/**
 * A step includes a reader, a processor and a writer. Those are not enforced since the could be used in different ways (inheritance, composition, async messages)
 * @author canadensys
 *
 */
public interface ProcessingStepIF extends JobAction{
	
	/**
	 * Check that the step is ready to go.
	 * Initiate inner components
	 * @param sharedParameters if needed
	 * @throws IllegalStateException
	 */
	public void preStep(Map<SharedParameterEnum,Object> sharedParameters) throws IllegalStateException;
	
	/**
	 * The step is executed.
	 * Clean up phase.
	 */
	public void postStep();
	
	/**
	 * Execute the step.
	 */
	public void doStep();

}
