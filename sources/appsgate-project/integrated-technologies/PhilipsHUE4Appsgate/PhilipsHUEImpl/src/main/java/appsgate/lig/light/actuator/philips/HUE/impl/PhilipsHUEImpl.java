package appsgate.lig.light.actuator.philips.HUE.impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.colorLight.actuator.messages.ColorLightNotificationMsg;
import appsgate.lig.colorLight.actuator.spec.CoreColorLightSpec;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectSpec;
import appsgate.lig.proxy.PhilipsHUE.interfaces.PhilipsHUEServices;

/**
 * This class is the AppsGate implementation of ColorLightSpec for Philips HUE technology 
 * 
 * @author Cédric Gérard
 * @version 1.0.0
 * @since May 22, 2013
 * 
 */
public class PhilipsHUEImpl implements CoreColorLightSpec, CoreObjectSpec {
	
	private static long HUE_RED     = 0;
	private static long HUE_BLUE    = 46920;
	private static long HUE_GREEN   = 25500;
	private static long HUE_YELLOW  = 18456;
	private static long HUE_ORANGE  = 12750;
	private static long HUE_PURPLE  = 48765;
	private static long HUE_PINK    = 54332;
	private static long HUE_DEFAULT = 14922;
	
	private static int SAT_DEFAULT = 254;
	private static long BRI_DEFAULT = 180;
	
	
	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory.getLogger(PhilipsHUEImpl.class);
	
	private PhilipsHUEServices PhilipsBridge;
	
	private String actuatorName;
	private String actuatorId;
	private String actuatorType;
	
	private String pictureId;
	private String userType;
	
	private String lightBridgeId;
	private String lightBridgeIP;
	private String reachable;
	
	private String state;
	private String hue;
	private String sat;
	private String bri;
	private String x;
	private String y;
	private String ct;
	private String speed;
	private String alert;
	private String mode;
	private String effect;
	
	/**
	 * The current sensor status.
	 * 
	 * 0 = Off line or out of range
	 * 1 = In validation mode (test range for sensor for instance)
	 * 2 = In line or connected
	 */
	private String status;
	
	@Override
	public JSONObject getLightStatus() {
		return PhilipsBridge.getLightState(lightBridgeIP, lightBridgeId);
	}

	@Override
	public long getLightColor() {
		Long colorCode = Long.valueOf(-1);
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				colorCode = state.getLong("hue");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return colorCode;
	}

	@Override
	public int getLightBrightness() {
		int brightnessCode = -1;
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				brightnessCode = state.getInt("bri");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return brightnessCode;
	}
	
	@Override
	public int getLightColorSaturation() {
		int saturationCode = -1;
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				saturationCode = state.getInt("sat");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return saturationCode;
	}
	
	public String getLightEffect() {
		String effect = "";
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				effect = state.getString("effect");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return effect;
	}
	
	public String getLightAlert() {
		String alert = "";
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				alert = state.getString("alert");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return alert;
	}
	
	public long getTransitionTime() {
		long transistion = -1;
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				transistion = state.getLong("transitiontime");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return transistion;
	}

	@Override
	public boolean getCurrentState() {
		boolean lightState = false;
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			JSONObject state;
			try {
				state = jsonResponse.getJSONObject("state");
				lightState = state.getBoolean("on");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return lightState;
	}

	@Override
	public JSONObject getManufacturerDetails() {
		JSONObject manufacturerState = new JSONObject();
		JSONObject jsonResponse = getLightStatus();
		if(jsonResponse != null) {
			try {
				manufacturerState.put("type", jsonResponse.get("type"));
				manufacturerState.put("name", jsonResponse.get("name"));
				manufacturerState.put("model", jsonResponse.get("modelid"));
				manufacturerState.put("version", jsonResponse.get("swversion"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return manufacturerState;
	}

	@Override
	public boolean setStatus(JSONObject newStatus) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, newStatus)) {
			notifyChanges("status", newStatus.toString());
			return true;
		}
		
		return false;
	}

	@Override
	public boolean On() {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "on", true)) {
			notifyChanges("value", "true");
			return true;
		}
		
		return false;
	}

