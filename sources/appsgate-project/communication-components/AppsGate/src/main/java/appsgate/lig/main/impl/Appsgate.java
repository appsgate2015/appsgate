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

import appsgate.lig.eude.interpreter.spec.EUDE_InterpreterSpec;
import appsgate.lig.main.impl.upnp.AppsGateServerDevice;
import appsgate.lig.main.impl.upnp.ServerInfoService;
import appsgate.lig.main.impl.upnp.StateVariableServerIP;
import appsgate.lig.main.impl.upnp.StateVariableServerURL;
import appsgate.lig.main.impl.upnp.StateVariableServerWebsocket;
import appsgate.lig.main.spec.AppsGateSpec;
import appsgate.lig.manager.space.spec.Space;
import appsgate.lig.manager.space.spec.Space.TYPE;
import appsgate.lig.manager.space.spec.SpaceManagerSpec;
import appsgate.lig.router.spec.RouterApAMSpec;



/**
 * This class is the central component for AppsGate server. It allow client part
 * to make methods call from HMI managers.
 * 
 * It expose AppGate server as an UPnP device to gather informations about it
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
	private final static Logger LOGGER = LoggerFactory.getLogger(Appsgate.class);

	/**
	 * HTTP service dependency resolve by iPojo. Allow to register HTML
	 * resources to the Felix HTTP server
	 */
	private HttpService httpService;

	/**
	 * The place manager ApAM component to handle the space manager service reference
	 */
	private SpaceManagerSpec spaceManager;

	/**
	 * Reference on the AppsGate Router to execute command on devices
	 */
	private RouterApAMSpec router;

	/**
	 * Reference to the EUDE interpreter to manage end user programs
	 */
	private EUDE_InterpreterSpec interpreter;
	
	
	private final String wsPort="8087";

	private final BundleContext context;
	private ServiceRegistration<?> serviceRegistration;

	private final AppsGateServerDevice upnpDevice;
	private ServerInfoService upnpService;
	private StateVariableServerIP serverIP;
	private StateVariableServerURL serverURL;
	private StateVariableServerWebsocket serverWebsocket;	

	/**
	 * Default constructor for AppsGate java object. it load UPnP device and
	 * services profiles and subscribes the corresponding listeners.
	 * 
	 */
	public Appsgate(BundleContext context) {
		LOGGER.debug("new AppsGate, BundleContext : " + context);
		this.context = context;
		upnpDevice = new AppsGateServerDevice(context);
		LOGGER.debug("UPnP Device instanciated");
		registerUpnpDevice();
		retrieveLocalAdress();

		LOGGER.info("AppsGate instanciated");
	}
	
	
	private void registerUpnpDevice() {
		Dictionary<String, Object> dict = upnpDevice.getDescriptions(null);
		serviceRegistration = context.registerService(
				UPnPDevice.class.getName(), upnpDevice, dict);
		LOGGER.debug("UPnP Device registered");
		
		upnpService = (ServerInfoService) upnpDevice.getService(ServerInfoService.SERVICE_ID);
		serverIP = (StateVariableServerIP) upnpService.getStateVariable(StateVariableServerIP.VAR_NAME);
		serverURL = (StateVariableServerURL) upnpService.getStateVariable(StateVariableServerURL.VAR_NAME);
		serverWebsocket = (StateVariableServerWebsocket) upnpService.getStateVariable(StateVariableServerWebsocket.VAR_NAME);
	}

	/**
	 * Called by APAM when an instance of this implementation is created
	 */
	public void newInst() {
		LOGGER.debug("AppsGate is starting");

		if (httpService != null) {
			final HttpContext httpContext = httpService.createDefaultHttpContext();
			final Dictionary<String, String> initParams = new Hashtable<String, String>();
			initParams.put("from", "HttpService");
			try {
				httpService.registerResources("/appsgate", "/WEB", httpContext);
				LOGGER.debug("Registered URL : "
						+ httpContext.getResource("/WEB"));
				LOGGER.info("Appsgate mains HTML pages registered.");
			} catch (NamespaceException ex) {
				LOGGER.error("NameSpace exception");
			}
		}
	}

	/**
	 * Called by APAM when an instance of this implementation is removed
	 */
	public void deleteInst() {
		LOGGER.info("AppsGate is stopping");
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
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("ref", objectId);
		ArrayList<Space> spaces = spaceManager.getSpacesWithPropertiesValue(properties);
		for(Space space : spaces) {
			space.addProperty("name", name);
			
			//Send notification
			try {
				JSONObject notify = new JSONObject();
				notify.put("reason", "spaceNameChanged");
				notify.put("spaceId", space.getId());
				notify.put("properties", "");
			
				spaceManager.spaceUpdated(notify);
			}catch(JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getUserObjectName(String objectId, String user) {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("ref", objectId);
		ArrayList<Space> spaces = spaceManager.getSpacesWithPropertiesValue(properties);
		return spaces.get(0).getName();
	}

	@Override
	public void deleteUserObjectName(String objectId, String user) {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("ref", objectId);
		ArrayList<Space> spaces = spaceManager.getSpacesWithPropertiesValue(properties);
		for(Space space : spaces) {
			space.removeProperty("name");
			
			//Send notification
			try {
				JSONObject notify = new JSONObject();
				notify.put("reason", "spaceNameRemoved");
				notify.put("spaceId", space.getId());
				notify.put("properties", "");
			
				spaceManager.spaceUpdated(notify);
			}catch(JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public JSONArray getJSONSpaces() {
		
		@SuppressWarnings("unchecked")
		ArrayList<Space> spaces =  (ArrayList<Space>) spaceManager.getSpaces().clone();
		JSONArray jsonspaceList = new JSONArray();
		
		for(Space space : spaces) {
			jsonspaceList.put(space.getDescription());
		}

		return jsonspaceList;
	}

	@Override
	public String newSpace(JSONObject place) {
		try {
			String parent = place.getString("parent");
			String spaceId = null;
			Space parentPlace = spaceManager.getSpace(parent);
			if(parentPlace != null) {
				String spaceName = place.getString("name");
				String category = place.getString("category");
				boolean isTagged = false;
				ArrayList<String> tagsList =  new ArrayList<String>();
				boolean isProperties = false;
				HashMap<String, String> propertiesMap =  new HashMap<String, String>();
				
				if(place.has("tags")) {
					JSONArray tags = place.getJSONArray("tags");
					int size = tags.length();
					int i = 0;
					while (i < size) {
						String tag = (String) tags.get(i);
						tagsList.add(tag);
						i++;
					}
					isTagged = true;
				}
				
				if(place.has("properties")){
					JSONArray tags = place.getJSONArray("properties");
					int size = tags.length();
					int i = 0;
					while (i < size) {
						JSONObject prop = tags.getJSONObject(i);
						propertiesMap.put(prop.getString("key"), prop.getString("value"));
						i++;
					}
					isProperties = true;
				}
				
				if( isTagged && isProperties) {
					spaceId = spaceManager.addSpace(TYPE.valueOf(category), tagsList, propertiesMap, spaceManager.getSpace(parent));
				}else {
					spaceId = spaceManager.addSpace(TYPE.valueOf(category), spaceManager.getSpace(parent));
					Space spaceRef = spaceManager.getSpace(spaceId);
					if(isTagged) {
						spaceRef.setTags(tagsList);
					}
					
					if(isProperties) {
						spaceRef.setProperties(propertiesMap);
					}
					
					if(place.has("name")) {
						spaceRef.addProperty("name", spaceName);
					}
				}
				
				return spaceId;
			}else {
				LOGGER.error("Ne parent place found with id: "+parent);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean removeSpace(String id) {
		Space space = spaceManager.getSpace(id);
		if(space != null) {
			return spaceManager.removeSpace(space);
		}
		return false;
	}

	@Override
	public boolean updateSpace(JSONObject space) {
		try {
			String spaceID = space.getString("id");
			Space spaceRef = spaceManager.getSpace(spaceID);
			boolean isSuccess = true; 
			JSONObject notify = new JSONObject();
			notify.put("reason", "updateSpace");
			notify.put("spaceId", spaceID);
			
			//Rename the space
			if(space.has("name")) {
				spaceRef.addProperty("name", space.getString("name"));
				isSuccess &= true;
				notify.put("properties", "");
			}
			//Move the space 
			if(space.has("parent")) {
				isSuccess &= spaceManager.moveSpace(spaceRef, spaceManager.getSpace(space.getString("parent")));
				notify.put("parentId", "");
			}
			
			//Set or clear the tag list
			if(space.has("taglist")) {
				
				JSONArray tagArray = space.getJSONArray("taglist");
				
				if(tagArray.length() == 0) {
					spaceRef.clearTags();
				}else {
					ArrayList<String> tags = new ArrayList<String>();
					int nbTag = tagArray.length();
					for(int i=0; i<nbTag; i++) {
						tags.add(tagArray.getString(i));
					}
					spaceRef.setTags(tags);
				}
				notify.put("tags", "");
			}
			
			//Set or clear the property list
			if(space.has("proplist")) {
				
				JSONArray propArray = space.getJSONArray("proplist");
				
				if(propArray.length() == 0) {
					spaceRef.clearProperties();
				}else {
					HashMap<String, String> props = new HashMap<String, String>();
					int nbProp = propArray.length();
					for(int i=0; i<nbProp; i++) {
						JSONObject prop = propArray.getJSONObject(i);
						props.put(prop.getString("key"), prop.getString("value"));
					}
					spaceRef.setProperties(props);
				}
				notify.put("properties", "");
			}
			
			//Send notification
			spaceManager.spaceUpdated(notify);
			
			return isSuccess;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public JSONObject getSpaceInfo(String spaceId) {
		return spaceManager.getSpace(spaceId).getDescription();
	}


	@Override
	public JSONArray getSpacesByName(String name) {
		JSONArray spaceByName = new JSONArray();
		ArrayList<Space> spacesList = spaceManager.getSpacesWithName(name);
		for(Space space : spacesList){
			spaceByName.put(space.getDescription());
		}
		return spaceByName;
	}


	@Override
	public JSONArray getSpacesWithTags(JSONArray tags) {
		int tagNb  = tags.length();
		ArrayList<String> tagsList = new ArrayList<String>();
		for(int i=0; i<tagNb; i++) {
			try {
				tagsList.add(tags.getString(i));
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		
		JSONArray spaceByTag = new JSONArray();
		ArrayList<Space> spacesList = spaceManager.getSpacesWithTags(tagsList);
		for(Space space : spacesList){
			spaceByTag.put(space.getDescription());
		}
		return spaceByTag;
	}


	@Override
	public JSONArray getSpacesWithProperties(JSONArray keys) {
		int keysNb  = keys.length();
		ArrayList<String> keysList = new ArrayList<String>();
		for(int i=0; i<keysNb; i++) {
			try {
				keysList.add(keys.getString(i));
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		
		JSONArray spaceByProp = new JSONArray();
		ArrayList<Space> spacesList = spaceManager.getSpacesWithProperties(keysList);
		for(Space space : spacesList){
			spaceByProp.put(space.getDescription());
		}
		return spaceByProp;
	}


	@Override
	public JSONArray getSpacesWithPropertiesValue(JSONArray properties) {
		int propertiesNb  = properties.length();
		HashMap<String, String> propertiesList = new HashMap<String, String>();
		for(int i=0; i<propertiesNb; i++) {
			try {
				JSONObject prop = properties.getJSONObject(i);
				propertiesList.put(prop.getString("key"), prop.getString("value"));
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		
		JSONArray spaceByPropValue = new JSONArray();
		ArrayList<Space> spacesList = spaceManager.getSpacesWithPropertiesValue(propertiesList);
		for(Space space : spacesList){
			spaceByPropValue.put(space.getDescription());
		}
		return spaceByPropValue;
	}
	

	@Override
	public JSONObject getTreeDescription() {
		return spaceManager.getTreeDescription();
	}


	@Override
	public JSONObject getTreeDescription(String rootId) {
		Space root = spaceManager.getSpace(rootId);
		return spaceManager.getTreeDescription(root);
	}


	@Override
	public JSONObject getRootSpace() {
		return spaceManager.getRootSpace().getDescription();
	}


	@Override
	public boolean addTag(String placeId, String tag) {
		Space spaceRef = spaceManager.getSpace(placeId);
		if(spaceRef != null) {
			if(spaceRef.addTag(tag)) {
				//Send notification
				try {
					JSONObject notify = new JSONObject();
					notify.put("reason", "addTag");
					notify.put("spaceId", placeId);
					notify.put("tags", "");
			
					spaceManager.spaceUpdated(notify);
				}catch(JSONException e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;
	}


	@Override
	public boolean removeTag(String placeId, String tag) {
		Space spaceRef = spaceManager.getSpace(placeId);
		if(spaceRef != null) {
			if(spaceRef.removeTag(tag)) {
				//Send notification
				try {
					JSONObject notify = new JSONObject();
					notify.put("reason", "removeTag");
					notify.put("spaceId", placeId);
					notify.put("tags", "");
			
					spaceManager.spaceUpdated(notify);
				}catch(JSONException e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;

	}


	@Override
	public boolean addProperty(String placeId, String key, String value) {
		Space spaceRef = spaceManager.getSpace(placeId);
		if(spaceRef != null) {
			if( spaceRef.addProperty(key, value)) {
				//Send notification
				try {
					JSONObject notify = new JSONObject();
					notify.put("reason", "addProperty");
					notify.put("spaceId", placeId);
					notify.put("properties", "");
			
					spaceManager.spaceUpdated(notify);
				}catch(JSONException e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;

	}


	@Override
	public boolean removeProperty(String placeId, String key) {
		Space spaceRef = spaceManager.getSpace(placeId);
		if(spaceRef != null) {
			if(spaceRef.removeProperty(key)) {
				//Send notification
				try {
					JSONObject notify = new JSONObject();
					notify.put("reason", "removeProperty");
					notify.put("spaceId", placeId);
					notify.put("properties", "");
			
					spaceManager.spaceUpdated(notify);
				}catch(JSONException e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;

	}
	
	@Override
	public JSONArray getDevicesInSpaces(JSONArray typeList, JSONArray spaces) {
		ArrayList<Space> spacesList = new ArrayList<Space>();
		ArrayList<String> typeArray = new ArrayList<String>();
		JSONArray coreObject = new JSONArray();
		
		try {
			
			//First get all Space from their space id, if the spaces array if empty
			//we get only the root space
			int size = spaces.length();
			int i = 0;
			if(size > 0) {
				while(i < size){
					spacesList.add(spaceManager.getSpace(spaces.getString(i)));
					i++;
				}
			}else {
				spacesList.add(spaceManager.getRootSpace());
			}
			
			//Second convert unusable JSONArray type to an ArrayList<String>
			size = typeList.length();
			i = 0;
			while(i < size){
				typeArray.add(typeList.getString(i));
				i++;
			}
			
			// For each selected space we check if one of its descendant
			// match any type in the type list
			for(Space place : spacesList) {
				ArrayList<Space> subSpaces = place.getSubSpaces();
				for(Space subSpace : subSpaces) {
					//TODO the TYPE.DEVICE check will be move latter whit service integration
					//If no type is specified we get all devices
					if(size > 0) {
						if(subSpace.getType().equals(TYPE.DEVICE) && typeArray.contains(subSpace.getPropertyValue("type"))) {
							coreObject.put(router.getDevice(subSpace.getPropertyValue("ref")));
						}
					}else {
						if(subSpace.getType().equals(TYPE.DEVICE)) {
							coreObject.put(router.getDevice(subSpace.getPropertyValue("ref")));
						}
					}
				}
			}
		}catch(JSONException jsonEx) {
			jsonEx.printStackTrace();
		}
		
		return coreObject;
	}

	@Override
	public JSONObject getPlaces(String habitatID) {
		JSONObject jsonPlaces = null;
		for(Space subSpace : spaceManager.getSpace(habitatID).getChildren()){
			if(subSpace.getName().contentEquals("places")) {
				jsonPlaces = subSpace.getDescription();
				break;
			}
		}
		return jsonPlaces;
	}

	@Override
	public JSONObject getPlaceInfo(String habitatID, String placeID) {
		JSONObject jsonPlaces = null;
		for(Space subSpace : spaceManager.getSpace(habitatID).getChildren()){
			if(subSpace.getName().contentEquals("places")) {
				jsonPlaces = subSpace.getSubSpace(placeID).getDescription();
				break;
			}
		}
		return jsonPlaces;
	}

	@Override
	public JSONArray getUsers() {
		JSONArray userArray = new JSONArray();
		Space userRoot = spaceManager.getUserRoot();
		userArray.put(userRoot.getDescription());
		for(Space user : userRoot.getSubSpaces()) {
			userArray.put(user.getDescription());
		}
		return userArray;
	}

	@Override
	public String createUser(String login, String password, String lastName,
			String firstName, String role) {
		
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("login", login);
		properties.put("name", login);
		properties.put("password", password);
		properties.put("lastname", lastName);
		properties.put("firsname", firstName);
		properties.put("role", role);
		
		return spaceManager.addSpace(TYPE.USER, properties, spaceManager.getUserRoot());
	}

	@Override
	public boolean deleteUser(String id, String password) {
		Space userSpace = spaceManager.getSpace(id);
		//TODO manage to authenticate the user with the password value
		// see the former user base to authenticate
		return spaceManager.removeSpace(userSpace);
	}

	@Override
	public JSONObject getUserDetails(String id) {
		Space userSpace = spaceManager.getSpace(id);
		return userSpace.getDescription();
	}

//	@Override
//	public JSONObject getUserFullDetails(String id) {
//		JSONObject obj = new JSONObject();
//
//		try {
//			obj.put("user", userManager.getUserDetails(id));
//			obj.put("devices", userManager.getAssociatedDevices(id));
//			obj.put("accounts", userManager.getAccountsDetails(id));
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//		return obj;
//	}

	@Override
	public boolean checkIfLoginIsFree(String login) {
		return spaceManager.getSpacesWithName(login).isEmpty();
	}

//	@Override
//	public boolean synchronizeAccount(String id, String password,
//			JSONObject accountDetails) {
//		return userManager.addAccount(id, password, accountDetails);
//	}
//
//	@Override
//	public boolean desynchronizedAccount(String id, String password,
//			JSONObject accountDetails) {
//		return userManager.removeAccount(id, password, accountDetails);
//	}
//
//	@Override
//	public boolean associateDevice(String id, String password, String deviceId) {
//		return userManager.addDevice(id, password, deviceId);
//	}
//
//	@Override
//	public boolean separateDevice(String id, String password, String deviceId) {
//		return userManager.removeDevice(id, password, deviceId);
//	}

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
	
	@Override
	public void addNewDeviceSpace(JSONObject description) {
		Space deviceRoot = spaceManager.getDeviceRoot(spaceManager.getCurrentHabitat());
		try {
			//Looking for the device space category
			String type  = description.getString("type");
			Space deviceCat = null;
			for(Space child : deviceRoot.getChildren()) {
				if(child.getType().equals(TYPE.CATEGORY) && child.getPropertyValue("deviceType").contentEquals(type)){
					deviceCat = child;
					break;
				}
			}
		
			if(deviceCat == null) { //if no category exist for this device type we create it.
				HashMap<String, String> properties = new HashMap<String, String>();
				properties.put("deviceType", type);
				String spaceId = spaceManager.addSpace(TYPE.CATEGORY, properties, deviceRoot);
				deviceCat = spaceManager.getSpace(spaceId);
			}
		
			//Now we create the device space...
			//... and we add it to the device category
			HashMap<String, String> deviceProperties = new HashMap<String, String>();
			deviceProperties.put("deviceType", type);
			deviceProperties.put("ref", description.getString("id"));
			spaceManager.addSpace(TYPE.DEVICE, deviceProperties, deviceCat);
			
		}catch(JSONException jsonex) {
			jsonex.printStackTrace();
		}
		
	}


	@Override
	public void removeDeviceSpace(String deviceId, String type) {
		Space deviceRoot = spaceManager.getDeviceRoot(spaceManager
				.getCurrentHabitat());

		// Looking for the device space category
		Space deviceCat = null;
		for (Space child : deviceRoot.getChildren()) {
			if (child.getType().equals(TYPE.CATEGORY)
					&& child.getPropertyValue("deviceType").contentEquals(type)) {
				deviceCat = child;
				break;
			}
		}

		Space deviceSpace = null;
		// Looking for the device space in the category children
		for (Space child : deviceCat.getChildren()) {
			if (child.getPropertyValue("ref").contentEquals(deviceId)) {
				deviceSpace = child;
				break;
			}
		}

		// remove the device auto manage space from the space manager
		spaceManager.removeSpace(deviceSpace);
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
						LOGGER.debug("The newtwork interface {} will be inspected.",netint.getDisplayName());
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
			LOGGER.debug("State Variable name : "+serverIP.getName()+", value : "+serverIP.getCurrentStringValue());
			serverURL.setStringValue("http://"+serverIP.getCurrentStringValue()+ "/index.html");
			LOGGER.debug("State Variable name : "+serverURL.getName()+", value : "+serverURL.getCurrentStringValue());
			serverWebsocket.setStringValue("http://"+serverIP.getCurrentStringValue()+ ":"+wsPort+"/");
			LOGGER.debug("State Variable name : "+serverWebsocket.getName()+", value : "+serverWebsocket.getCurrentStringValue());

		} catch (UnknownHostException e) {
			LOGGER.debug("Unknown host: ");
			e.printStackTrace();
		} catch (SocketException e) {
			LOGGER.debug("Socket exception for UPnP: ");
			e.printStackTrace();
		}
	}

}
