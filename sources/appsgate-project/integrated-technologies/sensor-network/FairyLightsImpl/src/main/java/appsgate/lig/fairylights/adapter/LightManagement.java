package appsgate.lig.fairylights.adapter;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.fairylights.adapter.FairyLightsAdapterSpec.LightReservationPolicy;
import appsgate.lig.fairylights.service.LumiPixelImpl;

/**
 * This class is designed to enforce reservation policy acting as entry point to fairy light control
 * read is allowed to all, but write (i.e: changing a light color) depends on the reservation policy
 * Should intercept all color changes, and trigger the vent to the listeners
 * @author thibaud
 *
 */
public class LightManagement {
	
	String[] affectations;
	final static int FAIRYLIGHT_SIZE = 25;
	public static final String GROUP_ALL_ID = "FairyLights-All";

	
	// Enforcing singleton pattern (with default values)
	private static LightManagement instance =
			new LightManagement(LumiPixelImpl.DEFAULT_PROTOCOL+LumiPixelImpl.DEFAULT_HOST,
					LightReservationPolicy.SHARED);
	
	private static Logger logger = LoggerFactory.getLogger(LightManagement.class);
	
	public String host;
	
	Set<FairyLightsStatusListener> listeners;

	public void setHost(String host) {
		this.host = host;
	}
	
	LightReservationPolicy policy;
	
	/**
	 * Should not change often because the lights behavior may not be consistant
	 * @param policy
	 */
	public void setPolicy(LightReservationPolicy policy) {
		this.policy = policy;
	}
	
	
	public static LightManagement getInstance() {
		return instance;
	}

	private LightManagement(String host, LightReservationPolicy policy) {
		affectations = new String[FAIRYLIGHT_SIZE];
		for(int i = 0; i< FAIRYLIGHT_SIZE; i++) {
			affectations[i] = null;
		}
		setHost(host);
		setPolicy(policy);
		listeners = new HashSet<FairyLightsStatusListener>();
	}
	
	public void addListener(FairyLightsStatusListener listener) {
		logger.trace("addListener(FairyLightsStatusListener listener : {})", listener);
		if(listener != null && !listeners.contains(listener)) {
			listeners.add(listener);
			logger.trace("addListener(...), listener added");
		} else {
			logger.warn("addListener(...), listener not added (null or already in the list)");			
		}
	}
	
	public void removeListener(FairyLightsStatusListener listener) {
		logger.trace("removeListener(FairyLightsStatusListener listener : {})", listener);
		if(listener != null && listeners.contains(listener)) {
			listeners.remove(listener);
			logger.trace("removeListener(...), listener removed");
		} else {
			logger.warn("removeListener(...), listener not removed (null or not in the list)");			
		}
	}
	
	
	/**
	 * Might throws an IndexOutOfBoundException if lightNumber is not between 0 and FAIRYLIGHT_SIZE
	 * @param groupId
	 * @param lightNumber
	 * @return
	 */
	public synchronized boolean affect(String groupId, int lightNumber) {
		logger.trace("affect(String groupId : {}, int lightNumber : {})", groupId, lightNumber);
		if(groupId != null && affectations[lightNumber]== null ) {
			logger.trace("affect(...), affectation successful");
			affectations[lightNumber] = groupId;
			return true;
		} else {
			logger.warn("affect(...), affectation unsuccessful"
					+ " (groupId is null or lightNumber already affected)");
			return false;			
		}
	}
	
	/**
	 * Might throws an IndexOutOfBoundException if lightNumber is not between 0 and FAIRYLIGHT_SIZE
	 * @param lightNumber
	 */
	public synchronized void release(int lightNumber) {
		logger.trace("release(int lightNumber : {})", lightNumber);
		affectations[lightNumber] = null;
	}
	
	/**
	 * Might throws an IndexOutOfBoundException if lightNumber is not between 0 and FAIRYLIGHT_SIZE
	 * @param lightNumber
	 * @return the groupId that owns this light number or null if it is unaffected
	 */
	public synchronized String getAffectation(int lightNumber) {
		logger.trace("getAffectation(int lightNumber : {}), returning {}", lightNumber, affectations[lightNumber]);
		return affectations[lightNumber];
	}

	public JSONArray getAllLights() {
		logger.trace("getAllLights()");
		return LumiPixelImpl.getAllLights(host);
	}

