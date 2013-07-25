
/*
__BANNER__
*/
// this file was generated at 18-July-2013 04:15 PM by ${author}
package appsgate.lig.upnp.media.proxy;

import org.apache.felix.upnp.devicegen.holder.*;
import appsgate.lig.upnp.media.*;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import org.osgi.framework.Filter;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPException;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPService;

import fr.imag.adele.apam.Instance;

import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectSpec;


public class RenderingControlProxyImpl implements CoreObjectSpec, RenderingControl, UPnPEventListener {		

	private String 		userObjectName;
	private int			locationId;
	private String 		pictureId;
	
	private String 		serviceType ;
	private String 		serviceId ;

	private UPnPDevice  upnpDevice;
	private UPnPService upnpService;
	
	RenderingControlProxyImpl(){
		super();
	}
	
	@SuppressWarnings("unused")
	private void initialize(Instance instance) {
		serviceType = instance.getProperty(UPnPService.TYPE);
		serviceId 	= instance.getProperty(UPnPService.ID);
		upnpService	= upnpDevice.getService(serviceId);
	}
	
	public String getAbstractObjectId() {
		return upnpDevice.getDescriptions(null).get(UPnPDevice.ID)+"/"+serviceId;
	}

	public String getUserType() {
		// TODO Generate unique type identifier !!!
		return Integer.toString(serviceType.hashCode());
	}

	public int getObjectStatus() {
		return 2;
	}
	
	
	private java.lang.String lastChange;
	
	@Override
	public java.lang.String getLastChange() {
		return lastChange;
	}
	
		
	public String getUserObjectName() {
		return userObjectName;
	}

	public void setUserObjectName(String userName) {
		this.userObjectName = userName;
	}

