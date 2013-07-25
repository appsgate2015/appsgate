
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


public class AVTransportProxyImpl implements CoreObjectSpec, AVTransport, UPnPEventListener {		

	private String 		userObjectName;
	private int			locationId;
	private String 		pictureId;
	
	private String 		serviceType ;
	private String 		serviceId ;

	private UPnPDevice  upnpDevice;
	private UPnPService upnpService;
	
	AVTransportProxyImpl(){
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
			return AVTransportProxyImpl.this;
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

 * currentURI in  parameter

 * currentURIMetaData in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setAVTransportURI(
		long instanceID,

java.lang.String currentURI,

java.lang.String currentURIMetaData
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetAVTransportURI");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetAVTransportURI"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("CurrentURI", 
						(new StringHolder(currentURI)).getObject());
		
		_parameters.put("CurrentURIMetaData", 
						(new StringHolder(currentURIMetaData)).getObject());
		

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

 * nextURI in  parameter

 * nextURIMetaData in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setNextAVTransportURI(
		long instanceID,

java.lang.String nextURI,

java.lang.String nextURIMetaData
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetNextAVTransportURI");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetNextAVTransportURI"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("NextURI", 
						(new StringHolder(nextURI)).getObject());
		
		_parameters.put("NextURIMetaData", 
						(new StringHolder(nextURIMetaData)).getObject());
		

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

 * nrTracks out  parameter

 * mediaDuration out  parameter

 * currentURI out  parameter

 * currentURIMetaData out  parameter

 * nextURI out  parameter

 * nextURIMetaData out  parameter

 * playMedium out  parameter

 * recordMedium out  parameter

 * writeStatus out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getMediaInfo(
		long instanceID,

LongHolder nrTracks,

StringHolder mediaDuration,

StringHolder currentURI,

StringHolder currentURIMetaData,

StringHolder nextURI,

StringHolder nextURIMetaData,

StringHolder playMedium,

StringHolder recordMedium,

StringHolder writeStatus
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetMediaInfo");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetMediaInfo"+" is not provided by service "+getAbstractObjectId());

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

		nrTracks.setValue(LongHolder.toValue((java.lang.Long)_result.get("NrTracks")));
		mediaDuration.setValue(StringHolder.toValue((java.lang.String)_result.get("MediaDuration")));
		currentURI.setValue(StringHolder.toValue((java.lang.String)_result.get("CurrentURI")));
		currentURIMetaData.setValue(StringHolder.toValue((java.lang.String)_result.get("CurrentURIMetaData")));
		nextURI.setValue(StringHolder.toValue((java.lang.String)_result.get("NextURI")));
		nextURIMetaData.setValue(StringHolder.toValue((java.lang.String)_result.get("NextURIMetaData")));
		playMedium.setValue(StringHolder.toValue((java.lang.String)_result.get("PlayMedium")));
		recordMedium.setValue(StringHolder.toValue((java.lang.String)_result.get("RecordMedium")));
		writeStatus.setValue(StringHolder.toValue((java.lang.String)_result.get("WriteStatus")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * currentTransportState out  parameter

 * currentTransportStatus out  parameter

 * currentSpeed out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getTransportInfo(
		long instanceID,

StringHolder currentTransportState,

StringHolder currentTransportStatus,

StringHolder currentSpeed
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetTransportInfo");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetTransportInfo"+" is not provided by service "+getAbstractObjectId());

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

		currentTransportState.setValue(StringHolder.toValue((java.lang.String)_result.get("CurrentTransportState")));
		currentTransportStatus.setValue(StringHolder.toValue((java.lang.String)_result.get("CurrentTransportStatus")));
		currentSpeed.setValue(StringHolder.toValue((java.lang.String)_result.get("CurrentSpeed")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * track out  parameter

 * trackDuration out  parameter

 * trackMetaData out  parameter

 * trackURI out  parameter

 * relTime out  parameter

 * absTime out  parameter

 * relCount out  parameter

 * absCount out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getPositionInfo(
		long instanceID,

LongHolder track,

StringHolder trackDuration,

StringHolder trackMetaData,

StringHolder trackURI,

StringHolder relTime,

StringHolder absTime,

IntegerHolder relCount,

IntegerHolder absCount
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetPositionInfo");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetPositionInfo"+" is not provided by service "+getAbstractObjectId());

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

		track.setValue(LongHolder.toValue((java.lang.Long)_result.get("Track")));
		trackDuration.setValue(StringHolder.toValue((java.lang.String)_result.get("TrackDuration")));
		trackMetaData.setValue(StringHolder.toValue((java.lang.String)_result.get("TrackMetaData")));
		trackURI.setValue(StringHolder.toValue((java.lang.String)_result.get("TrackURI")));
		relTime.setValue(StringHolder.toValue((java.lang.String)_result.get("RelTime")));
		absTime.setValue(StringHolder.toValue((java.lang.String)_result.get("AbsTime")));
		relCount.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("RelCount")));
		absCount.setValue(IntegerHolder.toValue((java.lang.Integer)_result.get("AbsCount")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * playMedia out  parameter

 * recMedia out  parameter

 * recQualityModes out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getDeviceCapabilities(
		long instanceID,

StringHolder playMedia,

StringHolder recMedia,

StringHolder recQualityModes
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetDeviceCapabilities");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetDeviceCapabilities"+" is not provided by service "+getAbstractObjectId());

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

		playMedia.setValue(StringHolder.toValue((java.lang.String)_result.get("PlayMedia")));
		recMedia.setValue(StringHolder.toValue((java.lang.String)_result.get("RecMedia")));
		recQualityModes.setValue(StringHolder.toValue((java.lang.String)_result.get("RecQualityModes")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * playMode out  parameter

 * recQualityMode out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getTransportSettings(
		long instanceID,

StringHolder playMode,

StringHolder recQualityMode
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetTransportSettings");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetTransportSettings"+" is not provided by service "+getAbstractObjectId());

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

		playMode.setValue(StringHolder.toValue((java.lang.String)_result.get("PlayMode")));
		recQualityMode.setValue(StringHolder.toValue((java.lang.String)_result.get("RecQualityMode")));
		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void stop(
		long instanceID
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Stop");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Stop"+" is not provided by service "+getAbstractObjectId());

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

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * speed in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void play(
		long instanceID,

java.lang.String speed
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Play");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Play"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Speed", 
						(new StringHolder(speed)).getObject());
		

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


	 */
	@SuppressWarnings("rawtypes")
	public void pause(
		long instanceID
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Pause");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Pause"+" is not provided by service "+getAbstractObjectId());

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

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void record(
		long instanceID
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Record");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Record"+" is not provided by service "+getAbstractObjectId());

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

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * unit in  parameter

 * target in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void seek(
		long instanceID,

java.lang.String unit,

java.lang.String target
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Seek");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Seek"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("Unit", 
						(new StringHolder(unit)).getObject());
		
		_parameters.put("Target", 
						(new StringHolder(target)).getObject());
		

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


	 */
	@SuppressWarnings("rawtypes")
	public void next(
		long instanceID
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Next");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Next"+" is not provided by service "+getAbstractObjectId());

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

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void previous(
		long instanceID
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("Previous");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"Previous"+" is not provided by service "+getAbstractObjectId());

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

		
	}


	/**
	 * This method is "add description here"	
 * instanceID in  parameter

 * newPlayMode in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setPlayMode(
		long instanceID,

java.lang.String newPlayMode
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetPlayMode");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetPlayMode"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("NewPlayMode", 
						(new StringHolder(newPlayMode)).getObject());
		

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

 * newRecordQualityMode in  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void setRecordQualityMode(
		long instanceID,

java.lang.String newRecordQualityMode
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("SetRecordQualityMode");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"SetRecordQualityMode"+" is not provided by service "+getAbstractObjectId());

		Dictionary _parameters = new Hashtable();
		
		
		_parameters.put("InstanceID", 
						(new LongHolder(instanceID)).getObject());
		
		_parameters.put("NewRecordQualityMode", 
						(new StringHolder(newRecordQualityMode)).getObject());
		

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

 * actions out  parameter


	 */
	@SuppressWarnings("rawtypes")
	public void getCurrentTransportActions(
		long instanceID,

StringHolder actions
	) throws UPnPException {
		
		UPnPAction upnpAction = upnpService.getAction("GetCurrentTransportActions");

		if (upnpAction == null)
			throw new UnsupportedOperationException("Action "+"GetCurrentTransportActions"+" is not provided by service "+getAbstractObjectId());

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

		actions.setValue(StringHolder.toValue((java.lang.String)_result.get("Actions")));
		
	}

	

	
}
