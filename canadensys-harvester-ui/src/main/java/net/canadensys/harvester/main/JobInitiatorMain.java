package net.canadensys.harvester.main;

import net.canadensys.harvester.config.ProcessingConfig;
import net.canadensys.harvester.occurrence.view.OccurrenceHarvesterMainView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class JobInitiatorMain{
	
	@Autowired
	private OccurrenceHarvesterMainView occurrenceHarvesterMainView;

	public void initiateApp(){
		occurrenceHarvesterMainView.initView();
	}
	
	/**
	 * JobInitiator Entry point
	 * @param args
	 */
	public static void main() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ProcessingConfig.class);
		JobInitiatorMain jim = ctx.getBean(JobInitiatorMain.class);
		jim.initiateApp();
	}
}