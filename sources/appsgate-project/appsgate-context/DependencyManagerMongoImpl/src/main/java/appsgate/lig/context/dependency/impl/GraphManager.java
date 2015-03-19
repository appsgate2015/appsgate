package appsgate.lig.context.dependency.impl;

import appsgate.lig.context.dependency.graph.Graph;
import appsgate.lig.context.dependency.graph.ProgramGraph;
import appsgate.lig.ehmi.spec.EHMIProxySpec;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jr
 */
public class GraphManager {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphManager.class);

    /**
     *
     */
    private Graph graph;

    private final DependencyManagerImpl dependency;

    /**
     * @param interpreter
     */
    public GraphManager(DependencyManagerImpl i) {
        this.dependency = i;
    }


    /**
     * build the graph
     */
    public Graph buildGraph() {
        graph = new Graph();

        /* BUILD NODES FROM DEVICES */
        // Retrieving devices id
        JSONArray devices = getContext().getDevices();
        for (int i = 0; i < devices.length(); i++) {
            graph.addDevice(devices.optJSONObject(i));
        }

        /* BUILD NODES FROM PROGRAMS */
        for (String pid : dependency.getListProgramIds()) {
            ProgramGraph p = getProgramNode(pid);
            if (p != null) {
                graph.addProgram(pid, p.getProgramName(), p.getReferences(), p.getStateName());
            }
            // Link to the scheduler
            if (isPlanificationLink(pid)) {
                graph.addSchedulerEntity(pid);
            }
        }


        /* BUILD GHOSTS NODES */
        graph.buildGhosts();
        /* BUILD PLACE NODES */
        JSONArray places = getContext().getPlaces();
        for (int i = 0; i < places.length(); i++) {
            graph.addPlace(places.optJSONObject(i));

        }
        graph.buildTypes();
        return graph;
    }

    /**
     * Method to build the planification link of a program
     *
     * @param pid : id of the program we want to build planification links
     */
    private boolean isPlanificationLink(String pid) {

        // Planification of the checkPrograms to avoid stucking if no scheduling service
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Object> task = new Callable<Object>() {
            @Override
            public Object call() {
                return getContext().checkProgramsScheduled();
            }
        };
        Future<Object> future = executor.submit(task);
        try {
            JSONArray programsScheduled = (JSONArray) future.get(2, TimeUnit.SECONDS);
            // Links program - scheduler
            return (programsScheduled != null && programsScheduled.toString().contains(pid));
        } catch (TimeoutException ex) {
            LOGGER.error("Time Out trying to reach scheduling service, aborting)");
            // handle the timeout
        } catch (InterruptedException e) {
            // handle the interrupts
        } catch (ExecutionException e) {
            // handle other exceptions
        } finally {
            future.cancel(true); // may or may not desire this
        }

        return false;
    }

    /**
     * Method to update the nodes graph with the latest values
     */
    public Graph updateGraph() {
        try {
            // Place names
            for (int j = 0; j < getContext().getPlaces().length(); j++) {
                JSONObject place = getContext().getPlaces().getJSONObject(j);
                graph.setPlaceName(place.optString("id"), place.optString("name"));
            }
            // Devices
            for (int j = 0; j < getContext().getDevices().length(); j++) {
                JSONObject currentDevice = getContext().getDevices().getJSONObject(j);
                switch (Integer.parseInt(currentDevice.getString("type"))) {
                    case 3: // Contact
                        graph.setDevice(currentDevice.optString("id"), currentDevice.optString(""), currentDevice.getString("contact"));
                        break;
                    case 4: // CardSwitch
                        graph.setDevice(currentDevice.optString("id"), currentDevice.optString(""), currentDevice.getString("inserted"));
                        break;
                    case 6: // Plug
                        graph.setDevice(currentDevice.optString("id"), currentDevice.optString(""), currentDevice.getString("plugState"));
                        break;
                    case 7: // Lamp
                        graph.setDevice(currentDevice.optString("id"), currentDevice.optString(""), currentDevice.getString("state"));
                        break;
                    default:
                        break;
                }
            }
            // Programs
            for (String s : dependency.getListProgramIds()) {
                ProgramGraph currentProgram = getProgramNode(s);
                graph.setProgramState(s, currentProgram.getStateName());

            }
        } catch (JSONException ex) {
        }
        return graph;
    }

    /**
     * @return the EHMI Proxy
     */
    private EHMIProxySpec getContext() {
        return this.dependency.getContext();
    }

    /**
     *
     * @param pid
     * @return the program id corresponding to the pid
     */
    private ProgramGraph getProgramNode(String pid) {
        return dependency.getNodeProgram(pid);
    }

    /**
     *
     * @param programId
     * @return the graph once updated
     */
    public Graph updateProgramStatus(String programId) {
        updateGraph();
        return graph;
    }

    /**
     *
     * @param srcId
     * @param varName
     * @param value
     * @return the graph once updated
     */
    public Graph updateDeviceStatus(String srcId, String varName, String value) {
        updateGraph();
        return graph;
    }

}