package org.distropia.server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;


public class MaintenanceList {
	
	protected List<MaintenanceItem> items = new ArrayList<MaintenanceList.MaintenanceItem>();
	protected java.util.Timer maintencanceTimer = null;
	
	private static class MaintenanceItem extends WeakReference<Maintenanceable> {
		private int interval;
		private long nextMaintenance;
		
		public MaintenanceItem(int interval, Maintenanceable maintenanceable) {
			super( maintenanceable);
			this.interval = interval;
			this.nextMaintenance = System.currentTimeMillis() + interval;
		}

		public long getNextMaintenance() {
			return nextMaintenance;
		}

		public void setNextMaintenance(long nextMaintenance) {
			this.nextMaintenance = nextMaintenance;
		}

		public int getInterval() {
			return interval;
		}

	}

	public void close(){
		if (maintencanceTimer != null){
			maintencanceTimer.cancel();
			maintencanceTimer = null;
		}
	}
	
	private void maintenance() {
		List<MaintenanceItem> doMaintenanceAt = new ArrayList<MaintenanceList.MaintenanceItem>(); 
		synchronized (items) {
			doMaintenanceAt.addAll( items);
		}
		long currentMillis = System.currentTimeMillis();
		for(MaintenanceItem maintenanceItem: doMaintenanceAt){
			if (maintenanceItem.getNextMaintenance()< currentMillis)
			{
				try {
					Maintenanceable maintenanceable = maintenanceItem.get();
					if (maintenanceable == null){
						synchronized (items) {
							items.remove( maintenanceItem);
						}						
					}
					else maintenanceable.maintenance();
				} catch (Exception e) {
					synchronized (items) {
						items.remove( maintenanceItem);
					}
				}
				maintenanceItem.setNextMaintenance( currentMillis + maintenanceItem.getInterval());
			}
		}
	}
	
	public MaintenanceList() {
		super();
		TimerTask task = new TimerTask() 
		{			
			@Override
			public void run() {
				maintenance();
			}
		};
		maintencanceTimer = new java.util.Timer("maintenanceTimer");
		maintencanceTimer.schedule(task, 11000, 11000); // TODO: calculate next timer on next event
	}

	public void addWithWeakReference( Maintenanceable maintenanceable, int interval){
		synchronized (items) {
			for( MaintenanceItem maintenanceItem: items)
				if (maintenanceItem.get() == maintenanceable) return;
			items.add( new MaintenanceItem(interval, maintenanceable));
		}
	}
	
	public void remove( Maintenanceable maintenanceable){
		synchronized (items) {
			for(int index=items.size()-1; index>=0; index--)
				if(items.get(index).get() == maintenanceable) 
					items.remove(index);			
		}
	}
}
