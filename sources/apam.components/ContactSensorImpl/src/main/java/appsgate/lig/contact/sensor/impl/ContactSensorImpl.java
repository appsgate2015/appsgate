package appsgate.lig.contact.sensor.impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsGate.lig.manager.communication.service.send.SendWebsocketsService;
import appsgate.lig.contact.sensor.messages.ContactNotificationMsg;
import appsgate.lig.contact.sensor.spec.ContactSensorSpec;
import appsgate.lig.logical.object.messages.NotificationMsg;
import appsgate.lig.logical.object.spec.AbstractObjectSpec;
import appsgate.lig.proxy.services.EnOceanService;

public class ContactSensorImpl implements ContactSensorSpec, AbstractObjectSpec {
	
	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory.getLogger(ContactSensorImpl.class);
	
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
	private String sensoreType;

	/**
	 * True if the device is paired with EnOcean proxy false otherwise
	 */
	private String isPaired;
	
	/**
	 * The current status = the last value received from this sensor
	 */
	private String currentStatus;
	
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

	@Override
	public boolean getContactStatus() {
		return Boolean.valueOf(currentStatus);
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

	public String getSensoreType() {
		return sensoreType;
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
	public void setUserObjectName(String userName) {
		this.userName = userName;
		notifyChanges("name", userName);
	}

	@Override
	public void setLocationId(int locationId) {
		this.locationId = String.valueOf(locationId);
	}

	@Override
	public void setPictureId(String pictureId) {
		this.pictureId = pictureId;
		notifyChanges("pictureId",  pictureId);

	}
	
	@Override
	public JSONObject getDescription() throws JSONException {
		JSONObject descr = new JSONObject();
		descr.put("id", sensorId);
		descr.put("name", userName);
		descr.put("type", userType); //3 for contact sensor
		descr.put("locationId", locationId);
		descr.put("status", status);
		descr.put("contact", currentStatus);
		
		return descr;
	}

	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.info("New contact sensor detected, "+sensorId);
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.info("Contact sensor desapeared, "+sensorId);
	}
	
	public void isPairedChanged(String newPairedState){
		logger.info("New Paired status, "+newPairedState+", for "+sensorId);
	}
	
	/**
	 * Called by APAM when a new contact status is received from the sensor.
	 * @param newContactStatus, the new contact status
	 */
	public void currentStatusChanged(String newContactStatus) {
		logger.info("New contact status from "+sensorId+"/"+sensorName+", "+newContactStatus);
		notifyChanges("contact", newContactStatus);
	}
	
	/**
	 * Called by ApAM when the status value changed
	 * @param newStatus, the new status value.
	 * its a string the represent a integer value for the status code.
	 */
	public void statusChanged(String newStatus) {
		logger.info("The sensor, "+ sensorId+" / "+ userName +" status changed to "+newStatus);
		notifyChanges("status", newStatus);
	}

	/**
	 * This method uses the ApAM message model. Each call produce a
	 * ContactNotificationMsg object and notifies ApAM that a new message has
	 * been released.
	 * 
	 * @return nothing, it just notifies ApAM that a new message has been
	 *         posted.
	 */
	public NotificationMsg notifyChanges(String varName, String value) {
		//TODO remove this call when Adele fix the message bug.
		try {
			sendToClientService.send(new ContactNotificationMsg(Boolean.valueOf(currentStatus), varName, value, this).JSONize().toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return new ContactNotificationMsg(Boolean.valueOf(currentStatus), varName, value, this);
	}
	
	/**
	 * Service to communicate with clients (TEMP)
	 */
	//TODO remove this class member when Adele commit the message fix.
	private SendWebsocketsService sendToClientService;

}
