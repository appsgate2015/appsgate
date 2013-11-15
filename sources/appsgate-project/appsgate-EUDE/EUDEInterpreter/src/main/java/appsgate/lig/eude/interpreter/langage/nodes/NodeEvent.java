package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.EUDEInterpreterImpl;
import org.json.JSONObject;

import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.components.StartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node representing the events
 *
 * @author Rémy Dautriche
 * @author Cédric Gérard
 *
 * @since June 25, 2013
 * @version 1.0.0
 */
public class NodeEvent extends Node {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

    /**
     * Type of the source to listen. Can be "program" or "device"
     */
    private final String sourceType;
    /**
     * ID of the source to listen
     */
    private final String sourceId;

    public String getSourceId() {
        return sourceId;
    }
    /**
     * Name of the event to listen
     */
    private final String eventName;

    public String getEventName() {
        return eventName;
    }
    /**
     * Value of the event to wait
     */
    private final String eventValue;

    public String getEventValue() {
        return eventValue;
    }

    /**
     *
     * @param interpreter
     * @param s_type
     * @param s_id
     * @param name
     * @param value
     */
    public NodeEvent(EUDEInterpreterImpl interpreter, String s_type, String s_id, String name, String value) {
        super(interpreter);
        sourceType = s_type;
        sourceId = s_id;
        eventName = name;
        eventValue = value;
  }

    /**
     *
     * @param interpreter Pointer on the interpreter
     * @param eventJSON JSON representation of the event
     * @throws appsgate.lig.eude.interpreter.langage.nodes.NodeException
     */
    public NodeEvent(EUDEInterpreterImpl interpreter, JSONObject eventJSON) throws NodeException {
        super(interpreter);

        sourceType = getJSONString(eventJSON, "sourceType");
        sourceId = getJSONString(eventJSON, "sourceId");
        eventName = getJSONString(eventJSON, "eventName");
        eventValue = getJSONString(eventJSON, "eventValue");

    }

    @Override
    public Integer call() {
        fireStartEvent(new StartEvent(this));
        started = true;
        // if the source of the event is a program
        if (sourceType.equals("program")) {
            // get the node of the program
            NodeProgram p = interpreter.getNodeProgram(sourceId);
            // if it exists
            if (p != null) {
                // listen to its start event...
                if (eventName.equals("runningState")) {
                    interpreter.addNodeListening(this);
                }
            } else { // interpreter does not know the program, then the end event is automatically fired
                started = false;
                fireEndEvent(new EndEvent(this));
            }
            // sourceType is "device"
        } else {
            interpreter.addNodeListening(this);
        }

        return null;
    }

    @Override
    public void stop() {
        if (started) {
            stopping = true;
            if (sourceType.equals("program")) {
                NodeProgram p = interpreter.getNodeProgram(sourceId);
                if (eventName.equals("start")) {
                    p.removeStartEventListener(this);
                } else if (eventName.equals("end")) {
                    p.removeEndEventListener(this);
                }
            } else {
                interpreter.removeNodeListening(this);
            }
            started = false;
            stopping = false;
        }
    }

    /**
     *
     */
    public void coreEventFired() {
        started = false;
        //interpreter.removeNodeListening(this);
        // the node is done when the relevant event has been caught
        fireEndEvent(new EndEvent(this));
    }

    @Override
    public void endEventFired(EndEvent e) {
        LOGGER.debug("endEvent fired: " + e.toString());
    }

}
