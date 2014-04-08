package appsgate.lig.ehmi.spec;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * EHMI specification that define all method that a client can remote call
 * to interact with the AppsGate system.
 * 
 * @author Cedric Gérard
 * @since June 19, 2013
 * @version 1.0.0
 *
 */
public interface EHMIProxySpec {
	
	/***************************/
	/**   Device management    */
	/***************************/
	
	/**
	 * Get all the devices description
         * @return 
	 */
	public JSONArray getDevices();
	
	/**
	 * Get device details
	 * @param deviceId the targeted device identifier
	 * @return the device description as a JSONObject
	 */
	public JSONObject getDevice(String deviceId);
	
	/**
	 * Get all the devices of a specify user type
	 * @param type the type to filter devices
	 * @return the device list as a JSONArray
	 */
	public JSONArray getDevices(String type);

	
	/***************************/
	/** Device properties management */
	/***************************/
	
	/**
	 * Call AppsGate to add a user object name 
	 * @param objectId the object
	 * @param user the user that name this object
	 * @param name the new name of this object
	 */
	public void setUserObjectName(String objectId, String user, String name);
	
	/**
	 * Get the name of an object for a specific user
	 * @param objectId the object
	 * @param user the user who ask
	 * @return the name of the object named by user
	 */
	public String getUserObjectName(String objectId, String user);
	
	/**
	 * Delete an name for an object set by a user
	 * @param objectId the object
	 * @param user the user who give the name to this object
	 */
	public void deleteUserObjectName(String objectId, String user);
	
	/**
	 * Add grammar in the context properties manager for a new device type
	 * @param deviceType the type of device
	 * @param grammarDescription the grammar associated to the device type
	 * @return true if the grammar is really new, false if the grammar has been replaced
	 */
	public boolean addGrammar(String deviceType, JSONObject grammarDescription);
	
	/**
	 * Remove grammar associated to a device type
	 * @param deviceType the device type from which to remove the grammar
	 * @return true if the grammar has been removed, false otherwise
	 */
	public boolean removeGrammar(String deviceType);

	/**
	 * Get the grammar associated to a device type
	 * @param deviceType the device type from which to get the grammar
	 * @return the grammar as a JSONObject
	 */
	public JSONObject getGrammarFromType(String deviceType);
	
	
	/***************************/
	/**    Place management    */
	/***************************/

	/**
	 * Call AppsGate to get all existing place definition.
	 * @return a JSON array that describe each place.
	 */
	public JSONArray getPlaces();

	/**
	 * Add a new place and move object in it.
	 * @param place the new place description and the list of object to move in
	 */
	public void newPlace(JSONObject place);

	/**
	 * Update a place on the smart place
	 * @param place the new place description
	 */
	public void updatePlace(JSONObject place);

	/**
	 * Remove a place from the smart place
	 * @param id the place identifier
	 */
	public void removePlace(String id);

	/**
	 * Move a device in a specified place
	 * @param objId the object to move
	 * @param srcPlaceId the previous place of this object
	 * @param destPlaceId the destination of this object
	 */
	
	public void moveDevice(String objId, String srcPlaceId, String destPlaceId);
	
	/**
	 * Move a service in a specified place
	 * @param serviceId the service to move
	 * @param srcPlaceId the previous place of this object
	 * @param destPlaceId the destination of this object
	 */
	public void moveService(String serviceId, String srcPlaceId, String destPlaceId);

	/**
	 * Get the place identifier of a core object
	 * @param objId the core object identifier 
	 * @return the identifier of the place where the core object is placed.
	 */
	public String getCoreObjectPlaceId(String objId);
	
	/**
	 * Call AppsGate to get all the places that match a specific name
	 * @param name the name to match
	 * @return the places with the name <name> as a JSONArray
	 */
	public JSONArray getPlacesByName(String name);
	
	/**
	 * Get places that have been tagged with all tags
	 * give in parameter.
	 * @param tags the tags list that places have to match
	 * @return places as a JSONArray
	 */
	public JSONArray gePlacesWithTags(JSONArray tags);
	
	/**
	 * Get places that contains the properties keys in parameters
	 * @param keys all properties that places have to be set
	 * @return places list as a JSONArray
	 */
	public JSONArray getPlacesWithProperties(JSONArray keys);
	
	/**
	 * Get places that contains the properties keys in parameters
	 * and with the corresponding values
	 * @param properties all properties that places have to be set with
	 * the corresponding value
	 * @return places list as a JSONArray
	 */
	public JSONArray getPlacesWithPropertiesValue(JSONArray properties);
	
	/** Get the root places description
	 * @return all root places as a JSONArray
	 */
	public JSONArray getRootPlaces();
	
	/**
	 * Add a tag to the tag of list of the specified place
	 * @param placeId the place where to add the tag
	 * @param tag the tag to add
	 * @return true if the tag has been added, false otherwise
	 */
	public boolean addTag(String placeId, String tag);
	
	/**
	 * Remove a tag from a place
	 * @param placeId the place from where to remove the tag
	 * @param tag the tag to remove
	 * @return true if the tag has been removed, false otherwise
	 */
	public boolean removeTag(String placeId, String tag);
	
