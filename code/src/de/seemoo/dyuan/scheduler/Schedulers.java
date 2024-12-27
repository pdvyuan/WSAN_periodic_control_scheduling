package de.seemoo.dyuan.scheduler;

import de.seemoo.dyuan.scheduler.alice.AliceScheduler;
import de.seemoo.dyuan.scheduler.cllf.CLLFScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.RandomScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.busy_first.LST_MCFScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.edf.EDFScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.edzl.EDZLScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.epd.EPDScheduler;
import de.seemoo.dyuan.scheduler.dynamic_priority.lst.LSTScheduler;
import de.seemoo.dyuan.scheduler.fixed_priority.DMScheduler;
import de.seemoo.dyuan.scheduler.fixed_priority.PDMScheduler;
import de.seemoo.dyuan.scheduler.fixed_priority.RMScheduler;
import de.seemoo.dyuan.scheduler.tasa.TasaScheduler;

public interface Schedulers {
	
	public final static Class[] schedulers_classes = { 
//		OptimalBBScheduler.class, 
		

		RMScheduler.class, 
		
		DMScheduler.class, 
		
		PDMScheduler.class, 
		
		CLLFScheduler.class, 
		
		EDFScheduler.class,
		
		LSTScheduler.class, 
		
		EPDScheduler.class, 
		
//		PfairPFScheduler.class, PfairPD2Scheduler.class,
//		ERfairPFScheduler.class, ERfairPD2Scheduler.class, 
		
		EDZLScheduler.class,
		LST_MCFScheduler.class,
		AliceScheduler.class,
		TasaScheduler.class,
		RandomScheduler.class
		
	};
	
	public final static Class[] schedulers_to_comp = {
		LSTScheduler.class,
		LST_MCFScheduler.class
	};
	
	public final static Class best_scheduler_class = LST_MCFScheduler.class;
	
	public final static Class[] restricted_schedulers_classes = { 
//		OptimalBBScheduler.class, 
		CLLFScheduler.class, 

		RMScheduler.class, 
		
		DMScheduler.class, 
		
		EDFScheduler.class,
		LSTScheduler.class, PDMScheduler.class, EPDScheduler.class, 
		
		EDZLScheduler.class
		
	};

}