	public int getLocationId() {
		return locationId;
	}

	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}
	
	public String getPictureId() {
		return pictureId;
	}

	public void setPictureId(String pictureId) {
		this.pictureId = pictureId;
	}

	public JSONObject getDescription() throws JSONException {
		JSONObject description = new JSONObject();
		
		description.put("id", getAbstractObjectId());
		description.put("physical-device", upnpDevice.getDescriptions(null).get(UPnPDevice.ID));
		description.put("name", getUserObjectName());
		description.put("type", getUserType()); 
		description.put("locationId", getLocationId());
		description.put("status", getObjectStatus());
		
		
		description.put("lastChange", getLastChange());
		
		
		return description;
	}

	@SuppressWarnings("unused")
	private Filter upnpEventFilter;
	
	@Override
	@SuppressWarnings("unchecked")
	public void notifyUPnPEvent(String deviceId, String serviceId,	@SuppressWarnings("rawtypes") Dictionary events) {

		Enumeration<String> variables = events.keys();
		while( variables.hasMoreElements()) {

			String variable = variables.nextElement();
			Object value	= events.get(variable);
			
			stateChanged(variable,value);
		}
	}		

	private NotificationMsg stateChanged(String variable, Object value) {
	
	
		if (variable.equals("LastChange"))
			lastChange = (java.lang.String) value;
	
		
		return new Notification(variable, value.toString());
	}

	private  class Notification implements NotificationMsg {

		private final String variable;
		private final String value;
		
		public Notification( String variable, String value) {
			this.variable 	= variable;
			this.value		= value;
		}
		
		@Override
		public CoreObjectSpec getSource() {
			return RenderingControlProxyImpl.this;
		}

		@Override
		public String getNewValue() {
			return value;
		}

		@Override
		public JSONObject JSONize() throws JSONException {
			
			JSONObject notification = new JSONObject();
			
			notification.put("objectId", getAbstractObjectId());
			notification.put("varName", variable);
			notification.put("value", value);
		
			return notification;
		}	
	
	}

	
	
	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentPresetNameList out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void listPresets(
		long instanceID,

StringHolder currentPresetNameList
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("ListPresets");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"ListPresets"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentPresetNameList.setValue(StringHolder.toValue((java.lang.String)_result.get("CurrentPresetNameList")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * presetName in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void selectPreset(
		long instanceID,

java.lang.String presetName
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SelectPreset");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SelectPreset"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("PresetName", 
						(new StringHolder(presetName)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentBrightness out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getBrightness(
		long instanceID,

IntegerHolder currentBrightness
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetBrightness");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetBrightness"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentBrightness.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentBrightness")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredBrightness in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setBrightness(
		long instanceID,

int desiredBrightness
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetBrightness");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetBrightness"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredBrightness", 
						(new IntegerHolder(desiredBrightness)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentContrast out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getContrast(
		long instanceID,

IntegerHolder currentContrast
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetContrast");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetContrast"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentContrast.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentContrast")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredContrast in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setContrast(
		long instanceID,

int desiredContrast
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetContrast");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetContrast"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredContrast", 
						(new IntegerHolder(desiredContrast)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentSharpness out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getSharpness(
		long instanceID,

IntegerHolder currentSharpness
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetSharpness");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetSharpness"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentSharpness.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentSharpness")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredSharpness in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setSharpness(
		long instanceID,

int desiredSharpness
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetSharpness");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetSharpness"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredSharpness", 
						(new IntegerHolder(desiredSharpness)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentRedVideoGain out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getRedVideoGain(
		long instanceID,

IntegerHolder currentRedVideoGain
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetRedVideoGain");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetRedVideoGain"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentRedVideoGain.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentRedVideoGain")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredRedVideoGain in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setRedVideoGain(
		long instanceID,

int desiredRedVideoGain
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetRedVideoGain");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetRedVideoGain"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredRedVideoGain", 
						(new IntegerHolder(desiredRedVideoGain)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentGreenVideoGain out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getGreenVideoGain(
		long instanceID,

IntegerHolder currentGreenVideoGain
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetGreenVideoGain");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetGreenVideoGain"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentGreenVideoGain.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentGreenVideoGain")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredGreenVideoGain in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setGreenVideoGain(
		long instanceID,

int desiredGreenVideoGain
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetGreenVideoGain");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetGreenVideoGain"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredGreenVideoGain", 
						(new IntegerHolder(desiredGreenVideoGain)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentBlueVideoGain out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getBlueVideoGain(
		long instanceID,

IntegerHolder currentBlueVideoGain
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetBlueVideoGain");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetBlueVideoGain"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentBlueVideoGain.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentBlueVideoGain")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredBlueVideoGain in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setBlueVideoGain(
		long instanceID,

int desiredBlueVideoGain
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetBlueVideoGain");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetBlueVideoGain"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredBlueVideoGain", 
						(new IntegerHolder(desiredBlueVideoGain)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentRedVideoBlackLevel out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getRedVideoBlackLevel(
		long instanceID,

IntegerHolder currentRedVideoBlackLevel
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetRedVideoBlackLevel");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetRedVideoBlackLevel"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentRedVideoBlackLevel.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentRedVideoBlackLevel")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredRedVideoBlackLevel in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setRedVideoBlackLevel(
		long instanceID,

int desiredRedVideoBlackLevel
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetRedVideoBlackLevel");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetRedVideoBlackLevel"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredRedVideoBlackLevel", 
						(new IntegerHolder(desiredRedVideoBlackLevel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentGreenVideoBlackLevel out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getGreenVideoBlackLevel(
		long instanceID,

IntegerHolder currentGreenVideoBlackLevel
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetGreenVideoBlackLevel");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetGreenVideoBlackLevel"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentGreenVideoBlackLevel.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentGreenVideoBlackLevel")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredGreenVideoBlackLevel in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setGreenVideoBlackLevel(
		long instanceID,

int desiredGreenVideoBlackLevel
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetGreenVideoBlackLevel");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetGreenVideoBlackLevel"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredGreenVideoBlackLevel", 
						(new IntegerHolder(desiredGreenVideoBlackLevel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentBlueVideoBlackLevel out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getBlueVideoBlackLevel(
		long instanceID,

IntegerHolder currentBlueVideoBlackLevel
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetBlueVideoBlackLevel");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetBlueVideoBlackLevel"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentBlueVideoBlackLevel.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentBlueVideoBlackLevel")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredBlueVideoBlackLevel in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setBlueVideoBlackLevel(
		long instanceID,

int desiredBlueVideoBlackLevel
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetBlueVideoBlackLevel");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetBlueVideoBlackLevel"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredBlueVideoBlackLevel", 
						(new IntegerHolder(desiredBlueVideoBlackLevel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentColorTemperature out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getColorTemperature(
		long instanceID,

IntegerHolder currentColorTemperature
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetColorTemperature");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetColorTemperature"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentColorTemperature.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentColorTemperature")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredColorTemperature in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setColorTemperature(
		long instanceID,

int desiredColorTemperature
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetColorTemperature");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetColorTemperature"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredColorTemperature", 
						(new IntegerHolder(desiredColorTemperature)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentHorizontalKeystone out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getHorizontalKeystone(
		long instanceID,

IntegerHolder currentHorizontalKeystone
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetHorizontalKeystone");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetHorizontalKeystone"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentHorizontalKeystone.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentHorizontalKeystone")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredHorizontalKeystone in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setHorizontalKeystone(
		long instanceID,

int desiredHorizontalKeystone
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetHorizontalKeystone");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetHorizontalKeystone"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredHorizontalKeystone", 
						(new IntegerHolder(desiredHorizontalKeystone)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentVerticalKeystone out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getVerticalKeystone(
		long instanceID,

IntegerHolder currentVerticalKeystone
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetVerticalKeystone");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetVerticalKeystone"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentVerticalKeystone.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentVerticalKeystone")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * desiredVerticalKeystone in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setVerticalKeystone(
		long instanceID,

int desiredVerticalKeystone
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetVerticalKeystone");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetVerticalKeystone"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("DesiredVerticalKeystone", 
						(new IntegerHolder(desiredVerticalKeystone)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * currentMute out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getMute(
		long instanceID,

java.lang.String channel,

BooleanHolder currentMute
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetMute");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetMute"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentMute.setValue(BooleanHolder.toValue((java.lang.Boolean)_result.get("CurrentMute")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * desiredMute in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setMute(
		long instanceID,

java.lang.String channel,

boolean desiredMute
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetMute");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetMute"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		
		_parameters.put("DesiredMute", 
						(new BooleanHolder(desiredMute)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * currentVolume out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getVolume(
		long instanceID,

java.lang.String channel,

IntegerHolder currentVolume
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetVolume");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetVolume"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentVolume.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentVolume")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * desiredVolume in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setVolume(
		long instanceID,

java.lang.String channel,

int desiredVolume
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetVolume");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetVolume"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		
		_parameters.put("DesiredVolume", 
						(new IntegerHolder(desiredVolume)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * currentVolume out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getVolumeDB(
		long instanceID,

java.lang.String channel,

IntegerHolder currentVolume
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetVolumeDB");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetVolumeDB"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentVolume.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("CurrentVolume")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * desiredVolume in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setVolumeDB(
		long instanceID,

java.lang.String channel,

int desiredVolume
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetVolumeDB");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetVolumeDB"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		
		_parameters.put("DesiredVolume", 
						(new IntegerHolder(desiredVolume)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * minValue out  parameter

 * maxValue out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getVolumeDBRange(
		long instanceID,

java.lang.String channel,

IntegerHolder minValue,

IntegerHolder maxValue
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetVolumeDBRange");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetVolumeDBRange"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		minValue.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("MinValue")));
		maxValue.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("MaxValue")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * currentLoudness out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getLoudness(
		long instanceID,

java.lang.String channel,

BooleanHolder currentLoudness
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetLoudness");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetLoudness"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		currentLoudness.setValue(BooleanHolder.toValue((java.lang.Boolean)_result.get("CurrentLoudness")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * channel in  parameter

 * desiredLoudness in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setLoudness(
		long instanceID,

java.lang.String channel,

boolean desiredLoudness
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetLoudness");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetLoudness"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Channel", 
						(new StringHolder(channel)).getObject());
		
		_parameters.put("DesiredLoudness", 
						(new BooleanHolder(desiredLoudness)).getObject());
		

		Dictionary _result;
		try {
				_result = upnpAction.invoke(_parameters);
		} catch (UPnPException e) {
			throw e;
		} catch (Exception e) {
			throw new UPnPException(UPnPException.DEVICE_INTERNAL_ERROR,e.getMessage());
		}

		
	}

	

	
}
