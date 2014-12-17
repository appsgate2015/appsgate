package appsgate.lig.eude.interpreter.langage.components;

import appsgate.lig.eude.interpreter.langage.exceptions.SpokNodeException;
import appsgate.lig.eude.interpreter.langage.nodes.NodeSelect;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jr
 */
public class ErrorMessagesFactory {

    /**
     * @return a JSONObject corresponding to empty program error message
     */
    public static JSONObject getEmptyProgramMessage() {
        JSONObject error = new JSONObject();
        try {
            error.put("msg", "programs.error.empty");
        } catch (JSONException ex) {
            // never happens
        }
        return error;
    }

    /**
     * @param nodeException the node exception that throw the invalid state for the program
     * @return a JSONObject corresponding to the NodeException
     */
    public static JSONObject getMessageFromSpokNodeException(SpokNodeException nodeException) {
        JSONObject error = new JSONObject();
        try {
            error.put("msg", "programs.error.node");
            error.put("nodeid", nodeException.getNodeId());
        } catch (JSONException ex) {
            // never happens
        }
        return error;
        
    }

    /**
     * 
     * @param deviceId the id of the device
     * @return a JSONObject corresponding to a missing device
     */
    public static JSONObject getMessageFromMissingDevice(String deviceId) {
        JSONObject error = new JSONObject();
        try {
            error.put("msg", "programs.error.device");
            error.put("device", deviceId);
        } catch (JSONException ex) {
            // never happens
        }
        return error;
        
    } 

    /**
     * @param programId the id of the missing program
     * @return a JSONObject corresponding to the missing program message
     */
    static JSONObject getMessageFromMissingProgram(String programId) {
        JSONObject error = new JSONObject();
        try {
            error.put("msg", "programs.error.missingProgram");
            error.put("pid", programId);
        } catch (JSONException ex) {
            // never happens
        }
        return error;
    }

    /**
     * @param programId the id of the missing program
     * @return a JSONObject corresponding to the invalid program message
     */
    static JSONObject getMessageFromInvalidProgram(String programId) {
        JSONObject error = new JSONObject();
        try {
            error.put("msg", "programs.error.invalidProgram");
            error.put("pid", programId);
        } catch (JSONException ex) {
            // never happens
        }
        return error;
    }

    static JSONObject getMessageFromEmptySelect(NodeSelect s) {
        JSONObject error = new JSONObject();
        try {
            error.put("msg", "programs.error.emptySelec");
           // error.put("pid", programId);
        } catch (JSONException ex) {
            // never happens
        }
        return error;
    
    }
}
