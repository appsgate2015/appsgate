package appsgate.lig.smartplug.actuator_sensor.messages;

import org.json.JSONException;
import org.json.JSONObject;

import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectSpec;

/**
 * This class is an ApAM message for smart plug event notification
 * 
 * @author Cédric Gérard
 * version 1.0.0
 * @since August 13, 2013
 */
public class SmartPlugNotificationMsg implements NotificationMsg {

	/**
	 * The source sensor of this notification
	 */
	private CoreObjectSpec source;

	/**
	 * The name of the change variable
	 */
	private String varName;

	/**
	 * The value corresponding to the varName variable
	 */
	private String value;

	/**
	 * Constructor of Smart Plug ApAM message
	 * @param source the abstract object source of this message
	 * @param varName the variable that changed
	 * @param value the new variable value
	 */
	public SmartPlugNotificationMsg(CoreObjectSpec source, String varName, String value) {
		this.source = source;
		this.varName = varName;
		this.value = value;
	}

	@Override
	public CoreObjectSpec getSource() {
		return source;
	}

	@Override
	public String getNewValue() {
		return value;
	}

	@Override
	public JSONObject JSONize() throws JSONException {
		JSONObject notif = new JSONObject();
		notif.put("objectId", source.getAbstractObjectId());
		notif.put("varName", varName);
		notif.put("value", value);
		
		return notif;
	}

}