	@Override
	public boolean Off() {		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "on", false)) {
			notifyChanges("value", "false");
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean toggle() {
		if(getCurrentState()) {
			return Off();
		} else {
			return On();
		}
	}

	@Override
	public boolean setColor(long color) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "hue", color)) {
			notifyChanges("color", String.valueOf(color));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Change the HUe color value and set the brightness and the saturation to 
	 * default.
	 * @param color the HUE color value
	 * @return true if the color is set with default saturation and brightness values, false otherwise
	 */
	public boolean setSaturatedColor(long color) {
		
		JSONObject JSONAttribute = new JSONObject();
		
		try {
			JSONAttribute.put("hue", color);
			JSONAttribute.put("bri", BRI_DEFAULT);
			JSONAttribute.put("sat", SAT_DEFAULT);
			JSONAttribute.put("on", true);
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	
	
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, JSONAttribute)) {
			notifyChanges("value", "true");
			notifyChanges("color", String.valueOf(color));
			notifyChanges("brightness", String.valueOf(BRI_DEFAULT));
			notifyChanges("saturation", String.valueOf(SAT_DEFAULT));
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setBrightness(long brightness) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "bri", brightness)) {
			notifyChanges("brightness", String.valueOf(brightness));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Set the default brightness value
	 * @return true if the brightness return to default, false otherwise
	 */
	public boolean setDefaultBrightness() {
		return setBrightness(BRI_DEFAULT);
	}
	
	@Override
	public boolean setSaturation(int saturation) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "sat", saturation)) {
			notifyChanges("saturation", String.valueOf(saturation));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Turn the HUE light saturation to default value
	 * @return true if the default saturation is correctly set, false otherwise
	 */
	public boolean setDefaultSaturation() {
		return setSaturation(SAT_DEFAULT);
	}
	
	/**
	 * Change the HUE light effect, ie color loop or not
	 * @param effect the desired effect conform to the REST call from Philips HUE API (none or colorloop)	
	 * @return true if the HUE effect is correctly set, false otherwise
	 */
	public boolean setEffect(String effect) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "effect", effect)) {
			notifyChanges("effect", String.valueOf(effect));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Turn the HUE light in alert mode
	 * @param alert the alert mode: one blink (select), blink for 30 seconds (lselect), or return to previous settings (none)
	 * @return true of the alert mode turns on, false otherwise
	 */
	public boolean setAlert(String alert) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "alert", alert)) {
			notifyChanges("alert", String.valueOf(alert));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Set the color transition time
	 * @return true if the transition time is modified, false otherwise
	 */
	public boolean setTransitionTime(long transition) {
		
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "transitiontime", transition)) {
			notifyChanges("transitiontime", String.valueOf(transition));
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setRed() {
		if(setSaturatedColor(HUE_RED)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setBlue() {
		if(setSaturatedColor(HUE_BLUE)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setGreen() {
		if(setSaturatedColor(HUE_GREEN)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setYellow() {
		if(setSaturatedColor(HUE_YELLOW)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setOrange() {
		if(setSaturatedColor(HUE_ORANGE)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setPurple() {
		if(setSaturatedColor(HUE_PURPLE)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean setPink() {
		if(setSaturatedColor(HUE_PINK)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean setWhite() {
		JSONObject JSONAttribute  = new JSONObject();
		try {
			JSONAttribute.put("bri", 220);
			JSONAttribute.put("sat", 0);
			JSONAttribute.put("on", true);
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	
	
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, JSONAttribute)) {
			notifyChanges("value", "true");
			notifyChanges("saturation", String.valueOf(0));
			notifyChanges("brightness", String.valueOf(220));
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean setDefault() {
		if(setSaturatedColor(HUE_DEFAULT)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean increaseBrightness(int step) {
		int newBri = getLightBrightness()+step;
		if(PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "bri", newBri)) {
			notifyChanges("brightness", String.valueOf((newBri)));
			return true;
		}
		return false;
	}

	@Override
	public boolean decreaseBrightness(int step) {
		int newBri = getLightBrightness()-step;
		if( PhilipsBridge.setAttribute(lightBridgeIP, lightBridgeId, "bri", newBri)) {
			notifyChanges("brightness", String.valueOf((newBri)));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean blink() {
		return setAlert("select");
	}

	@Override
	public boolean blink30() {
		return setAlert("lselect");
	}

	@Override
	public boolean colorLoop() {
		return setEffect("colorloop");
	}
	
	public String getSensorName() {
		return actuatorName;
	}

	public void setSensorName(String actuatorName) {
		this.actuatorName = actuatorName;
	}
	
	@Override
	public String getAbstractObjectId() {
		return actuatorId;
	}

	@Override
	public String getUserType() {
		return userType;
	}

	@Override
	public int getObjectStatus() {
		return Integer.valueOf(status);
	}

	@Override
	public String getPictureId() {
		return pictureId;
	}

	@Override
	public JSONObject getDescription() throws JSONException {
	
		JSONObject descr = new JSONObject();
		descr.put("id", actuatorId);
		descr.put("type", userType); // 7 for color light
		descr.put("status", status);
		descr.put("value", getCurrentState());
		descr.put("color", getLightColor());
		descr.put("saturation", getLightColorSaturation());
		descr.put("brightness", getLightBrightness());
		descr.put("brightness", getLightBrightness());
		//Entry added for configuration GUI
		descr.put("deviceType", actuatorType);
		
		return descr;
	}

	@Override
	public void setPictureId(String pictureId) {
		this.pictureId = pictureId;
		notifyChanges("pictureId", pictureId);
	}
	
	public String getActuatorType() {
		return actuatorType;
	}

	public void setActuatorType(String actuatorType) {
		this.actuatorType = actuatorType;
	}
	
	public boolean isReachable() {
		return Boolean.valueOf(reachable);
	}

	/**
	 * Called by ApAM when the status value changed
	 * @param newStatus the new status value.
	 * its a string the represent a integer value for the status code.
	 */
	public void statusChanged(String newStatus) {
		logger.info("The actuator, "+ actuatorId+" status changed to "+newStatus);
		notifyChanges("status", newStatus);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newReachable the new reachable value.
	 * its a string the represent a boolean value for the reachable status.
	 */
	public void reachableChanged(String newReachable) {
		logger.info("The actuator, "+ actuatorId+" reachable changed to "+newReachable);
		notifyChanges("reachable", newReachable);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newState the new state value.
	 * its a string the represent a boolean value for the state(On/Off) of the light.
	 */
	public void stateChanged(String newState) {
		logger.info("The actuator, "+ actuatorId+" state changed to "+newState);
		notifyChanges("state", newState);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newHue the new hue value.
	 * its a string the represent a integer value for the hue status.
	 */
	public void hueChanged(String newHue) {
		logger.info("The actuator, "+ actuatorId+" hue changed to "+newHue);
		notifyChanges("hue", newHue);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newSat the new color saturation value.
	 * its a string the represent a integer value for the color saturation status.
	 */
	public void satChanged(String newSat) {
		logger.info("The actuator, "+ actuatorId+" sat changed to "+newSat);
		notifyChanges("sat", newSat);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newBri the new brightness value.
	 * its a string the represent a integer value for the brightness status.
	 */
	public void briChanged(String newBri) {
		logger.info("The actuator, "+ actuatorId+" bri changed to "+newBri);
		notifyChanges("bri", newBri);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newX the new x value.
	 * its a string the represent a float value for the x status.
	 */
	public void xChanged(String newX) {
		logger.info("The actuator, "+ actuatorId+" x changed to "+newX);
		notifyChanges("x", newX);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newY the new y value.
	 * its a string the represent a float value for the y status.
	 */
	public void yChanged(String newY) {
		logger.info("The actuator, "+ actuatorId+" y changed to "+newY);
		notifyChanges("y", newY);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newCT the new ct value.
	 * its a string the represent a integer value for the ct status.
	 */
	public void ctChanged(String newCT) {
		logger.info("The actuator, "+ actuatorId+" ct changed to "+newCT);
		notifyChanges("ct", newCT);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newSpeed the new transition time value.
	 * its a string the represent a integer value for the speed status.
	 */
	public void speedChanged(String newSpeed) {
		logger.info("The actuator, "+ actuatorId+" speed changed to "+newSpeed);
		notifyChanges("speed", newSpeed);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newAlert the new alert value.
	 */
	public void alertChanged(String newAlert) {
		logger.info("The actuator, "+ actuatorId+" alert changed to "+newAlert);
		notifyChanges("alert", newAlert);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newMode the new color mode value.
	 */
	public void modeChanged(String newMode) {
		logger.info("The actuator, "+ actuatorId+" mode changed to "+newMode);
		notifyChanges("mode", newMode);
	}
	
	/**
	 * Called by ApAM when the attribute value changed
	 * @param newEffect the new effect value.
	 */
	public void effectChanged(String newEffect) {
		logger.info("The actuator, "+ actuatorId+" effect changed to "+newEffect);
		notifyChanges("effect", newEffect);
	}
	
	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.info("New color light actuator detected, "+actuatorId);
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.info("A color light actuator desapeared, "+actuatorId);
	}

	/**
	 * This method uses the ApAM message model. Each call produce a
	 * TemperatureNotificationMsg object and notifies ApAM that a new message has
	 * been released.
	 * 
	 * @return nothing, it just notifies ApAM that a new message has been
	 *         posted.
	 */
	public NotificationMsg notifyChanges(String varName, String value) {
		return new ColorLightNotificationMsg(this, varName, value);
	}
	
	/**************************************/
	/** Getter and setter for attributes **/
	/**************************************/
	
	public boolean getReachable() {
		return Boolean.valueOf(reachable);
	}

	public boolean getState() {
		return Boolean.valueOf(state);
	}

	public int getHue() {
		return Integer.valueOf(hue);
	}

	public int getSat() {
		return Integer.valueOf(sat);
	}

	public int getBri() {
		return Integer.valueOf(bri);
	}

	public float getX() {
		return Float.valueOf(x);
	}

	public float getY() {
		return Float.valueOf(y);
	}

	public int getCt() {
		return Integer.valueOf(ct);
	}

	public long getSpeed() {
		return Long.valueOf(speed);
	}

	public String getAlert() {
		return alert;
	}

	public String getMode() {
		return mode;
	}

	public String getEffect() {
		return effect;
	}

}


