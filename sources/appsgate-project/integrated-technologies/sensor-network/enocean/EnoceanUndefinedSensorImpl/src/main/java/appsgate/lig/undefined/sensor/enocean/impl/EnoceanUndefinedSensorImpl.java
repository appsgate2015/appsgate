package appsgate.lig.undefined.sensor.enocean.impl;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.core.object.spec.CoreObjectSpec;
import appsgate.lig.enocean.ubikit.adapter.spec.UbikitAdapterService;
import appsgate.lig.undefined.sensor.spec.CoreUndefinedSensorSpec;

/**
 * This is the class that represent the EnOcean implementation of an undefined sensor.
 * 
 * @author Cédric Gérard
 * @since December 1, 2012
 * @version 1.0.0
 * 
 * @see UndefinedSensorSpec
 * @see CoreObjectSpec
 */
public class EnoceanUndefinedSensorImpl implements CoreUndefinedSensorSpec, CoreObjectSpec {
	
	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory.getLogger(EnoceanUndefinedSensorImpl.class);
	
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
	 * The type for end suer
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
	UbikitAdapterService enoceanProxy;
	
	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
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
		descr.put("id", sensorId);
		descr.put("name", "Undefined");
		descr.put("type", userType); //-1 for undefined sensor
		descr.put("locationId", "no where");
		descr.put("status", status);
		
		return descr;
	}

	@Override
	public void setPictureId(String pictureId) {
		this.pictureId = pictureId;
	}
	
	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.info("New undefined sensor detected, "+sensorId);
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.info("Undefined sensor desapeared, "+sensorId);
	}
	
	/**
	 * Called by ApAM when the status value changed
	 * @param newStatus the new status value.
	 * its a string the represent a integer value for the status code.
	 */
	public void statusChanged(String newStatus) {
		logger.info("The sensor, "+ sensorId+" / "+ sensorName +" status changed to "+newStatus);
	}

	@Override
	public JSONArray getCapabilities() {
		return enoceanProxy.getItemCapabilities(sensorId);
	}

	@Override
	public void validate(String profile) {
		logger.info("valiation command sent: "+profile);
		ArrayList<String> caps = new ArrayList<String>();
		caps.add(profile);
		enoceanProxy.validateItem(sensorId,	caps, true);
	}
	
}
