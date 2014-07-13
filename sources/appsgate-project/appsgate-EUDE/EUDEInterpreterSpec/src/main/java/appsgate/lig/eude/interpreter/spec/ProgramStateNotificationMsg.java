package appsgate.lig.eude.interpreter.spec;

import org.json.JSONException;
import org.json.JSONObject;

import appsgate.lig.ehmi.spec.messages.NotificationMsg;

/**
 * ApAm Program state notification class
 *
 * @author Cédric Gérard
 *
 */
public class ProgramStateNotificationMsg implements NotificationMsg {

    /**
     * the id of the program associated to the notification message
     */
    private final String programId;
    /**
     * the var that has changed in the program
     */
    private final String varName;
    /**
     * the new value of the var
     */
    private final String value;

    /**
     * Constructor
     *
     * @param programId
     * @param varName
     * @param value
     */
    public ProgramStateNotificationMsg(String programId, String varName, String value) {
        super();
        this.programId = programId;
        this.varName = varName;
        this.value = value;
    }

    @Override
    public String getSource() {
        return "";
    }
    
    @Override
    public String getVarName() {
    	return varName;
    }

    @Override
    public String getNewValue() {
        return value;
    }

    @Override
    public JSONObject JSONize() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("objectId", programId);
            obj.put("varName", varName);
            obj.put("value", value);
        } catch (JSONException e) {
            // No exception will be raised since put value is always set
        }

        return obj;
    }

}
