/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.context.agregator.spec.ContextAgregatorSpec;
import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokException;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokExecutionException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jr
 */
class NodeState extends Node {

    /**
     * Logger
     */
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NodeState.class);

    /**
     * The object to be observed
     */
    private Node object;
    /**
     * The state to look for
     */
    private String stateName;

    private NodeEvent eventStart = null;
    private NodeEvent eventEnd = null;

    private ContextAgregatorSpec context;
    private boolean isOnRules;

    private String type;

    /**
     * Private constructor to allow copy
     *
     * @param p the parent node
     */
    private NodeState(Node p) {
        super(p);
    }

    /**
     * Constructor
     *
     * @param o the json description
     * @param parent the parent node
     */
    public NodeState(JSONObject o, Node parent) throws SpokException {
        super(parent);
        object = Builder.buildFromJSON(getJSONObject(o, "object"), parent);
        stateName = getJSONString(o, "stateName");
        context = getMediator().getContext();
        isOnRules = context.isOfState(object.getValue(), stateName);
        type = context.getBrickType(object.getValue());

    }

    @Override
    protected void specificStop() {
        eventEnd.stop();
        eventStart.stop();
    }

    @Override
    public JSONObject call() {
        try {
            buildEventsList();
        } catch (SpokExecutionException ex) {
            LOGGER.error("Unable to build Events list");
            return ex.getJSONDescription();
        }
        // We are in state
        if (context.isOfState(object.getValue(), stateName)) {
            listenEndStateEvent();
        } else {
            isOnRules = false;
            eventStart.addEndEventListener(this);
            eventStart.call();
        }
        return null;
    }

    @Override
    public String getExpertProgramScript() {
        return object.getExpertProgramScript() + ".isOfState(" + stateName + ")";
    }

    @Override
    protected Node copy(Node parent) {
        NodeState o = new NodeState(parent);
        o.object = object.copy(o);
        o.stateName = stateName;
        return o;
    }

    @Override
    public void endEventFired(EndEvent e) {
        Node n = (Node) e.getSource();
        if (n == eventStart) {
            LOGGER.trace("the start event of the state {} has been thrown", stateName);
            listenEndStateEvent();
        } else {
            LOGGER.trace("the end event of the state {} has been thrown", stateName);
            isOnRules = false;
            fireEndEvent(new EndEvent(this));
            setStarted(false);
        }
    }

    @Override
    public JSONObject getJSONDescription() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", "state");
            o.put("object", object.getJSONDescription());
            o.put("stateName", stateName);
        } catch (JSONException e) {
            // Do nothing since 'JSONObject.put(key,val)' would raise an exception
            // only if the key is null, which will never be the case
        }

        return o;
    }

    /**
     * Method that build the event list
     *
     * @throws SpokExecutionException
     */
    private void buildEventsList() throws SpokExecutionException {
        if (type == null || type.isEmpty()) {
            throw new SpokExecutionException("There is no type found for the device " + object.getValue());
        }
        JSONObject eventsFromState = context.getEventsFromState(type, stateName);
        if (eventsFromState == null) {
            throw new SpokExecutionException("No events are defined for the given state");
        }
        // everything is OK
        eventStart = buildFromOntology(eventsFromState.optJSONObject("startEvent"), type);
        eventEnd = buildFromOntology(eventsFromState.optJSONObject("endEvent"), type);
    }

    /**
     *
     * @param o
     * @param type
     * @return
     * @throws SpokExecutionException
     */
    private NodeEvent buildFromOntology(JSONObject o, String type) throws SpokExecutionException {
        String name;
        String value;
        if (o == null) {
            throw new SpokExecutionException("No event associated with this state");
        }
        try {
            name = o.getString("name");
            value = o.optString("value");
        } catch (JSONException e) {
            throw new SpokExecutionException("events are not correctly defined for this state");
        }
        return new NodeEvent("device", object.getValue(), name, value, this);
    }

    /**
     *
     * @return
     */
    boolean isOnRules() {
        return isOnRules;
    }

    /**
     * do the job when the end state event has been raised
     */
    private void listenEndStateEvent() {
        isOnRules = true;
        fireEndEvent(new EndEvent(this));
        eventEnd.addEndEventListener(this);
        eventEnd.call();
    }

    @Override
    public String toString() {
        return "[State " + stateName + "]";
    }

    /**
     * @return the method that set the state in the correct shape
     */
    NodeAction getSetter() throws SpokException {
        JSONObject action = context.getSetter(type, stateName);
        try {
            action.put("target", object.getJSONDescription());
        } catch (JSONException ex) {
            // Do nothing since 'JSONObject.put(key,val)' would raise an exception
            // only if the key is null, which will never be the case
        }
        return new NodeAction(action, this);
    }

    /**
     * @return the id of the object whose state is observed
     */
    String getObjectId() {
        return object.getValue();
    }

    /**
     * @return the name of the state that is observed
     */
    String getName() {
        return stateName;

    }
}
