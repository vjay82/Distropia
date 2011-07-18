package org.distropia.server.communication.dht;

import java.io.Serializable;
import java.util.Date;

public class ItemWithCreationTime implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2142544344863478358L;
	Object object;
	long creationTime;
	
	
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public long getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}
	public ItemWithCreationTime(Object object) {
		super();
		this.object = object;
	}
	public static ItemWithCreationTime createWithTimeNow(Object object) {
		ItemWithCreationTime itemWithCreationTime = new ItemWithCreationTime(object);
		itemWithCreationTime.creationTime = getNow();
		return itemWithCreationTime;
	}
	public boolean isOlderThan( ItemWithCreationTime itemWithCreationTime)
	{
		return creationTime<itemWithCreationTime.creationTime;
	}
	public boolean isOlderThan( long time)
	{
		return creationTime<time;
	}
	public boolean isOlderThanMilliseconds( long time)
	{
		return creationTime< getNow() - time;
	}
	public boolean isOlderThanSeconds( long time)
	{
		return creationTime< getNow() - (time*1000);
	}
	public boolean isOlderThanMinutes( long time)
	{
		return creationTime< getNow() - (time*60000);
	}
	public static long getNow(){
		return (new Date()).getTime();
	}
	@Override
	public String toString() {
		if (object != null) return object.toString();
		return "no object set";
	}
	
}