	/**
	 * Add a property to a specified place
	 * @param placeId the place where to add the property
	 * @param key the key of the property to add
	 * @param value the value of the property to add
	 * @return true f the property has been added, false otherwise
	 */
	public boolean addProperty(String placeId, String key, String value);
	
	/**	
	 * Remove a property from a specified place
	 * @param placeId the place from where to remove the property	 
	 * @param key the key of the property that have to be removed
	 * @return true if the property is removed, false otherwise
	 */
	public boolean removeProperty(String placeId, String key);
	
	
	/***************************/
	/**  End User management   */
	/***************************/
	
	/**
	 * Get the end user list
	 * @return the user list as a JSONArray
	 */
	public JSONArray getUsers();

	/**
	 * Create a new end user
	 * @param id the user identifier
	 * @param password the user password
	 * @param lastName the user last name
	 * @param firstName the user first name
	 * @param role the user role
	 * @return true if the user is created, false otherwise
	 */
	public boolean createUser(String id, String password, String lastName, String firstName, String role);

	/**
	 * Delete an existing end user
	 * @param id the identifier of the user to be deleted
	 * @param password the corresponding password
	 * @return true if the user has been deleted, false otherwise
	 */
	public boolean deleteUser(String id, String password);

	/**
	 * Get details on a specify user
	 * @param id the identifier of the user
	 * @return user details as a JSONObject
	 */
	public JSONObject getUserDetails(String id);

	/**
	 * Get all information on a specify user
	 * @param id the identifier of the user
	 * @return user information as a JSONObject
	 */
	public JSONObject getUserFullDetails(String id);

	/**
	 * Check if the wanted identifier already existing.
	 * @param id the identifier to check
	 * @return true if the identifier is not use, false otherwise
	 */
	public boolean checkIfIdIsFree(String id);

	/**
	 * Synchronize a web service account with an end user profile
	 * @param id the end user identifier
	 * @param password the end user password
	 * @param accountDetails all service account needed to be connected
	 * @return true if the service account has been synchronized, false otherwise
	 */
	public boolean synchronizeAccount(String id, String password, JSONObject accountDetails);

	/**
	 * delete service account synchronization
	 * @param id the end user identifier
	 * @param password the end user password
	 * @param accountDetails all information needed to removed connection
	 * @return true it the synchronization has been canceled, false otherwise.
	 */
	public boolean desynchronizedAccount(String id, String password, JSONObject accountDetails);

	/**
	 * Associate a device to an end user
	 * @param id the end user identifier
	 * @param password the end user password
	 * @param deviceId the device identifier
	 * @return true if the association has been completed, false otherwise
	 */
	public boolean associateDevice(String id, String password, String deviceId);

	/**
	 * Remove end user and device association
	 * @param id the end user identifier
	 * @param password the end user password
	 * @param deviceId the device identifier
	 * @return true if the association has been deleted, false otherwise
	 */
	public boolean separateDevice(String id, String password, String deviceId);

	
	/************************************/
	/**   End User programs management  */
	/************************************/
	
	/**
	 * Deploy a new end user program in AppsGate system
	 * @param jsonProgram the JSONtree of the program
	 * @return true if the program has been deployed, false otherwise
	 */
	public boolean addProgram(JSONObject jsonProgram);
	
	/**
	 * Remove a currently deployed program.
	 * Stop it, if it is running.
	 * @param programId the identifier of the program to remove.
	 * @return true if the program has been removed, false otherwise.
	 */
	public boolean removeProgram(String programId);
	
	/**
	 * Update an existing program
	 * @param jsonProgram the JSONtree of the program
	 * @return true if the program has been updated, false otherwise
	 */
	public boolean updateProgram(JSONObject jsonProgram);
	
	/**
	 * Run a deployed end user program 
	 * @param programId the identifier of the program to run
	 * @return true if the program has been launched, false otherwise
	 */
	public boolean callProgram(String programId);
	
	/**
	 * Stop a deployed program execution
	 * @param programId identifier of the program
	 * @return true if the program has been stopped, false otherwise
	 */
	public boolean stopProgram(String programId);
	
	/**
	 * Stop the program but keep its current state
	 * @param programId the identifier of the program
	 * @return true if the program has been paused, false otherwise
	 */
	public boolean pauseProgram(String programId);
	
	/**
	 * Get the list of current deployed programs
	 * @return the programs list as a JSONArray
	 */
	public JSONArray getPrograms();
	
	/**
	 * Check if a program is active or not
	 * 
	 * @param programId the identifier of the program
	 * @return true if the program is active (STARTED), false otherwise
	 */
	public boolean isProgramActive(String programId);
	
	/************************************/
	/**    General AppsGate commands    */
	/************************************/
	
	/**
	 * Shutdown the AppsGate system
	 * (Shutdown the OSGi distribution)
	 */
	public void shutdown();
	
	/**
	 * restart the AppsGate system
	 * (Restart the system bundle from OSGi)
	 */
	public void restart();
	
}