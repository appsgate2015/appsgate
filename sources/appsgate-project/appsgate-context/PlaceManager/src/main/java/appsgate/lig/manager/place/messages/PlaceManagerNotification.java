package appsgate.lig.manager.place.messages;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import appsgate.lig.ehmi.spec.messages.NotificationMsg;

/**
 * This class is an ApAM message for place notification
 * 
 * @author Cédric Gérard
 * version 1.0.0
 * @since February 26, 2013
 */
public class PlaceManagerNotification implements NotificationMsg {
	
	/**
	 * The location identifier
	 */
	private String locationId;
	
	/**
	 * The location name
	 */
	private String name;
	
	/**
	 * The specified type of this notification
	 */
	private String type;

	/**
	 * Build a new place notification object
	 * @param locationId the identifier of the location
	 * @param name the name of the location
	 * @param type the type of the notification (Add, Remove or Update)
	 */
	public PlaceManagerNotification(String locationId, String name, String type) {
		super();
		this.locationId = locationId;
		this.name = name;
		this.type = type;
	}

	@Override
	public String getSource() {
		return "";
	}
	
	@Override
	public String getVarName() {
		return name;
	}

	@Override
	public String getNewValue() {
		return "Place manager send : newLocation";
	}

	@Override
	public JSONObject JSONize()  {
		JSONObject notif = new JSONObject();
		JSONObject content = new JSONObject();
		
		try {
			content.put("id", locationId);
			content.put("name", name);
			content.put("devices", new JSONArray());
			notif.put(type, content);
			
		} catch (JSONException e) {e.printStackTrace();}

		return notif;
	}

}
