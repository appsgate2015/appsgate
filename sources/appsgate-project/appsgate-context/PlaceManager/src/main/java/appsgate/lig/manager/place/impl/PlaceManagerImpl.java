package appsgate.lig.manager.place.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.context.services.DataBasePullService;
import appsgate.lig.context.services.DataBasePushService;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.manager.place.messages.MoveObjectNotification;
import appsgate.lig.manager.place.messages.PlaceManagerNotification;
import appsgate.lig.manager.place.spec.PlaceManagerSpec;
import appsgate.lig.manager.place.spec.SymbolicPlace;

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
		logger.info("Place manager starting...");
		placeObjectsMap = new HashMap<String, SymbolicPlace>();
		
		//restore places from data base
		JSONObject placeMap = contextHistory_pull.pullLastObjectVersion(this.getClass().getSimpleName());
		if(placeMap != null){
			HashMap<String, List<String>> tempChildrenMap = new HashMap<String, List<String>>();
			try {
				JSONArray state = placeMap.getJSONArray("state");
				int length = state.length();
				int i = 0;
				
				while(i < length) {
					JSONObject obj = state.getJSONObject(i);
					
					String placeId = (String)obj.keys().next();
					JSONObject jsonPlace = new JSONObject(obj.getString(placeId));
					String name = jsonPlace.getString("name");
					String parentId = jsonPlace.getString("parent");
					JSONArray tags = jsonPlace.getJSONArray("tags");
					JSONArray properties = jsonPlace.getJSONArray("properties");
					JSONArray devices = jsonPlace.getJSONArray("devices");
					JSONArray services = jsonPlace.getJSONArray("services");
					
					ArrayList<String> tagsList = new ArrayList<String>();
					HashMap<String, String> propertiesList = new HashMap<String, String>();
					ArrayList<String> coreObjectList = new ArrayList<String>();
					ArrayList<String> coreServiceList = new ArrayList<String>();
					
					int iTagsArray = 0;
					int tagsArrayLength = tags.length();
					while(iTagsArray < tagsArrayLength) {
						tagsList.add(tags.getString(iTagsArray));
						iTagsArray++;
					}
					
					int ipropArray = 0;
					int propArrayLength = properties.length();
					JSONObject prop;
					while(ipropArray < propArrayLength) {
						prop = properties.getJSONObject(ipropArray);
						propertiesList.put(prop.getString("key"), prop.getString("value"));
						ipropArray++;
					}
					
					int ideviceArray = 0;
					int deviceArrayLength = devices.length();
					while(ideviceArray < deviceArrayLength) {
						coreObjectList.add(devices.getString(ideviceArray));
						ideviceArray++;
					}
					
					int iserviceArray = 0;
					int serviceArrayLength = services.length();
					while(iserviceArray < serviceArrayLength) {
						coreServiceList.add(services.getString(iserviceArray));
						iserviceArray++;
					}
					
					JSONArray children = jsonPlace.getJSONArray("children");
					ArrayList<String> childIdList = new ArrayList<String>();
					int ichildArray = 0;
					int childArrayLength = children.length();
					while(ichildArray < childArrayLength) {
						childIdList.add(children.getString(ichildArray));
						ichildArray++;
					}
					if(childArrayLength > 0) {
						tempChildrenMap.put(placeId, childIdList);
					}
					
					SymbolicPlace loc = new SymbolicPlace(placeId, name, tagsList, propertiesList, placeObjectsMap.get(parentId), coreObjectList, coreServiceList);

					placeObjectsMap.put(placeId, loc);
					i++;
				}
				//Restore children if any
				for(String key : tempChildrenMap.keySet()) {
					SymbolicPlace loc = placeObjectsMap.get(key);
					List<String> childrenList = tempChildrenMap.get(key);
					for(String child : childrenList) {
						loc.addChild(placeObjectsMap.get(child));
					}
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

	@Override
	public synchronized String addPlace(String name, String parent) {

		String placeId = String.valueOf(new String(name+new Double(Math.random())).hashCode());
		if (!placeObjectsMap.containsKey(placeId)) {
			SymbolicPlace newPlace = new SymbolicPlace(placeId, name, placeObjectsMap.get(parent));
			placeObjectsMap.put(placeId, newPlace);
			notifyPlace(placeId, name, "newPlace");
			// Save the new devices name table 
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
			
			Set<String> keys = placeObjectsMap.keySet();
			for(String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
			}

			if( contextHistory_push.pushData_add(this.getClass().getSimpleName(), placeId, name, properties)) {
				return placeId;
			}
		}
		return null;
	}

	@Override
	public synchronized boolean removePlace(String placeId) {
		SymbolicPlace selectedPlace = placeObjectsMap.get(placeId);
		SymbolicPlace parent = selectedPlace.getParent();
		
		
		@SuppressWarnings("unchecked")
		ArrayList<SymbolicPlace> children = (ArrayList<SymbolicPlace>)selectedPlace.getChildren().clone();
		for(SymbolicPlace child : children) {
			removePlace(child.getId());
		}
		selectedPlace.clearDevices();
		if(parent != null) {
			parent.removeChild(selectedPlace);
		}
		placeObjectsMap.remove(placeId);
		notifyPlace(placeId, selectedPlace.getName(), "removePlace");
		
		// save the new devices name table 
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
					
		Set<String> keys = placeObjectsMap.keySet();
		for(String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
		}
					
		return contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, selectedPlace.getName(), properties);
	}

	/**
	 * Add an object to the designed place.
	 * 
	 * @param obj the object to locate
	 * @param placeId the target place
	 */
	private synchronized void addObject(String objId, String placeId) {
		SymbolicPlace loc = placeObjectsMap.get(placeId);
		loc.addDevice(objId);
		
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
	 * Add an service to the designed place.
	 * 
	 * @param obj the service to locate
	 * @param placeId the target place
	 */
	private synchronized void addService(String serviceId, String placeId) {
		SymbolicPlace loc = placeObjectsMap.get(placeId);
		loc.addService(serviceId);
		
		// save the new devices name table 
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
							
		Set<String> keys = placeObjectsMap.keySet();
		for(String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
		}
							
		contextHistory_push.pushData_add(this.getClass().getSimpleName(), placeId, serviceId, properties);
		
	}

	@Override
	public synchronized boolean moveObject(String objId,
			String oldPlaceID, String newPlaceID) {
		
		boolean moved = false;
		
		if (oldPlaceID.contentEquals("-1") && !newPlaceID.contentEquals("-1")) {
			addObject(objId, newPlaceID);
			moved = true;
		} else if (!oldPlaceID.contentEquals("-1") && !newPlaceID.contentEquals("-1")) {
			SymbolicPlace oldLoc = placeObjectsMap.get(oldPlaceID);
			SymbolicPlace newLoc = placeObjectsMap.get(newPlaceID);
			oldLoc.removeDevice(objId);
			newLoc.addDevice(objId);
			moved = true;
		} else if (!oldPlaceID.contentEquals("-1") && newPlaceID.contentEquals("-1")) {
			SymbolicPlace oldLoc = placeObjectsMap.get(oldPlaceID);
			oldLoc.removeDevice(objId);
			moved = true;
		}
		
		if(moved) {
			notifyMove(oldPlaceID, newPlaceID, objId, 0);
		
			// save the new devices name table 
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
									
			Set<String> keys = placeObjectsMap.keySet();
			for(String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
			}
									
			return contextHistory_push.pushData_change(this.getClass().getSimpleName(), objId, oldPlaceID, newPlaceID, properties);
		}
		return false;
	}
	
	@Override
	public boolean moveService(String serviceId, String oldPlaceID,
			String newPlaceID) {
		
		boolean moved = false;
		
		if (oldPlaceID.contentEquals("-1") && !newPlaceID.contentEquals("-1")) {
			addService(serviceId, newPlaceID);
			moved = true;
		} else if (!oldPlaceID.contentEquals("-1") && !newPlaceID.contentEquals("-1")) {
			SymbolicPlace oldLoc = placeObjectsMap.get(oldPlaceID);
			SymbolicPlace newLoc = placeObjectsMap.get(newPlaceID);
			oldLoc.removeService(serviceId);
			newLoc.addService(serviceId);
			moved = true;
		} else if (!oldPlaceID.contentEquals("-1") && newPlaceID.contentEquals("-1")) {
			SymbolicPlace oldLoc = placeObjectsMap.get(oldPlaceID);
			oldLoc.removeService(serviceId);
			moved = true;
		}
		
		if(moved) {
			notifyMove(oldPlaceID, newPlaceID, serviceId, 1);
		
			// save the new devices name table 
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
									
			Set<String> keys = placeObjectsMap.keySet();
			for(String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
			}
									
			return contextHistory_push.pushData_change(this.getClass().getSimpleName(), serviceId, oldPlaceID, newPlaceID, properties);
		}
		return false;
	}

	@Override
	public synchronized boolean renamePlace(String placeId, String newName) {
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
							
		return contextHistory_push.pushData_change(this.getClass().getSimpleName(), placeId, oldName, newName, properties);		
	}
	
	@Override
	public synchronized SymbolicPlace getSymbolicPlace(String placId) {
		return placeObjectsMap.get(placId);
	}

	@Override
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
	
	@Override
	public synchronized String getCoreObjectPlaceId(String objId) {
		Iterator<SymbolicPlace>  it = placeObjectsMap.values().iterator();
		
		SymbolicPlace loc = null;
		boolean found = false;
		
		while(it.hasNext() && !found) {
			
			loc = it.next();
			if(loc.hasDevice(objId) || loc.hasService(objId)) {
				found = true;
			}
		}
		
		if(found) {
			return loc.getId();
		} else {
			return "-1";
		}
	}

	/**
	 * Use to notify that a AbstractObject has moved.
	 * 
	 * @param oldPlaceId the former AbstractObject place
	 * @param newPlaceId the new AbstractObject place
	 * @param object the AbstractObject which has been moved.
	 * @param moveType the type of the object that moved
	 */
	private void notifyMove(String oldPlaceId, String newPlaceId, String objId, int moveType) {
		notifyChanged(new MoveObjectNotification(oldPlaceId, newPlaceId, objId, moveType));
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

	@Override
	public synchronized boolean movePlace(String placeId, String newParentId) {
		try {
			SymbolicPlace place = placeObjectsMap.get(placeId);
			SymbolicPlace newParent = placeObjectsMap.get(newParentId);
		
			String oldParent = place.getParent().getId();
			place.setParent(newParent);
			
			notifyPlace(placeId, newParentId, "movePlace");
			
			// save in data base
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
								
			Set<String> keys = placeObjectsMap.keySet();
			for(String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
			}		
			return contextHistory_push.pushData_change(this.getClass().getSimpleName(), placeId, oldParent, newParentId, properties);	
			
		}catch(Exception e){logger.error(e.getMessage());}
		return false;
	}

	@Override
	public synchronized void setTagsList(String placeId, ArrayList<String> tags) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		place.setTags(tags);
		
		notifyPlace(placeId, "newTagList", "updatePlaceTag");
		
		// save in data base
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();
							
		Set<String> keys = placeObjectsMap.keySet();
		for(String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String,Object>(e, sl.getDescription().toString()));
		}		
		contextHistory_push.pushData_change(this.getClass().getSimpleName(), placeId, "", "tags", properties);
	}

	@Override
	public synchronized void clearTagsList(String placeId) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		place.clearTags();
		
		notifyPlace(placeId, "taglistFree", "updatePlaceTag");
		
		// save in data base
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

		Set<String> keys = placeObjectsMap.keySet();
		for (String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String, Object>(e, sl
					.getDescription().toString()));
		}
		contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, "tags", properties);
	}

	@Override
	public synchronized boolean addTag(String placeId, String tag) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		if (place.addTag(tag)) {

			notifyPlace(placeId, tag, "updatePlaceTag");
			
			// save in data base
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

			Set<String> keys = placeObjectsMap.keySet();
			for (String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String, Object>(e,
						sl.getDescription().toString()));
			}
			return contextHistory_push.pushData_add(this.getClass().getSimpleName(), placeId, "tag", tag, properties);
		}
		return false;
	}

	@Override
	public synchronized boolean removeTag(String placeId, String tag) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		if(place.removeTag(tag)) {
		
			notifyPlace(placeId, tag, "removePlaceTag");
			
			// save in data base
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

			Set<String> keys = placeObjectsMap.keySet();
			for (String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String, Object>(e,sl.getDescription().toString()));
			}
			return contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, "tag", tag, properties);
		}
		return false;
	}

	@Override
	public synchronized void setProperties(String placeId, HashMap<String, String> properties) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		place.setProperties(properties);
		notifyPlace(placeId, "newPropertiesList", "updatePlaceProp");
		// save in data base
		ArrayList<Map.Entry<String, Object>> propertiesDB = new ArrayList<Map.Entry<String, Object>>();

		Set<String> keys = placeObjectsMap.keySet();
		for (String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			propertiesDB.add(new AbstractMap.SimpleEntry<String, Object>(e,sl.getDescription().toString()));
		}
		contextHistory_push.pushData_change(this.getClass().getSimpleName(), placeId, "", "properties", propertiesDB);
	}

	@Override
	public synchronized void clearPropertiesList(String placeId) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		place.clearProperties();
		notifyPlace(placeId, "propertiesListFree", "updatePlaceProp");
		// save in data base
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

		Set<String> keys = placeObjectsMap.keySet();
		for (String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String, Object>(e,
					sl.getDescription().toString()));
		}
		contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, "properties", properties);
	}

	@Override
	public synchronized boolean addProperty(String placeId, String key, String value) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		if( place.addProperty(key, value)) {
			notifyPlace(placeId, key+"-"+value, "updatePlaceProp");
			// save in data base
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

			Set<String> keys = placeObjectsMap.keySet();
			for (String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String, Object>(e,sl.getDescription().toString()));
			}
			return contextHistory_push.pushData_add(this.getClass().getSimpleName(), placeId, "property", key, properties);
		}
		return false;
	}

	@Override
	public synchronized boolean removeProperty(String placeId, String key) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		if( place.removeProperty(key)) {
			notifyPlace(placeId, key, "removePlaceProp");
			// save in data base
			ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

			Set<String> keys = placeObjectsMap.keySet();
			for (String e : keys) {
				SymbolicPlace sl = placeObjectsMap.get(e);
				properties.add(new AbstractMap.SimpleEntry<String, Object>(e,sl.getDescription().toString()));
			}
			return contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, "property", key, properties);
		}
		return false;
	}

	@Override
	public synchronized void removeAllCoreObject(String placeId) {
		SymbolicPlace place = placeObjectsMap.get(placeId);
		place.clearDevices();
		notifyPlace(placeId, "coreObjectListFree", "updatePlaceObject");
		// save in data base
		ArrayList<Map.Entry<String, Object>> properties = new ArrayList<Map.Entry<String, Object>>();

		Set<String> keys = placeObjectsMap.keySet();
		for (String e : keys) {
			SymbolicPlace sl = placeObjectsMap.get(e);
			properties.add(new AbstractMap.SimpleEntry<String, Object>(e,sl.getDescription().toString()));
		}
		contextHistory_push.pushData_remove(this.getClass().getSimpleName(), placeId, "coreObject", properties);
	}

	@Override
	public synchronized ArrayList<SymbolicPlace> getRootPlaces() {
		ArrayList<SymbolicPlace> rootPlaces = new ArrayList<SymbolicPlace>();
		for(SymbolicPlace place : placeObjectsMap.values()) {
			if(place.getParent() == null) {
				rootPlaces.add(place);
			}
		}
		return rootPlaces;
	}

	@Override
	public synchronized ArrayList<SymbolicPlace> getPlaces() {
		return new ArrayList<SymbolicPlace>(placeObjectsMap.values());
	}

	@Override
	public synchronized ArrayList<SymbolicPlace> getPlacesWithName(String name) {
		ArrayList<SymbolicPlace> placeName = new ArrayList<SymbolicPlace>();
		for(SymbolicPlace currentPlace : placeObjectsMap.values()) {
			if(currentPlace.getName().contentEquals(name)){
				placeName.add(currentPlace);
			}
		}
		return placeName;
	}

	@Override
	public synchronized ArrayList<SymbolicPlace> getPlacesWithTags(ArrayList<String> tags) {
		ArrayList<SymbolicPlace> placeTags = new ArrayList<SymbolicPlace>();
		for(SymbolicPlace currentPlace : placeObjectsMap.values()) {
			if(currentPlace.getTags().containsAll(tags)){
				placeTags.add(currentPlace);
			}
		}
		return placeTags;
	}

	@Override
	public synchronized ArrayList<SymbolicPlace> getPlacesWithProperties(
			ArrayList<String> propertiesKey) {
		ArrayList<SymbolicPlace> placeprop = new ArrayList<SymbolicPlace>();
		for(SymbolicPlace currentPlace : placeObjectsMap.values()) {
			if(currentPlace.getProperties().keySet().containsAll(propertiesKey)){
				placeprop.add(currentPlace);
			}
		}
		return placeprop;
	}

	@Override
	public synchronized ArrayList<SymbolicPlace> getPlacesWithPropertiesValue(HashMap<String, String> properties) {
		ArrayList<SymbolicPlace> placeprop = new ArrayList<SymbolicPlace>();
		for(SymbolicPlace currentPlace : placeObjectsMap.values()) {
			HashMap<String, String> currentProperties = currentPlace.getProperties();
			boolean equals =  false;
			for(String key : properties.keySet()) {
				if(currentProperties.containsKey(key)){
					if(currentProperties.get(key).contentEquals(properties.get(key))) {
						equals = true;
					}else{
						equals = false;
						break;
					}
				}else {
					equals = false;
					break;
				}
			}
			
			if(equals){
				placeprop.add(currentPlace);
			}
		}
		return placeprop;
	}
	
	@Override
	public SymbolicPlace getPlaceWithDevice(String deviceId) {
		SymbolicPlace place = null;
		for(SymbolicPlace currentPlace : placeObjectsMap.values()) {
			boolean isHere = false;
			for(String objectID : currentPlace.getDevices()) {
				if(objectID.contentEquals(deviceId)) {
					isHere = true;
					break;
				}
			}
			if(isHere) {
				place = currentPlace;
				break;
			}
		}
		return place;
	}
	
	@Override
	public SymbolicPlace getPlaceWithService(String serviceId) {
		SymbolicPlace place = null;
		for(SymbolicPlace currentPlace : placeObjectsMap.values()) {
			boolean isHere = false;
			for(String serviceID : currentPlace.getServices()) {
				if(serviceID.contentEquals(serviceId)) {
					isHere = true;
					break;
				}
			}
			if(isHere) {
				place = currentPlace;
				break;
			}
		}
		return place;
	}
	
	/*************************/
	/** for JUnit mock test **/
	/*************************/
	public void initiateMock(DataBasePullService pull, DataBasePushService push) {
		this.contextHistory_pull = pull;
		this.contextHistory_push = push;
	}

}