	public String getOneLight(int lightNumber) {
		logger.trace("getOneLight(int lightNumber : {})", lightNumber);
		return LumiPixelImpl.getOneLight(host, lightNumber);
	}

	
	public JSONObject setOneColorLight(String groupId, int lightNumber, String color) {
		logger.trace("setOneColorLight(String groupId : {}, int lightNumber : {}, String color : {})",
				groupId, lightNumber, color);
		if( isAvailable( groupId,  lightNumber)) {
			JSONObject response = LumiPixelImpl.setOneColorLight(host, lightNumber, color);
			if(response != null && listeners != null && listeners.size()>0) {
				logger.trace("setOneColorLight(...),"
						+ " light sucessfully changed firing the new status to all listeners");
				JSONArray lights = new JSONArray();
				lights.put(response);
				for(FairyLightsStatusListener listener : listeners) {
					listener.lightChanged(lights);
				}
			}			
			return response;
		} else {
			logger.warn("setOneColorLight(...), cannot set color, groupId is null or reservation policy does not allow to change the color");
			return null;
		}
	}
	
	/**
	 * checking all reservation policy
	 * @param groupId
	 * @param lightNumber
	 * @return
	 */
	public boolean isAvailable(String groupId, int lightNumber) {
		logger.trace("isAvailable(String groupId : {}, int lightNumber : {})",
				groupId, lightNumber);
		if( groupId!= null && ( 
				policy == LightReservationPolicy.SHARED // any group can change the color of any light
				|| groupId.equals(getAffectation(lightNumber)) // the group owns the light (works with ASSIGNED and EXCLUSIVE)
				|| (policy == LightReservationPolicy.ASSIGNED && groupId.equals(GROUP_ALL_ID) ) // the group "all" can always change any light color in the ASSIGNED policy	
				)) {
			logger.trace("isAvailable(...), returning true");
			return true;
		} else {
			logger.trace("isAvailable(...), returning false");
			return false;
		}
	}
	
	public JSONArray setColorPattern(String groupId, JSONArray pattern) {
		logger.trace("setColorPattern(String groupId : {}, JSONObject pattern : {})", groupId, pattern);
		int length = pattern.length();	
		
		for(int i = 0; i< length; i++) {
			JSONObject obj = pattern.getJSONObject(i);
			if(isAvailable(groupId, obj.getInt(LumiPixelImpl.KEY_ID))) {
				LumiPixelImpl.setOneColorLight(host,
					obj.getInt(LumiPixelImpl.KEY_ID), obj.getString(LumiPixelImpl.KEY_COLOR));
			}
		}
		
		return triggerLightsChanged();
	}
	
	
	private JSONArray triggerLightsChanged() {
		JSONArray lights = getAllLights();
		if(listeners != null && listeners.size()>0) {
			logger.trace("triggerLightsChanged(),"
					+ " lights may have changed firing the whole status to all listeners");
			for(FairyLightsStatusListener listener : listeners) {
				listener.lightChanged(lights);
			}
		}
		return lights;

		
	}
	/**
	 * Depending on the affectation this one will change all lights belonging to a group
	 * @param groupId
	 * @param color
	 * @return
	 */
	public JSONArray setAllColorLight(String groupId, String color) {
		logger.trace("setAllColorLight(String groupId, String color : {})", groupId, color);
		
		for(int i = 0; i< FAIRYLIGHT_SIZE; i++) {
			if(isAvailable(groupId, i)) {
				LumiPixelImpl.setOneColorLight(host,
					i, color);
			}			
		}
		
		return triggerLightsChanged();
	}

	/**
	 * with this function lights will return to their original states
	 * (and we don't want to bother triggerring any single color changes) 
	 * @param groupId
	 * @param start
	 * @param end
	 * @param color
	 */
	public void singleChaserAnimation(String groupId, int start, int end, String color) {
		logger.trace("singleChaserAnimation(String groupId: {}, int start : {}, int end : {}, String color : {})",
				groupId, start, end, color);
		
		JSONArray cache = getAllLights();
		
		if(start < end) {
			for(int i = start; i<= end; i++) {
				if (i> start) {
					LumiPixelImpl.setOneColorLight(host,
							i-1, cache.getJSONObject(i-1).getString(LumiPixelImpl.KEY_COLOR));
				}
				LumiPixelImpl.setOneColorLight(host,
						i, color);
			}
			LumiPixelImpl.setOneColorLight(host,
					end, cache.getJSONObject(end).getString(LumiPixelImpl.KEY_COLOR));
		} else {
			for(int i = start; i>= end; i--) {
				if (i<start) {
					LumiPixelImpl.setOneColorLight(host,
							i+1, cache.getJSONObject(i+1).getString(LumiPixelImpl.KEY_COLOR));
				}
				LumiPixelImpl.setOneColorLight(host,
						i, color);
			}
			LumiPixelImpl.setOneColorLight(host,
					end, cache.getJSONObject(end).getString(LumiPixelImpl.KEY_COLOR));
		}
		logger.trace("singleChaserAnimation(...), chaser ended");
	}
	
}
