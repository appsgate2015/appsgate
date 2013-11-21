package appsgate.lig.temperature.sensor.dfki.impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.adapter.urc.services.URCAdapterServices;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectSpec;
import appsgate.lig.temperature.sensor.messages.TemperatureNotificationMsg;
import appsgate.lig.temperature.sensor.spec.CoreTemperatureSensorSpec;

public class DFKITempSensorImpl implements CoreObjectSpec, CoreTemperatureSensorSpec {

	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory.getLogger(DFKITempSensorImpl.class);

	/**
	 * the system name of this sensor.
	 */
	private String sensorName;

	/**
	 * The network sensor id
	 */
	private String sensorId;

	/**
	 * The sensor type (Actuator or Sensor)
	 */
	private String sensorType;

	/**
	 * True if the device is paired with EnOcean proxy false otherwise
	 */
	private String isPaired;

	/**
	 * The current temperature = the last value received from this sensor
	 */
	private String currentTemperature;

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
	 * URC adapter service
	 */
	URCAdapterServices URCAdapterServices;

	@Override
	public TemperatureUnit getTemperatureUnit() {
		return TemperatureUnit.Celsius;
	}

	@Override
	public float getTemperature() {
		return Float.valueOf(currentTemperature);
	}

	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	public boolean isPaired() {
		return Boolean.valueOf(isPaired);
	}

	public void setPaired(boolean isPaired) {
		this.isPaired = String.valueOf(isPaired);
	}

	public String getSensorId() {
		return sensorId;
	}
	
	@Override
	public String getAbstractObjectId() {
		return getSensorId();
	}

	/**
	 * Get the kind of sensor
	 * @return String that represent the sensor type from EnOcean
	 */
	public String getSensoreType() {
		return sensorType;
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
	public void setPictureId(String pictureId) {
		this.pictureId = pictureId;
		notifyChanges("pictureId", pictureId);
	}
	
	@Override
	public JSONObject getDescription() throws JSONException {
		JSONObject descr = new JSONObject();
		
		descr.put("id", sensorId);
		descr.put("type", userType); //O for temperature sensor
		descr.put("status", status);
		descr.put("value", currentTemperature);
		
		return descr;
	}

	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.info("New temperature sensor detected, "+sensorId);
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.info("Temperature sensor desapeared, "+sensorId);
	}
	
	public void isPairedChanged(String newPairedState){
		logger.info("New Paired status, "+newPairedState+", for "+sensorId);
	}
	
	/**
	 * Called by APAM when a new temperature value is received from the sensor.
	 * @param newTemperatureValue the new temperature
	 */
	public void currentTemperatureChanged (String newTemperatureValue) {
		logger.info("New temperature value from "+sensorId+"/"+sensorName+", "+newTemperatureValue);
		notifyChanges("value", newTemperatureValue);
	}
	
	/**
	 * Called by ApAM when the status value changed
	 * @param newStatus the new status value.
	 * its a string the represent a integer value for the status code.
	 */
	public void statusChanged(String newStatus) {
		logger.info("The sensor, "+ sensorId+" status changed to "+newStatus);
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
		return new TemperatureNotificationMsg(Float.valueOf(currentTemperature), varName, value, this);
	}
	
}