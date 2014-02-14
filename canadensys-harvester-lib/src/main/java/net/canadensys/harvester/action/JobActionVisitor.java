package net.canadensys.harvester.action;

import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.ProcessingStepIF;

/**
 * Used to execute an action on a specific implementation of JobAction.
 * @author canadensys
 *
 */
public interface JobActionVisitor {
	
	void visit(ProcessingStepIF step);
	void visit(ItemTaskIF task);

}
