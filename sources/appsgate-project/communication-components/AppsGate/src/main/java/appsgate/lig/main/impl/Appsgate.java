package appsgate.lig.main.impl;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.upnp.UPnPDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.context.device.name.table.spec.DeviceNameTableSpec;
import appsgate.lig.context.userbase.spec.UserBaseSpec;
import appsgate.lig.eude.interpreter.spec.EUDE_InterpreterSpec;
import appsgate.lig.main.impl.upnp.AppsGateServerDevice;
import appsgate.lig.main.impl.upnp.ServerInfoService;
import appsgate.lig.main.impl.upnp.StateVariableServerIP;
import appsgate.lig.main.impl.upnp.StateVariableServerURL;
import appsgate.lig.main.impl.upnp.StateVariableServerWebsocket;
import appsgate.lig.main.spec.AppsGateSpec;
import appsgate.lig.manager.place.spec.PlaceManagerSpec;
import appsgate.lig.manager.place.spec.SymbolicPlace;
import appsgate.lig.router.spec.RouterApAMSpec;

/**
 * This class is the central component for AppsGate server. It allow client part
 * to make methods call from HMI managers.
 * 
 * It expose Appsgate server as an UPnP device to gather informations about it
 * through the SSDP discovery protocol
 * 
 * @author Cédric Gérard
 * @since April 23, 2013
 * @version 1.0.0
 * 
 */
public class Appsgate implements AppsGateSpec {

	/**
	 * 
	 * static class logger member
	 */
	private static Logger logger = LoggerFactory.getLogger(Appsgate.class);

	/**
	 * HTTP service dependency resolve by iPojo. Allow to register HTML
	 * resources to the Felix HTTP server
	 */
	private HttpService httpService;

	/**
	 * Table for deviceId, user and device name association
	 */
	private DeviceNameTableSpec deviceNameTable;

	/**
	 * The place manager ApAM component to handle the object place
	 */
	private PlaceManagerSpec placeManager;

	/**
	 * The user manager ApAM component to handle the user base
	 */
	private UserBaseSpec userManager;

	/**
	 * Reference on the AppsGate Router to execute command on devices
	 */
	private RouterApAMSpec router;

	/**
	 * Reference to the EUDE interpreter to manage end user programs
	 */
	private EUDE_InterpreterSpec interpreter;
	
	
	private String wsPort="8087";

	private BundleContext context;
	private ServiceRegistration<?> serviceRegistration;

	private AppsGateServerDevice upnpDevice;
	private ServerInfoService upnpService;
	private StateVariableServerIP serverIP;
	private StateVariableServerURL serverURL;
	private StateVariableServerWebsocket serverWebsocket;	

	/**
	 * Default constructor for Appsgate java object. it load UPnP device and
	 * services profiles and subscribes the corresponding listeners.
	 * 
	 */
	public Appsgate(BundleContext context) {
		logger.debug("new AppsGate, BundleContext : " + context);
		this.context = context;
		upnpDevice = new AppsGateServerDevice(context);
		logger.debug("UPnP Device instanciated");
		registerUpnpDevice();
		retrieveLocalAdress();

		logger.info("AppsGate instanciated");
	}
	
	
	private void registerUpnpDevice() {
		Dictionary<String, Object> dict = upnpDevice.getDescriptions(null);
		serviceRegistration = context.registerService(
				UPnPDevice.class.getName(), upnpDevice, dict);
		logger.debug("UPnP Device registered");
		
		upnpService = (ServerInfoService) upnpDevice.getService(ServerInfoService.SERVICE_ID);
		serverIP = (StateVariableServerIP) upnpService.getStateVariable(StateVariableServerIP.VAR_NAME);
		serverURL = (StateVariableServerURL) upnpService.getStateVariable(StateVariableServerURL.VAR_NAME);
		serverWebsocket = (StateVariableServerWebsocket) upnpService.getStateVariable(StateVariableServerWebsocket.VAR_NAME);
	}

	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		logger.debug("AppsGate is starting");

