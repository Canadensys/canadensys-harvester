package net.canadensys.harvester.occurrence.step.stream;

import java.util.ArrayList;
import java.util.List;

import net.canadensys.harvester.StepIF;
import net.canadensys.harvester.occurrence.step.async.AbstractReceiverStep;

/**
 * Asynchronous emitter step that will emit objects that will be handled by another class in a distributed environment.
 * After the all the objects have been emitted , the doStep method returns and notify the listener when the step reach completion.
 * @author cgendreau
 *
 */
public abstract class AbstractStreamStep implements StepIF {
	
	protected List<Class<? extends AbstractReceiverStep>> asyncReceivers;
		
	/**
	 * Add a the class responsible to receive and handle the emitted message.
	 * @param asyncReceiver
	 */
	protected void addAsyncReceiverStep(Class<? extends AbstractReceiverStep> asyncReceiver){
		if(asyncReceivers == null){
			asyncReceivers = new ArrayList<Class<? extends AbstractReceiverStep>>();
		}
		asyncReceivers.add(asyncReceiver);
	}

}
