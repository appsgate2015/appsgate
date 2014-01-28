package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.langage.exceptions.SpokNodeException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.components.StartEvent;
import appsgate.lig.eude.interpreter.langage.components.SymbolTable;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokException;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a sequence of rules to evaluate
 *
 * // <seqRules> ::= <seqAndRules> { <opThenRule> <seqAndRules> }
 *
 *
 * @author Rémy Dautriche
 * @author Cédric Gérard
 *
 * @since May 22, 2013
 * @version 1.0.0
 *
 */
public class NodeSeqRules extends Node {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeSeqRules.class.getName());

    /**
     * Contains the block of rules separated by a "THEN" operator
     */
    private List<Node> instructions;

    /**
     *
     */
    private Iterator<Node> iterator;

    private Node currentNode = null;

    /**
     * private Constructor to copy Nodes
     *
     * @param p
     */
    private NodeSeqRules(Node p) {
        super(p);
    }

    /**
     * Initialize the sequence of rules from a JSON tree
     *
     * @param seqInstructions JSON Object containing the rules
     * @param parent
     * @throws SpokNodeException
     */
    public NodeSeqRules(JSONObject seqInstructions, Node parent) throws SpokException {
        super(parent);
        JSONArray seqRulesJSON = seqInstructions.optJSONArray("rules");

        instructions = new ArrayList<Node>();

        if (seqRulesJSON == null) {
            iterator = instructions.iterator();
            LOGGER.warn("No instructions in this block");
            return;
        }

        for (int i = 0; i < seqRulesJSON.length(); i++) {
            JSONObject inst = null;
            try {
                inst = seqRulesJSON.getJSONObject(i);
            } catch (JSONException ex) {
                throw new SpokNodeException("NodeSeqRules", "item " + i, ex);
            }
            if (inst != null) {
                instructions.add(Builder.BuildNodeFromJSON(inst, this));
            }
        }
        iterator = instructions.iterator();

    }

    @Override
    public JSONObject call() {
        iterator = instructions.iterator();
        setStarted(true);
        fireStartEvent(new StartEvent(this));

        if (!instructions.isEmpty()) {
            launchNextSeqAndRules();
        } else {
            LOGGER.warn("Trying to call a seq rule on an empty sequence");
            setStarted(false);
            fireEndEvent(new EndEvent(this));
        }

        return null;
    }

    @Override
    public void endEventFired(EndEvent e) {
        if (iterator.hasNext()) {
            LOGGER.trace("###### launching the next sequence of rules...");
            try {
                launchNextSeqAndRules();
            } catch (Exception ex) {
                LOGGER.error("Exception caught: {}", ex.getMessage());
            }
        } else {
            LOGGER.debug("###### SeqThenRules ended...");
            setStarted(false);
            fireEndEvent(new EndEvent(this));
        }
    }

    /**
     * Method that launch the next sequence
     */
    private void launchNextSeqAndRules() {

        synchronized (this) {
            // get the next sequence of rules to launch
            currentNode = iterator.next();
        }

        if (!isStopping()) {
            // launch the sequence of rules
            currentNode.addEndEventListener(this);
            currentNode.call();
        }
    }

    @Override
    public void specificStop() throws SpokException {
        for (Node n : instructions) {
            n.removeEndEventListener(this);
            n.stop();
        }
        synchronized (this) {
            if (instructions.size() > 0) {
                setStopping(true);
                if (currentNode != null) {
                    currentNode.stop();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "[Node SeqRules: [" + instructions.size() + "]]";
    }

    @Override
    public JSONObject getJSONDescription() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", "instructions");
            o.put("rules", getJSONArray());
        } catch (JSONException ex) {
            // Do nothing since 'JSONObject.put(key,val)' would raise an exception
            // only if the key is null, which will never be the case
        }
        return o;
    }

    /**
     *
     * @return
     */
    private JSONArray getJSONArray() {
        JSONArray a = new JSONArray();
        int i = 0;
        for (Node n : this.instructions) {
            try {
                a.put(i, n.getJSONDescription());
                i++;
            } catch (JSONException ex) {
                // Do nothing since 'JSONObject.put(key,val)' would raise an exception
                // only if the key is null, which will never be the case
            }
        }
        return a;
    }

    @Override
    public String getExpertProgramScript() {
        if (instructions.size() == 1) {
            return instructions.get(0).getExpertProgramScript();
        }

        String ret = "[";
        for (Node s : instructions) {
            ret += s.getExpertProgramScript() + ",";
        }
        return ret.substring(0, ret.length() - 1) + "]";
    }

    @Override
    protected void collectVariables(SymbolTable s) {
        for (Node seq : instructions) {
            seq.collectVariables(s);
        }
    }

    @Override
    protected Node copy(Node parent) {
        NodeSeqRules ret = new NodeSeqRules(parent);
        ret.instructions = new ArrayList<Node>();
        for (Node n : instructions) {
            ret.instructions.add(n.copy(ret));
        }

        ret.setSymbolTable(this.getSymbolTable());
        return ret;

    }
}
