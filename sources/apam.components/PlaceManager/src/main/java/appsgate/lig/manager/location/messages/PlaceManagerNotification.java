package appsgate.lig.manager.location.messages;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import appsgate.lig.logical.object.messages.NotificationMsg;
import appsgate.lig.logical.object.spec.AbstractObjectSpec;

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
	private int type;

	/**
	 * Build a new place notification object
	 * @param locationId the identifier of the location
	 * @param name the name of the location
	 * @param type the type of the notification (Add, Remove or Update)
	 */
	public PlaceManagerNotification(String locationId, String name, int type) {
		super();
		this.locationId = locationId;
		this.name = name;
		this.type = type;
	}

	@Override
	public AbstractObjectSpec getSource() {
		return null;
	}

	@Override
	public String getNewValue() {
		return "Place manager send : newLocation";
	}

	@Override
	public JSONObject JSONize() throws JSONException {
		JSONObject notif = new JSONObject();
		JSONObject content = new JSONObject();
		
		content.put("id", locationId);
		content.put("name", name);
		content.put("devices", new JSONArray());
		
		if(type == 0) {
			notif.put("newLocation", content);
		} else if(type == 1) {
			notif.put("updateLocation", content);
		}
		
		return notif;
	}

}
