/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokExecutionException;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokNodeException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jr
 */
public class NodeValue extends Node {

    // Logger
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NodeVariableDefinition.class);

    private JSONObject callVariable() {
        NodeVariableDefinition variableByName = this.getVariableByName(value);
        if (variableByName == null) {
            SpokExecutionException e = new SpokExecutionException("Variable " + value + "not found");
            return e.getJSONDescription();
        }
        variableByName.addEndEventListener(this);
        return variableByName.call();
    }

    public enum TYPE {

        DEVICE, LIST, PROGRAMCALL, VARIABLE, STRING, BOOLEAN, NUMBER;
    }

    /**
     * The type of the value
     * (device/list/programCall/variable/string/boolean/number)
     */
    private TYPE type;
    /**
     *
     */
    private String value;

    private JSONArray list;

    /**
     * private constructor to allow copy
     *
     * @param p
     */
    private NodeValue(Node p) {
        super(p);
    }

    /**
     *
     * @param o
     * @param parent
     * @throws SpokNodeException
     */
    public NodeValue(JSONObject o, Node parent) throws SpokNodeException {
        super(parent);
        type = TYPE.valueOf(getJSONString(o, "type").toUpperCase());
        switch (type) {
            case DEVICE:
            case PROGRAMCALL:
            case VARIABLE:
                value = this.getJSONString(o, "id");
                break;
            case LIST:
                list = this.getJSONArray(o, "value");
                value = list.toString();
                break;
            default:
                value = this.getJSONString(o, "value");
                break;
        }
    }

    /**
     *
     * @param t
     * @param v
     * @param parent
     */
    public NodeValue(String t, String v, Node parent) {
        super(parent);
        this.type = TYPE.valueOf(t.toUpperCase());
        this.value = v;
    }

    @Override
    protected void specificStop() {
    }

    @Override
    public JSONObject call() {
        if (type == TYPE.VARIABLE) {
            return callVariable();
        }
        setStarted(true);
        fireEndEvent(new EndEvent(this));
        return null;
    }

    @Override
    public String getExpertProgramScript() {
        switch (type) {
            case STRING:
                return '"' + value + '"';
            case DEVICE:
                return "/" + value + "/";
            case PROGRAMCALL:
                return "|" + value + "|";
            default:
                return value;
        }
    }

    @Override
    protected Node copy(Node parent) {
        NodeValue ret = new NodeValue(parent);
        ret.type = this.type;
        ret.value = this.value;
        try {
            if (list != null) {
                ret.list = new JSONArray(this.value);
            }
        } catch (JSONException ex) {
        }
        return ret;
    }

    @Override
    public void endEventFired(EndEvent e) {
        fireEndEvent(new EndEvent(this));
    }

    @Override
    public JSONObject getJSONDescription() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", getType());
            switch (type) {
                case DEVICE:
                case PROGRAMCALL:
                case VARIABLE:
                    o.put("id", this.value);
                    break;
                case LIST:
                    o.put("value", this.list);
                    break;
                default:
                    o.put("value", this.value);
                    break;
            }
        } catch (JSONException ex) {
            // Do nothing since 'JSONObject.put(key,val)' would raise an exception
            // only if the key is null, which will never be the case
        }
        return o;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Method to get the variables of a list, if the variable is not a list, it
     * returns null
     *
     * @return a list of Variable or null
     */
    @Override
    public List<NodeValue> getElements() {
        try {
            ArrayList<NodeValue> a = new ArrayList<NodeValue>();
            for (int i = 0; i < list.length(); i++) {
                a.add(new NodeValue(list.getJSONObject(i), this));
            }
            return a;

        } catch (JSONException ex) {
            LOGGER.error("list without a list");
            return null;
        } catch (SpokNodeException ex) {
            LOGGER.error("The variable was not well formed");
            return null;
        }
    }

    /**
     * @return the value of the variable
     */
    public String getVariableValue() {
        if (type == TYPE.VARIABLE) {
            NodeVariableDefinition var = getVariableByName(value);
            return var.getValue();
        }
        return null;
    }

    @Override
    public String getType() {
        return type.toString().toLowerCase();
    }

    public TYPE getValueType() {
        return type;
    }

    @Override
    public Node getResult() {
        return this;
    }

    @Override
    public String toString() {
        return "[NodeValue type: " + getType() + ", value: " + getValue() + "]";
    }

}
