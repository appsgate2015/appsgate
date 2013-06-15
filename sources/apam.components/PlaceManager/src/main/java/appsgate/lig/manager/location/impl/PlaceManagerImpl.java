package appsgate.lig.manager.location.impl;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.logical.object.messages.NotificationMsg;
import appsgate.lig.logical.object.spec.AbstractObjectSpec;
import appsgate.lig.manager.location.messages.MoveObjectNotification;
import appsgate.lig.manager.location.messages.PlaceManagerNotification;
import appsgate.lig.manager.location.spec.PlaceManagerSpec;

/**
 * This ApAM component is used to maintain location information for any object
 * in the smart habitat.
 * 
 * @author Cédric Gérard
 * @version 1.0.0
 * @since February 26, 2013
 * 
 */
public class PlaceManagerImpl implements PlaceManagerSpec {

	/**
	 * class logger member
	 */
	private static Logger logger = LoggerFactory.getLogger(PlaceManagerImpl.class);

	/**
	 * This is the hash map to match the location of devices.
	 */
	private HashMap<String, SymbolicLocation> locationObjectsMap;

	/**
	 * Called by ApAM when all dependencies are available
	 */
	public void newInst() {
		logger.info("Place manager started.");
		locationObjectsMap = new HashMap<String, SymbolicLocation>();
	}

	/**
	 * Called by ApAM when the component is not available
	 */
	public void deleteInst() {
		logger.info("Removing place manager...");
	}

	/**
	 * Add a new place to the hash map.
	 * 
	 * @param locationId
	 *            , the id of the new location
	 * @param name
	 *            , the place name
	 */
	public synchronized void addPlace(String locationId, String name) {
		if (!locationObjectsMap.containsKey(locationId)) {
			locationObjectsMap.put(locationId, new SymbolicLocation(locationId, name));
			notifyPlace(locationId, name, 0);
		}
	}

	/**
	 * Remove the designed place
	 * 
	 * @param locationId
	 *            , the place to removed
	 */
	public synchronized void removePlace(String locationId) {
		SymbolicLocation loc = locationObjectsMap.get(locationId);
		loc.removeAll();
		locationObjectsMap.remove(locationId);
	}

	/**
	 * Add an object to the designed place.
	 * 
	 * @param obj
	 *            , the object to locate
	 * @param locationId
	 *            , the target place
	 */
	private synchronized void addObject(AbstractObjectSpec obj,
			String locationId) {
		SymbolicLocation loc = locationObjectsMap.get(locationId);
		loc.addObject(obj);
	}

	/**
	 * Move the object obj from oldPlace to the newPlace.
	 * 
	 * @param obj
	 *            , the object to move.
	 * @param oldPlaceID
	 *            , the former place where obj was located
	 * @param newPlaceID
	 *            , the new place where obj is locate.
	 */
	public synchronized void moveObject(AbstractObjectSpec obj,
			String oldPlaceID, String newPlaceID) {


		if (Integer.getInteger(oldPlaceID) == -1) {
			addObject(obj, newPlaceID);
		} else {
			SymbolicLocation oldLoc = locationObjectsMap.get(oldPlaceID);
			SymbolicLocation newLoc = locationObjectsMap.get(newPlaceID);
			oldLoc.removeObject(obj);
			newLoc.addObject(obj);
		}

		notifyMove(oldPlaceID, newPlaceID, obj);
	}

	/**
	 * Rename the location
	 */
	public synchronized void renameLocation(String locationId, String newName) {
		SymbolicLocation loc = locationObjectsMap.get(locationId);
		loc.setName(newName);
		
		notifyPlace(locationId, newName, 1);
	}

	/**
	 * Get the JSON tab of all location
	 * 
	 * @return all the location as a JSONArray
	 */
	public synchronized JSONArray getJSONLocations() {
		Iterator<SymbolicLocation> locations = locationObjectsMap.values().iterator();
		JSONArray jsonLocationList = new JSONArray();
		SymbolicLocation loc;

		while (locations.hasNext()) {
			loc = locations.next();
			jsonLocationList.put(loc.getDescription());
		}

		return jsonLocationList;
	}

	/**
	 * Use to notify that a AbstractObject has moved.
	 * 
	 * @param oldLocationId the former AbstractObject location
	 * @param newLocationId the new AbstractObject location
	 * @param object the AbstractObject which has been moved.
	 */
	private void notifyMove(String oldLocationId, String newLocationId, AbstractObjectSpec object) {
		notifyChanged(new MoveObjectNotification(oldLocationId, newLocationId, object));
	}
	
	/**
	 * Use to notify that new place has been created or has changed
	 * 
	 * @param locationId the location identifier
	 * @param locationName the user name of this location
	 * @param type indicate if this notification is a place creation (0) or an update (1)
	 */
	private void notifyPlace(String locationId, String locationName, int type) {
		notifyChanged(new PlaceManagerNotification(locationId, locationName, type));
	}
	
	/**
	 * This method notify ApAM that a new notification message has been produced.
	 * @param notif the notification message to send.
	 * @return nothing it just notify ApAM.
	 */
	public NotificationMsg notifyChanged (NotificationMsg notif) {
		logger.debug("Location Notify: "+ notif);
		return notif;
	}

	/**
	 * Get the location identifier of the core object give in parameter.
	 * @param the object from which get the location
	 */
	@Override
	public String getCoreObjectLocationId(AbstractObjectSpec obj) {
		Iterator<SymbolicLocation>  it = locationObjectsMap.values().iterator();
		
		SymbolicLocation loc = null;
		boolean found = false;
		
		while(it.hasNext() && !found) {
			
			loc = it.next();
			if(loc.isHere(obj)) {
				found = true;
			}
		}
		
		if(found) {
			return loc.getId();
		} else {
			return "-1";
		}
	}

}
