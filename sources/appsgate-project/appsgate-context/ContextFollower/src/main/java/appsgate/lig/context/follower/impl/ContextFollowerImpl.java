package appsgate.lig.context.follower.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.context.follower.listeners.CoreListener;
import appsgate.lig.context.follower.spec.ContextFollowerSpec;
import appsgate.lig.core.object.messages.NotificationMsg;

/**
 * This class is use to allow other components to subscribe for specific
 * triggering event or request for system or devices state
 * 
 * @author Cédric Gérard
 * @since May 28, 2013
 * @version 1.0.0
 */
public class ContextFollowerImpl implements ContextFollowerSpec {

	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory
			.getLogger(ContextFollowerImpl.class);
	
	private HashMap<Entry, ArrayList<CoreListener>> eventsListeners = new HashMap<Entry, ArrayList<CoreListener>>();

	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.debug("The context follower ApAM component has been initialized");
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.debug("The context follower ApAM component has been stopped");
	}

	@Override
	public void addListener(CoreListener coreListener) {
		logger.debug("Adding a listener...");
		Entry eventKey = new Entry(coreListener.getObjectId(), coreListener.getEvent(), coreListener.getValue());
		
		Set<Entry> keys = eventsListeners.keySet();
		Iterator<Entry> keysIt = keys.iterator();
		boolean added = false;
		
		while(keysIt.hasNext() && !added) {
			Entry key = keysIt.next();
			if(eventKey.equals(key)) {
				eventsListeners.get(key).add(coreListener);
				logger.debug("Add follower to existing listener list");
				added = true;
			}
		}
		
		if(!added) {
			ArrayList<CoreListener> coreListenerList = new ArrayList<CoreListener>();
			coreListenerList.add(coreListener);
			eventsListeners.put(eventKey, coreListenerList);
			logger.debug("Add new event follower list");
		}
	}

	@Override
	public void deleteListener(CoreListener coreListener) {
		logger.debug("Deleting all listeners...");
		Entry eventKey = new Entry(coreListener.getObjectId(), coreListener.getEvent(), coreListener.getValue());
		
		Set<Entry> keys = eventsListeners.keySet();
		Iterator<Entry> keysIt = keys.iterator();
		
		while(keysIt.hasNext()) {
			Entry key = keysIt.next();
			if(eventKey.equals(key)) {
				ArrayList<CoreListener> coreListenerList = eventsListeners.get(key);
				coreListenerList.remove(coreListener);
				if(coreListenerList.isEmpty()) {
					eventsListeners.remove(key);
				}
				break;
			}
		}

		logger.debug("Listeners deleted");
	}

	/**
	 * Called by ApAM when Notification message comes
	 * 
	 * @param notif
	 *            the notification message from ApAM
	 */
	public void gotNotification(NotificationMsg notif) {
		try {
			logger.debug("Event message receive, " + notif.JSONize());
			JSONObject event = notif.JSONize();
			
			//TODO delete this exception when the notification
			//mechanism will be totally define
			try{
				event.getString("objectId");
			}catch(JSONException e) {
				return;
			}
				
			Entry eventKey = new Entry(event.getString("objectId"), event.getString("varName"), event.getString("value"));
			
			Set<Entry> keys = eventsListeners.keySet();
			Iterator<Entry> keysIt = keys.iterator();
			
			while(keysIt.hasNext()) {
				Entry key = keysIt.next();
				if(eventKey.equals(key)) {
					logger.debug("Event is followed, retreiving listeners...");
					ArrayList<CoreListener> coreListenerList = eventsListeners.get(key);
					Iterator<CoreListener> it = coreListenerList.iterator();
					while(it.hasNext()) {
						it.next().notifyEvent();
						logger.debug("Notify listener.");
					}
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Inner class use to create an event notify condition
	 * 
	 * @author Cédric Gérard
	 * @since May 28, 2013
	 * @version 1.0.0
	 */
	private class Entry {

		/**
		 * Identifier of the source f the event
		 */
		private String objectId;
		
		/**
		 * The variable name to follow
		 */
		private String varName;
		
		/**
		 * The threshold value
		 */
		private String value;

		/**
		 * Constructor for an Entry
		 * @param objectId
		 * @param varName
		 * @param value
		 */
		public Entry(String objectId, String varName, String value) {
			this.objectId = objectId;
			this.varName = varName;
			this.value = value;
		}

		public String getObjectId() {
			return objectId;
		}

		public String getVarName() {
			return varName;
		}

		public String getValue() {
			return value;
		}

		/**
		 * Override the equals method to compare two entry together but 
		 * compare their contents
		 */
		@Override
		public boolean equals(Object eventEntry) {
			if (eventEntry instanceof Entry) {
				Entry entry = (Entry) eventEntry;
				return (entry.getObjectId().contentEquals(objectId)
						&& entry.getVarName().contentEquals(varName) && entry
						.getValue().contentEquals(value));
			}
			return false;
		}

	}
	

}
