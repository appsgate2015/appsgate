/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.ehmi.spec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jr
 */
public class GrammarDescription {

    private final JSONObject json;

    private final HashMap<String, ArrayList<String>> commandsState = new HashMap<String, ArrayList<String>>();

    /**
     * public constructor
     *
     * @param o
     */
    public GrammarDescription(JSONObject o) {
        this.json = o;
        if (o!= null && o.has("commands")) {
            try {
                JSONArray commands = o.getJSONArray("commands");

                for (int i = 0; i < commands.length(); i++) {
                    ArrayList<String> props = new ArrayList<String>();
                    JSONObject commandObj = commands.getJSONObject(i);
                    JSONArray jsonProps = commandObj.getJSONArray("properties");
                    for (int j = 0; j < jsonProps.length(); j++) {
                        props.add(jsonProps.getString(j));
                    }
                    commandsState.put(commandObj.getString("name"), props);
                }
            } catch (JSONException ex) {
            }
        } 
    }

    /**
     *
     * @return
     */
    public String getType() {
        try {
            if (this.json.has("typename")) {
                return this.json.getString("typename");
            }
        } catch (JSONException ex) {
        }
        return "unknown";
    }

    /**
     * @return JSON object for compatibility
     */
    public JSONObject getJSON() {
        return this.json;
    }

    /**
     * @param command the string describing the command
     * @return a set of string representing the state that can be modified
     */
    public ArrayList<String> getPropertiesModifiedByCommand(String command) {
        return this.commandsState.get(command);
    }

    public JSONObject getStateDescription(String stateName) {
        try {
            JSONArray grammarStates = this.json.getJSONArray("states");
            for (int i = 0; i < grammarStates.length(); i++) {
                if (grammarStates.getJSONObject(i).getString("name").equalsIgnoreCase(stateName)) {
                    return grammarStates.getJSONObject(i);
                }
            }
        } catch (JSONException ex) {
        }
        return null;
    }

}