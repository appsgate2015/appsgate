package appsgate.lig.manager.place.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.context.history.services.DataBasePullService;
import appsgate.lig.context.history.services.DataBasePushService;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.manager.place.messages.MoveObjectNotification;
import appsgate.lig.manager.place.messages.PlaceManagerNotification;
import appsgate.lig.manager.place.spec.PlaceManagerSpec;

/**
 * This ApAM component is used to maintain place information for any object
 * in the smart habitat.
 * 
 * @author Cédric Gérard
 * @version 1.0.0
 * @since February 26, 2013
 * 
 * @see PlaceManagerSpec
 * 
 */
public class PlaceManagerImpl implements PlaceManagerSpec {

	/**
	 * class logger member
	 */
	private static Logger logger = LoggerFactory.getLogger(PlaceManagerImpl.class);

	/**
	 * This is the hash map to match the place of devices.
	 */
	private HashMap<String, SymbolicPlace> placeObjectsMap;
	
	/**
	 * Context history pull service to get past places state
	 */
	private DataBasePullService contextHistory_pull;
	
	/**
	 * Context history push service to save the current places
	 */
	private DataBasePushService contextHistory_push;

	/**
	 * Called by ApAM when all dependencies are available
	 */
	public void newInst() {
		logger.info("Place manager started.");
		placeObjectsMap = new HashMap<String, SymbolicPlace>();
		
		JSONObject placeMap = contextHistory_pull.pullLastObjectVersion(this.getClass().getSimpleName());
		if(placeMap != null){
			try {
				JSONArray state = placeMap.getJSONArray("state");
				int length = state.length();
				int i = 0;
				
				while(i < length) {
					JSONObject obj = state.getJSONObject(i);
					
					String placeId = (String)obj.keys().next();
					JSONObject jsonPlace = new JSONObject(obj.getString(placeId));
					SymbolicPlace loc = new SymbolicPlace(placeId, jsonPlace.getString("name"));
					
					JSONArray objects = jsonPlace.getJSONArray("devices");
					
					int l = objects.length();
					int j = 0;
					
					while(j < l) {
						loc.addObject(objects.getString(j));
						j++;
					}

					placeObjectsMap.put(placeId, loc);
					i++;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		logger.debug("The place manager has been initialized");
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
	 * @param placeId
	 *            , the id of the new place
	 * @param name
	 *            , the place name
	 */
	public synchronized void addPlace(String placeId, String name) {
		if (!placeObjectsMap.containsKey(placeId)) {
			placeObjectsMap.put(placeId, new SymbolicPlace(placeId, name));
			notifyPlace(placeId, name, "newPlace");
			
			// save the new devices name table 
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
			
			Set<String> keys = placeObjectsMap.keySet();
			for(String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
			}
			
			contextHistory_push.pushData_add(this.getClass().getSimpleName(), placeId, name, properties);
		}
	}

	/**
	 * Remove the designed place
	 * 
	 * @param placeId
	 *            , the place to removed
	 */
	public synchronized void removePlace(String placeId) {
		SymbolicPlace loc = placeObjectsMap.get(placeId);
		loc.removeAll();
		placeObjectsMap.remove(placeId);
		notifyPlace(placeId, "", "removePlace");
		
		// save the new devices name table 
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
					
		Set<String> keys = placeObjectsMap.keySet();
		for(String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
		}
					
		contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, loc.getName(), properties);		
	}

	/**
	 * Add an object to the designed place.
	 * 
	 * @param obj
	 *            , the object to locate
	 * @param placeId
	 *            , the target place
	 */
	private synchronized void addObject(String objId,
			String placeId) {
		SymbolicPlace loc = placeObjectsMap.get(placeId);
		loc.addObject(objId);
		
		// save the new devices name table 
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
							
		Set<String> keys = placeObjectsMap.keySet();
		for(String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
		}
							
		contextHistory_push.pushData_add(this.getClass().getSimpleName(), placeId, objId, properties);
		
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
	public synchronized void moveObject(String objId,
			String oldPlaceID, String newPlaceID) {
		
		boolean moved = false;
		
		if (oldPlaceID.contentEquals("-1") && !newPlaceID.contentEquals("-1")) {
			addObject(objId, newPlaceID);
			moved = true;
		} else if (!oldPlaceID.contentEquals("-1") && !newPlaceID.contentEquals("-1")) {
			SymbolicPlace oldLoc = placeObjectsMap.get(oldPlaceID);
			SymbolicPlace newLoc = placeObjectsMap.get(newPlaceID);
			oldLoc.removeObject(objId);
			newLoc.addObject(objId);
			moved = true;
		} else if (!oldPlaceID.contentEquals("-1") && newPlaceID.contentEquals("-1")) {
			SymbolicPlace oldLoc = placeObjectsMap.get(oldPlaceID);
			oldLoc.removeObject(objId);
			moved = true;
		}
		
		if(moved) {
			notifyMove(oldPlaceID, newPlaceID, objId);
		
			// save the new devices name table 
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
									
			Set<String> keys = placeObjectsMap.keySet();
			for(String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
			}
									
			contextHistory_push.pushData_change(this.getClass().getSimpleName(), objId, oldPlaceID, newPlaceID, properties);
		}
	}

	/**
	 * Rename the place
	 */
	public synchronized void renamePlace(String placeId, String newName) {
		SymbolicPlace loc = placeObjectsMap.get(placeId);
		String oldName = loc.getName();
		loc.setName(newName);
		
		notifyPlace(placeId, newName, "updatePlace");
		
		// save the new devices name table 
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
							
		Set<String> keys = placeObjectsMap.keySet();
		for(String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
		}
							
		contextHistory_push.pushData_change(this.getClass().getSimpleName(), placeId, oldName, newName, properties);		
	}

	/**
	 * Get the JSON tab of all place
	 * 
	 * @return all the place as a JSONArray
	 */
	public synchronized JSONArray getJSONPlaces() {
		Iterator<SymbolicPlace> places = placeObjectsMap.values().iterator();
		JSONArray jsonPlaceList = new JSONArray();
		SymbolicPlace loc;

		while (places.hasNext()) {
			loc = places.next();
			jsonPlaceList.put(loc.getDescription());
		}

		return jsonPlaceList;
	}

	/**
	 * Use to notify that a AbstractObject has moved.
	 * 
	 * @param oldPlaceId the former AbstractObject place
	 * @param newPlaceId the new AbstractObject place
	 * @param object the AbstractObject which has been moved.
	 */
	private void notifyMove(String oldPlaceId, String newPlaceId, String objId) {
		notifyChanged(new MoveObjectNotification(oldPlaceId, newPlaceId, objId));
	}
	
	/**
	 * Use to notify that new place has been created or has changed
	 * 
	 * @param placeId the place identifier
	 * @param placeName the user name of this place
	 * @param type indicate if this notification is a place creation (0) or an update (1)
	 */
	private void notifyPlace(String placeId, String placeName, String type) {
		notifyChanged(new PlaceManagerNotification(placeId, placeName, type));
	}
	
	/**
	 * This method notify ApAM that a new notification message has been produced.
	 * @param notif the notification message to send.
	 * @return nothing it just notify ApAM.
	 */
	public NotificationMsg notifyChanged (NotificationMsg notif) {
		logger.debug("Place Notify: "+ notif);
		return notif;
	}

	/**
	 * Get the place identifier of the core object give in parameter.
	 * @param the object from which get the place
	 */
	@Override
	public String getCoreObjectPlaceId(String objId) {
		Iterator<SymbolicPlace>  it = placeObjectsMap.values().iterator();
		
		SymbolicPlace loc = null;
		boolean found = false;
		
		while(it.hasNext() && !found) {
			
			loc = it.next();
			if(loc.isHere(objId)) {
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
