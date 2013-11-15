package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.EUDEInterpreterImpl;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.components.EndEventGenerator;
import appsgate.lig.eude.interpreter.langage.components.EndEventListener;
import appsgate.lig.eude.interpreter.langage.components.StartEvent;
import appsgate.lig.eude.interpreter.langage.components.StartEventGenerator;
import appsgate.lig.eude.interpreter.langage.components.StartEventListener;
import appsgate.lig.eude.interpreter.langage.components.SymbolTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for all the nodes of the interpreter
 *
 * @author Rémy Dautriche
 * @author Cédric Gérard
 *
 * @since May 22, 2013
 * @version 1.0.0
 *
 */
public abstract class Node implements Callable<Integer>, StartEventGenerator, StartEventListener, EndEventGenerator, EndEventListener {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

    /**
     * List of the listeners that listen to the StartEvent of the node
     */
    private final ArrayList<StartEventListener> startEventListeners = new ArrayList<StartEventListener>();

    /**
     * List of the listeners that listen to the EndEvent of the node
     */
    private final ArrayList<EndEventListener> endEventListeners = new ArrayList<EndEventListener>();

    protected EUDEInterpreterImpl interpreter;

    /**
     * Symbol table of the node containing the local symbols
     */
    protected SymbolTable symbolTable;

    /**
     * Node parent in the abstract tree of a program
     */
    protected Node parent;

    /**
     * Use to stop node but atomically
     */
    protected boolean stopping = false;

    /**
     * use to know when a node node is execute
     */
    protected boolean started = false;

    /**
     * Default constructor
     *
     * @param interpreter interpreter pointer for the nodes
     */
    public Node(EUDEInterpreterImpl interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * Stop the interpretation of the node. The current state is saved
     */
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startEventFired(StartEvent e) {
        LOGGER.debug("The start event has been fired: " + e.toString());
    }

    @Override
    public void endEventFired(EndEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Fire a start event to all the listeners
     *
     * @param e The start event to fire for all the listeners
     */
    protected void fireStartEvent(StartEvent e) {
        for (int i = 0; i < startEventListeners.size(); i++) {
            startEventListeners.get(i).startEventFired(e);
        }
    }

    /**
     * Fire an end event to all the listeners
     *
     * @param e The end event to fire for all the listeners
     */
    protected synchronized void fireEndEvent(EndEvent e) {
        Node n = (Node) e.getSource();
        if (n instanceof NodeProgram) {
            LOGGER.debug("NodeProgram {0} waking {1} nodes...", new Object[]{((NodeProgram) n).getName(), endEventListeners.size()});
            for (int i = 0; i < endEventListeners.size(); i++) {
                if (endEventListeners.get(i) instanceof NodeEvent) {
                    LOGGER.debug("////// ###waking a node event...");
                    ((NodeEvent) endEventListeners.get(i)).endEventFired(e);
                }
            }
        }

        for (int i = 0; i < endEventListeners.size(); i++) {
            if (endEventListeners.get(i) instanceof Node) {
                LOGGER.debug("###waking a node...");
            }
            if (endEventListeners.get(i) instanceof NodeEvent) {
                LOGGER.debug("###### Waking up a NodeEvent");
            }
            endEventListeners.get(i).endEventFired(e);
        }
    }

    /**
     * Add a new listener to the start event of the node
     *
     * @param listener Listener to add
     */
    @Override
    public void addStartEventListener(StartEventListener listener) {
        startEventListeners.add(listener);
    }

    /**
     * Remove a listener to the start event of the node
     *
     * @param listener Listener to remove
     */
    @Override
    public void removeStartEventListener(StartEventListener listener) {
        startEventListeners.remove(listener);
    }

    /**
     * Add a new listener to the end event of the node
     *
     * @param listener Listener to add
     */
    @Override
    public void addEndEventListener(EndEventListener listener) {
        endEventListeners.add(listener);
    }

    /**
     * Remove a listener to the end event of the node
     *
     * @param listener Listener to remove
     */
    @Override
    public void removeEndEventListener(EndEventListener listener) {
        endEventListeners.remove(listener);
    }

    /**
     * Getter for the local symbol table
     *
     * @return the symbol table of the node that contains the symbols defined by
     * the node
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Getter for the symbol table of the parent node
     *
     * @return the symbol table of the parent node if the node has a parent,
     * null otherwise
     */
    public SymbolTable getParentSymbolTable() {
        if (parent != null) {
            return parent.getSymbolTable();
        } else {
            return null;
        }
    }

    @Override
    public Integer call() {
        return null;
    }

    /**
     * Method that retrieve a string from a Json object
     *
     * @param jsonObj
     * @param jsonParam
     * @return the string corresponding to the jsonParam
     * @throws NodeException if there is no such parameter in the JSON Object
     */
    protected String getJSONString(JSONObject jsonObj, String jsonParam) throws NodeException {
        try {
            return jsonObj.getString(jsonParam);
        } catch (JSONException ex) {
            throw new NodeException(this.getClass().getName(), jsonParam, ex);
        }
    }

    /**
     * Method that retrieve an array from a Json object
     *
     * @param jsonObj
     * @param jsonParam
     * @return the array corresponding to the jsonParam
     * @throws NodeException if there is no such parameter in the JSON Object
     */
    protected JSONArray getJSONArray(JSONObject jsonObj, String jsonParam) throws NodeException {
        try {
            return jsonObj.getJSONArray(jsonParam);
        } catch (JSONException ex) {
            throw new NodeException(this.getClass().getName(), jsonParam, ex);
        }

    }
    /**
     * Method that retrieve an object from a Json object
     *
     * @param jsonObj
     * @param jsonParam
     * @return the object corresponding to the jsonParam
     * @throws NodeException if there is no such parameter in the JSON Object
     */
    protected JSONObject getJSONObject(JSONObject jsonObj, String jsonParam) throws NodeException {
        try {
            return jsonObj.getJSONObject(jsonParam);
        } catch (JSONException ex) {
            throw new NodeException(this.getClass().getName(), jsonParam, ex);
        }

    }

}
