package appsgate.lig.on_off.actuator.impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.logical.object.messages.NotificationMsg;
import appsgate.lig.logical.object.spec.AbstractObjectSpec;
import appsgate.lig.on_off.actuator.messages.OnOffActuatorNotificationMsg;
import appsgate.lig.on_off.actuator.spec.OnOffActuatorSpec;
import appsgate.lig.proxy.services.EnOceanService;

public class OnOffAcuatorImpl implements OnOffActuatorSpec, AbstractObjectSpec {
	
	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory.getLogger(OnOffAcuatorImpl.class);
	
	/**
	 * the system name of this sensor.
	 */
	private String actuatorName;

	/**
	 * The network sensor id
	 */
	private String actuatorId;

	/**
	 * The sensor type (Actuator or Sensor)
	 */
	private String actuatorType;

	/**
	 * True if the device is paired with EnOcean proxy false otherwise
	 */
	private String isPaired;

	/**
	 * The current virtual actuator state
	 */
	private String isOn;

	/**
	 * The name set by the end user
	 */
	private String userName;

	/**
	 * The location where the sensor is installed
	 */
	private String locationId;

	/**
	 * The type for user of this sensor
	 */
	private String userType;

	/**
	 * The current sensor status.
	 * 
	 * 0 = Off line or out of range
	 * 1 = In validation mode (test range for sensor for instance)
	 * 2 = In line or connected
	 */
	private String status;

	/**
	 * The current picture identifier
	 */
	private String pictureId;
	
	/**
	 * EnOcean proxy service uses to validate the sensor configuration with the
	 * EnOcean proxy (pairing phase)
	 */
	EnOceanService enoceanProxy;
	
	public String getActuatorName() {
		return actuatorName;
	}

	public void setActuatorName(String actuatorName) {
		this.actuatorName = actuatorName;
	}

	public boolean isPaired() {
		return Boolean.valueOf(isPaired);
	}

	public void setPaired(boolean isPaired) {
		this.isPaired = String.valueOf(isPaired);
	}

	public String getActuatorId() {
		return actuatorId;
	}
	
	/**
	 * Get the kind of sensor
	 * @return String that represent the sensor type from EnOcean
	 */
	public String getSensoreType() {
		return actuatorType;
	}

	@Override
	public String getAbstractObjectId() {
		return getActuatorId();
	}

	@Override
	public String getUserObjectName() {
		return userName;
	}

	@Override
	public int getLocationId() {
		return Integer.valueOf(locationId);
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
		descr.put("name", userName);
		descr.put("type", userType); //6 for On_Off device
		descr.put("locationId", locationId);
		descr.put("status", status);
		descr.put("isOn", isOn);
		
		return descr;
	}

	@Override
	public void setUserObjectName(String userName) {
		this.userName = userName;
		notifyChanges("name", userName);
	}

	@Override
	public void setLocationId(int locationId) {
		this.locationId = String.valueOf(locationId) ;
	}

	@Override
	public void setPictureId(String pictureId) {
		this.pictureId = pictureId;
		notifyChanges("pictureId", pictureId);
	}
	
	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.info("New temperature sensor detected, "+actuatorId);
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.info("Temperature sensor desapeared, "+actuatorId);
	}
	
	public void isPairedChanged(String newPairedState){
		logger.info("New Paired status, "+newPairedState+", for "+actuatorId);
	}
	
	/**
	 * Called by APAM when the status of the on/Off device change
	 */
	public void isOnChanged (String newTemperatureValue) {
		logger.info("New temperature value from "+actuatorId+"/"+actuatorName+", "+isOn);
		notifyChanges("value", isOn);
	}
	
	/**
	 * Called by ApAM when the status value changed
	 * @param newStatus, the new status value.
	 * its a string the represent a integer value for the status code.
	 */
	public void statusChanged(String newStatus) {
		logger.info("The sensor, "+ actuatorId+" / "+ userName +" status changed to "+newStatus);
		notifyChanges("status", newStatus);
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
		return new OnOffActuatorNotificationMsg(isOn, varName, value, this);
	}
	
	@Override
	public JSONObject getTargetState() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("state", isOn);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}

	@Override
	public void on() {
		enoceanProxy.turnOnActuator(actuatorId);
	}

	@Override
	public void off() {
		enoceanProxy.turnOffActuator(actuatorId);
	}

}
