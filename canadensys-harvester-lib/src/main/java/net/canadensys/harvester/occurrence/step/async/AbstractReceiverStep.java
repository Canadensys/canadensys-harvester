package net.canadensys.harvester.occurrence.step.async;

import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.StepResult;

public abstract class AbstractReceiverStep implements StepIF {
	@Override
	public final StepResult doStep(){
		//no op;
		return new StepResult(0);
	}
}