		if (httpService != null) {
			final HttpContext httpContext = httpService.createDefaultHttpContext();
			final Dictionary<String, String> initParams = new Hashtable<String, String>();
			initParams.put("from", "HttpService");
			try {
				httpService.registerResources("/appsgate", "/WEB", httpContext);
				logger.debug("Registered URL : "
						+ httpContext.getResource("/WEB"));
				logger.info("Appsgate mains HTML pages registered.");
			} catch (NamespaceException ex) {
				logger.error("NameSpace exception");
			}
		}
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		logger.info("AppsGate is stopping");
		httpService.unregister("/appsgate");
	}

	@Override
	public JSONArray getDevices() {
		return router.getDevices();
	}
	
	@Override
	public JSONObject getDevice(String deviceId) {
		return router.getDevice(deviceId);
	}
	
	@Override
	public JSONArray getDevices(String type) {
		return router.getDevices(type);
	}

	@Override
	public void setUserObjectName(String objectId, String user, String name) {
		deviceNameTable.addName(objectId, user, name);

	}

	@Override
	public String getUserObjectName(String objectId, String user) {
		return deviceNameTable.getName(objectId, user);
	}

	@Override
	public void deleteUserObjectName(String objectId, String user) {
		deviceNameTable.deleteName(objectId, user);
	}

	@Override
	public JSONArray getPlaces() {
		return placeManager.getJSONPlaces();
	}

	@Override
	public String newPlace(JSONObject place) {
		try {
			String parent = place.getString("parent");
			SymbolicPlace parentPlace = placeManager.getSymbolicPlace(parent);
			if(parentPlace != null) {
				String placeId = placeManager.addPlace(place.getString("name"), parent);
				JSONArray devices = place.getJSONArray("devices");
				int size = devices.length();
				int i = 0;
				while (i < size) {
					String objId = (String) devices.get(i);
					placeManager.moveObject(objId,
							placeManager.getCoreObjectPlaceId(objId), placeId);
					i++;
				}
				
				return placeId;
			}else {
				logger.error("Ne parent place found with id: "+parent);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean removePlace(String id) {
		return placeManager.removePlace(id);
	}

	@Override
	public boolean updatePlace(JSONObject place) {
		try {
			String placeID = place.getString("id");
			boolean isSuccess = true; 
			
			//Rename the place
			if(place.has("name")) {
				isSuccess &= placeManager.renamePlace(placeID, place.getString("name"));
			}
			//Move the place 
			if(place.has("parent")) {
				isSuccess &= placeManager.movePlace(placeID, place.getString("parent"));
			}
			
			//Set or clear the tag list
			if(place.has("taglist")) {
				
				JSONArray tagArray = place.getJSONArray("taglist");
				
				if(tagArray.length() == 0) {
					placeManager.clearTagsList(placeID);
				}else {
					ArrayList<String> tags = new ArrayList<String>();
					int nbTag = tagArray.length();
					for(int i=0; i<nbTag; i++) {
						tags.add(tagArray.getString(i));
					}
					placeManager.setTagsList(placeID, tags);
				}
			}
			
			//Set or clear the property list
			if(place.has("proplist")) {
				
				JSONArray propArray = place.getJSONArray("proplist");
				
				if(propArray.length() == 0) {
					placeManager.clearPropertiesList(placeID);
				}else {
					HashMap<String, String> props = new HashMap<String, String>();
					int nbProp = propArray.length();
					for(int i=0; i<nbProp; i++) {
						JSONObject prop = propArray.getJSONObject(i);
						props.put(prop.getString("key"), prop.getString("value"));
					}
					placeManager.setProperties(placeID, props);
				}
			}
			
			// Set or clear the core object list
			if(place.has("coreObjectlist")) {
				
				JSONArray tagArray = place.getJSONArray("coreObjectlist");
				
				if(tagArray.length() == 0) {
					placeManager.removeAllCoreObject(placeID);
				}else {
					int nbTag = tagArray.length();
					for(int i=0; i<nbTag; i++) {
						String objId = tagArray.getString(i);
						placeManager.moveObject(objId, placeManager.getCoreObjectPlaceId(objId), placeID);
					}
				}
			}
			
			
			return isSuccess;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean moveDevice(String objId, String srcPlaceId, String destPlaceId) {
		return placeManager.moveObject(objId, srcPlaceId, destPlaceId);
	}
	
	@Override
	public JSONObject getPlaceInfo(String placeId) {
		return placeManager.getSymbolicPlace(placeId).getDescription();
	}


	@Override
	public JSONArray getPlacesByName(String name) {
		JSONArray placeByName = new JSONArray();
		ArrayList<SymbolicPlace> placesList = placeManager.getPlacesWithName(name);
		for(SymbolicPlace place : placesList){
			placeByName.put(place.getDescription());
		}
		return placeByName;
	}


	@Override
	public JSONArray getPlacesWithTags(JSONArray tags) {
		int tagNb  = tags.length();
		ArrayList<String> tagsList = new ArrayList<String>();
		for(int i=0; i<tagNb; i++) {
			try {
				tagsList.add(tags.getString(i));
			} catch (JSONException e) {
				logger.error(e.getMessage());
			}
		}
		
		JSONArray placeByTag = new JSONArray();
		ArrayList<SymbolicPlace> placesList = placeManager.getPlacesWithTags(tagsList);
		for(SymbolicPlace place : placesList){
			placeByTag.put(place.getDescription());
		}
		return placeByTag;
	}


	@Override
	public JSONArray getPalcesWithProperties(JSONArray keys) {
		int keysNb  = keys.length();
		ArrayList<String> keysList = new ArrayList<String>();
		for(int i=0; i<keysNb; i++) {
			try {
				keysList.add(keys.getString(i));
			} catch (JSONException e) {
				logger.error(e.getMessage());
			}
		}
		
		JSONArray placeByProp = new JSONArray();
		ArrayList<SymbolicPlace> placesList = placeManager.getPlacesWithProperties(keysList);
		for(SymbolicPlace place : placesList){
			placeByProp.put(place.getDescription());
		}
		return placeByProp;
	}


	@Override
	public JSONArray getPalcesWithPropertiesValue(JSONArray properties) {
		int propertiesNb  = properties.length();
		HashMap<String, String> propertiesList = new HashMap<String, String>();
		for(int i=0; i<propertiesNb; i++) {
			try {
				JSONObject prop = properties.getJSONObject(i);
				propertiesList.put(prop.getString("key"), prop.getString("value"));
			} catch (JSONException e) {
				logger.error(e.getMessage());
			}
		}
		
		JSONArray placeByPropValue = new JSONArray();
		ArrayList<SymbolicPlace> placesList = placeManager.getPlacesWithPropertiesValue(propertiesList);
		for(SymbolicPlace place : placesList){
			placeByPropValue.put(place.getDescription());
		}
		return placeByPropValue;
	}


	@Override
	public JSONObject getRootPlace() {
		return placeManager.getRootPlace().getDescription();
	}


	@Override
	public boolean addTag(String placeId, String tag) {
		return placeManager.addTag(placeId, tag);
	}


	@Override
	public boolean removeTag(String placeId, String tag) {
		return placeManager.removeTag(placeId, tag);
	}


	@Override
	public boolean addProperty(String placeId, String key, String value) {
		return placeManager.addProperty(placeId, key, value);
	}


	@Override
	public boolean removeProperty(String placeId, String key) {
		return placeManager.removeProperty(placeId, key);
	}

	@Override
	public String getCoreObjectPlaceId(String objId) {
		return placeManager.getCoreObjectPlaceId(objId);
	}

	@Override
	public JSONArray getUsers() {
		return userManager.getUsers();
	}

	@Override
	public boolean createUser(String id, String password, String lastName,
			String firstName, String role) {
		return userManager.adduser(id, password, lastName, lastName, role);
	}

	@Override
	public boolean deleteUser(String id, String password) {
		return userManager.removeUser(id, password);
	}

	@Override
	public JSONObject getUserDetails(String id) {
		return userManager.getUserDetails(id);
	}

	@Override
	public JSONObject getUserFullDetails(String id) {
		JSONObject obj = new JSONObject();

		try {
			obj.put("user", userManager.getUserDetails(id));
			obj.put("devices", userManager.getAssociatedDevices(id));
			obj.put("accounts", userManager.getAccountsDetails(id));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return obj;
	}

	@Override
	public boolean checkIfIdIsFree(String id) {
		return userManager.checkIfIdIsFree(id);
	}

	@Override
	public boolean synchronizeAccount(String id, String password,
			JSONObject accountDetails) {
		return userManager.addAccount(id, password, accountDetails);
	}

	@Override
	public boolean desynchronizedAccount(String id, String password,
			JSONObject accountDetails) {
		return userManager.removeAccount(id, password, accountDetails);
	}

	@Override
	public boolean associateDevice(String id, String password, String deviceId) {
		return userManager.addDevice(id, password, deviceId);
	}

	@Override
	public boolean separateDevice(String id, String password, String deviceId) {
		return userManager.removeDevice(id, password, deviceId);
	}

	@Override
	public boolean addProgram(JSONObject jsonProgram) {
		return interpreter.addProgram(jsonProgram);
	}

	@Override
	public boolean removeProgram(String programId) {
		return interpreter.removeProgram(programId);
	}

	@Override
	public boolean updateProgram(JSONObject jsonProgram) {
		return interpreter.update(jsonProgram);
	}

	@Override
	public boolean callProgram(String programId) {
		return interpreter.callProgram(programId);
	}

	@Override
	public boolean stopProgram(String programId) {
		return interpreter.stopProgram(programId);
	}

	@Override
	public boolean pauseProgram(String programId) {
		return interpreter.pauseProgram(programId);
	}

	@Override
	public JSONArray getPrograms() {
		HashMap<String, JSONObject> map = interpreter.getListPrograms();
		JSONArray programList = new JSONArray();
		for (String key : map.keySet()) {
			programList.put(map.get(key));
		}
		return programList;
	}

	@Override
	public boolean isProgramActive(String programId) {
		return interpreter.isProgramActive(programId);
	}

	@Override
	public void shutdown() {
		BundleContext ctx = FrameworkUtil.getBundle(Appsgate.class)
				.getBundleContext();
		Bundle systemBundle = ctx.getBundle(0);
		try {
			systemBundle.stop();
		} catch (BundleException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void restart() {
		BundleContext ctx = FrameworkUtil.getBundle(Appsgate.class)
				.getBundleContext();
		Bundle systemBundle = ctx.getBundle(0);
		try {
			systemBundle.update();
		} catch (BundleException e) {
			e.printStackTrace();
		}
	}

	private void retrieveLocalAdress() {
		// initiate UPnP state variables
		try {


			Inet4Address localAddress = (Inet4Address) InetAddress.getLocalHost();
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				if (!netint.isLoopback() && !netint.isVirtual()
						&& netint.isUp()) { // TODO check also if its the local
											// network. but It will difficult to
											// find automatically the right
											// network interface
					if(!netint.getDisplayName().contentEquals("tun0")) {
						logger.debug("The newtwork interface {} will be inspected.",netint.getDisplayName());
						Enumeration<InetAddress> addresses = netint.getInetAddresses();
						for (InetAddress address : Collections.list(addresses)) {
							if (address instanceof Inet4Address) {
								localAddress = (Inet4Address) address;
								break;
							}
						}
					}
				}
			}
			
			serverIP.setStringValue(localAddress.getHostAddress());
			logger.debug("State Variable name : "+serverIP.getName()+", value : "+serverIP.getCurrentStringValue());
			serverURL.setStringValue("http://"+serverIP.getCurrentStringValue()+ "/index.html");
			logger.debug("State Variable name : "+serverURL.getName()+", value : "+serverURL.getCurrentStringValue());
			serverWebsocket.setStringValue("http://"+serverIP.getCurrentStringValue()+ ":"+wsPort+"/");
			logger.debug("State Variable name : "+serverWebsocket.getName()+", value : "+serverWebsocket.getCurrentStringValue());

		} catch (UnknownHostException e) {
			logger.debug("Unknown host: ");
			e.printStackTrace();
		} catch (SocketException e) {
			logger.debug("Socket exception for UPnP: ");
			e.printStackTrace();
		}
	}


	@Override
	public JSONArray getDevicesInSpaces(JSONArray typeList, JSONArray places) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONArray getSubtypes(JSONArray typeList) {
		// TODO Auto-generated method stub
		return null;
	}

}
