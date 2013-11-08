package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.EUDEInterpreterImpl;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONException;
import org.json.JSONObject;

import appsgate.lig.eude.interpreter.impl.ProgramStateNotificationMsg;
import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.components.StartEvent;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node program for the interpreter. Contains the metadatas of the program, the
 * parameters, the variables and the rules
 *
 * @author Rémy Dautriche
 * @author Cédric Gérard
 *
 * @since May 22, 2013
 * @version 1.0.0
 *
 */
public class NodeProgram extends Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeProgram.class);
    /**
     * Pool to execute the children. Possibly a single thread
     */
    protected ExecutorService pool;

    /**
     * Program's id set by the EUDE editor
     */
    private String id;

    public String getId() {
        return id;
    }

    /**
     * Program's name given by the user
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * User's name who wrote the program
     */
    private String author;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Target user
     */
    private String target;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Daemon attribute
     */
    private String daemon;

    public String getDeamon() {
        return daemon;
    }

    public void setDeamon(String deamon) {
        this.daemon = deamon;
    }

    public boolean isDeamon() {
        return daemon.contentEquals("true");
    }

    /**
     * Use for simplify user interface reverse compute
     */
    private String userInputSource;

    public String getUserInputSource() {
        return userInputSource;
    }

    /**
     * Sequence of rules to interpret
     */
    private NodeSeqRules seqRules;

    /**
     * JSON representation of the program
     */
    private JSONObject programJSON;

    public JSONObject getProgramJSON() {
        return programJSON;
    }

    /**
     * The current running state of this program - DEPLOYED - STARTED - STOPPED
     * - PAUSED - FAILED
     */
    private RUNNING_STATE runningState = RUNNING_STATE.DEPLOYED;

    /**
     * Attribute use to detect error like infinite loop and kill them
     */
    private int state = 0;

    public RUNNING_STATE getRunningState() {
        return runningState;
    }

    public void setRunningState(RUNNING_STATE runningState) {
        try {
            programJSON.put("runningState", runningState.toString());
            this.runningState = runningState;
            interpreter.notifyChanges(new ProgramStateNotificationMsg(id, "runningState", this.runningState.toString()));

            if (runningState.equals(RUNNING_STATE.STARTED)) {
                state++;
            } else if (runningState.equals(RUNNING_STATE.STOPPED)) {
                state--;
            }

            //This program seem to be in an infinite loop
            if (state > 1) {
                //stop it
                stop();
                //notify client that a program has been killed for security
                interpreter.notifyChanges(new ProgramStateNotificationMsg(id, "ERROR", "INFINITE_LOOP_KILLED"));
                //change the state of the program to failed start
                setRunningState(RUNNING_STATE.FAILED);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Future Java object to manage the program thread
     */
    private Future<Integer> seqRulesThread;

    /**
     * Default constructor
     *
     * @constructor
     * @param interpreter the interpreter that execute this program
     */
    public NodeProgram(EUDEInterpreterImpl interpreter) {
        super(interpreter);
        // initialize the thread pool
        pool = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize the program from a JSON object
     *
     * @param interpreter
     * @param programJSON Abstract tree of the program in JSON
     *
     * @constructor
     * @throws JSONException Thrown when the JSON file contains an error
     */
    public NodeProgram(EUDEInterpreterImpl interpreter, JSONObject programJSON)
            throws JSONException {
        this(interpreter);

        this.programJSON = programJSON;

        // initialize the program with the JSON
        id = programJSON.getString("id");
        runningState = RUNNING_STATE.valueOf(programJSON.getString("runningState"));
        userInputSource = programJSON.getString("userInputSource");

        JSONObject source = programJSON.getJSONObject("source");
        name = source.getString("programName");
        author = source.getString("author");
        target = source.getString("target");
        daemon = source.getString("daemon");
        seqRules = new NodeSeqRules(interpreter,
                source.getJSONArray("seqRules"));
    }

    /**
     * Update the current program source code Program need to be stopped.
     *
     * @param jsonProgram the new source code
     *
     * @return true if the source code has been updated, false otherwise
     * @throws org.json.JSONException
     */
    public boolean update(JSONObject jsonProgram) throws JSONException {

        this.programJSON = jsonProgram;
        userInputSource = programJSON.getString("userInputSource");

        JSONObject source = jsonProgram.getJSONObject("source");
        name = source.getString("programName");
        author = source.getString("author");
        target = source.getString("target");
        daemon = source.getString("daemon");
        seqRules = new NodeSeqRules(interpreter, source.getJSONArray("seqRules"));

        return true;

    }

    public boolean pause() {
        // TODO Auto-generated method stub

        return false;
    }

    /**
     * Return a JSON object with all the necessary information for the user
     *
     * @return
     */
    public JSONObject getInformation() {
        return programJSON;
    }

    /**
     * Launch the interpretation of the rules
     *
     * @return integer
     */
    @Override
    public Integer call() {

        if (runningState != RUNNING_STATE.PAUSED) {
            fireStartEvent(new StartEvent(this));

            seqRules.addStartEventListener(this);
            seqRules.addEndEventListener(this);

            seqRulesThread = pool.submit(seqRules);

            if (seqRulesThread != null) {
                return 1;
            } else {
                setRunningState(RUNNING_STATE.FAILED);
            }
        } else {
            // TODO restart from previous state
            // synchronized(pauseMutex) {
            // pauseMutex.notify();
            // return 1;
            // }
        }
        return -1;
    }

    /**
     * Restart daemon program after their previous termination
     */
    private int daemonCall() {
        state--;
        seqRules.addStartEventListener(this);
        seqRulesThread = pool.submit(seqRules);

        if (seqRulesThread != null) {
            return 1;
        } else {
            setRunningState(RUNNING_STATE.FAILED);
            return -1;
        }
    }

    @Override
    public void stop() {
        if (runningState == RUNNING_STATE.STARTED) {
            boolean terminate;
            seqRules.stop();
            if (!seqRulesThread.isDone()) {
                LOGGER.error("thread did not terminate");
                LOGGER.info("Try to kill the program thread");
                terminate = seqRulesThread.cancel(true);
            } else {
                terminate = true;
            }

            if (terminate) {
                seqRules.removeEndEventListener(this);
                setRunningState(RUNNING_STATE.STOPPED);
                fireEndEvent(new EndEvent(this));
                // pool = Executors.newSingleThreadExecutor();
            }
            // try {
            // boolean terminate = false;
            // pool.shutdownNow();
            // if( pool.awaitTermination(5, TimeUnit.SECONDS)) {
            // LoggerFactory.getLogger(NodeProgram.class.getName()).debug("Program thread stopped");
            // terminate = true;
            // //} else {
            // if(!seqRulesThread.isCancelled()) {
            // LoggerFactory.getLogger(NodeProgram.class.getName()).error("thread did not terminate");
            // LoggerFactory.getLogger(NodeProgram.class.getName()).info("Try to kill the program thread");
            // terminate = seqRulesThread.cancel(true);
            // }
            // }
            //
            // if(terminate) {
            // seqRules.removeEndEventListener(this);
            // setRunningState(RUNNING_STATE.STOPPED);
            // fireEndEvent(new EndEvent(this));
            // pool = Executors.newSingleThreadExecutor();
            // }
            //
            // } catch (InterruptedException e) {
            // e.printStackTrace();
            // }
        }
    }

    /**
     * Set the current running state to deployed
     */
    public void setDeployed() {
        setRunningState(RUNNING_STATE.DEPLOYED);
    }

    @Override
    public void startEventFired(StartEvent e) {
        setRunningState(RUNNING_STATE.STARTED);
        seqRules.removeStartEventListener(this);
    }

    @Override
    public void endEventFired(EndEvent e) {
        if (isDeamon()) {
            if (daemonCall() == -1) {
                seqRules.removeEndEventListener(this);
                fireEndEvent(new EndEvent(this));
            }
        } else {
            setRunningState(RUNNING_STATE.STOPPED);
            seqRules.removeEndEventListener(this);
            fireEndEvent(new EndEvent(this));
        }
    }

    /**
     * Program running state static enumeration
     *
     * @author Cédric Gérard
     * @since September 13, 2013
     */
    public static enum RUNNING_STATE {

        DEPLOYED("DEPLOYED"), STARTED("STARTED"), FAILED("FAILED"), STOPPED(
                "STOPPED"), PAUSED("PAUSED");

        private String name = "";

        RUNNING_STATE(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
