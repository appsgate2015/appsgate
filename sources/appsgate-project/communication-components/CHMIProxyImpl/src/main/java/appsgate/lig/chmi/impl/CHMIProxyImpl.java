package appsgate.lig.chmi.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsGate.lig.manager.client.communication.service.send.SendWebsocketsService;
import appsGate.lig.manager.client.communication.service.subscribe.ListenerService;
import appsgate.lig.chmi.exceptions.EHMIDependencyException;
import appsgate.lig.chmi.exceptions.ExternalComDependencyException;
import appsgate.lig.chmi.impl.listeners.RouterCommandListener;
import appsgate.lig.chmi.spec.CHMIProxySpec;
import appsgate.lig.chmi.spec.GenericCommand;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectBehavior;
import appsgate.lig.core.object.spec.CoreObjectSpec;
import appsgate.lig.core.object.spec.CoreObjectSpec.CORE_TYPE;
import appsgate.lig.ehmi.spec.EHMIProxySpec;
import fr.imag.adele.apam.Instance;

/**
 * This class is use to address with generic means all the devices and services
 * recruited through ApAM middleware.
 *
 * @author Cédric Gérard
 * @since February 14, 2013
 * @version 1.0.0
 *
 */
public class CHMIProxyImpl implements CHMIProxySpec {

    /**
     * Static class member uses to log what happened in each instances
     */
    private final static Logger logger = LoggerFactory.getLogger(CHMIProxyImpl.class);

    private RouterCommandListener commandListener;

    /**
     * Undefined sensors list, resolved by ApAM
     */
    Set<CoreObjectSpec> abstractDevice;

    /**
     * Service to be notified when clients send commands
     */
    private ListenerService addListenerService;

    /**
     * Service to communicate with clients
     */
    private SendWebsocketsService sendToClientService;

    /**
     * The main AppsGate component to call for every request
     */
    private EHMIProxySpec ehmiProxy;

    /**
     * Called by APAM when an instance of this implementation is created
     */
    public void newInst() {
        logger.debug("The CHMI proxy component has been initialized");
        commandListener = new RouterCommandListener(this);
        if (addListenerService.addCommandListener(commandListener)) {
            logger.info("Listeners services dependency resolved.");
        } else {
            logger.info("Listeners services dependency resolution failed.");
        }
    }

    /**
     * Called by APAM when an instance of this implementation is removed
     */
    public void deleteInst() {
        logger.debug("The CHMI proxy component has been stopped");
    }

    /**
     * Called by ApAM when an undefined instance is added to the set.
     *
     * @param inst , the new undefined instance
     */
    public void addAbstractObject(Instance inst) {
        logger.debug("New abstract device added: " + inst.getName());
        try {
            //notify that a new device, service or simulated instance appeared
            CoreObjectSpec newObj = (CoreObjectSpec) inst.getServiceObject();
            String newMsg = "";
            if (newObj.getCoreType().equals(CORE_TYPE.DEVICE)) {
            	newMsg = "newDevice";
            	try{
            		ehmiProxy.addGrammar(newObj.getUserType(), newObj.getGrammarDescription());
            	}catch(EHMIDependencyException comException) {
            		logger.warn("Resolution failled for ehmi dependency, no grammar will be added.");
            	}
            } else if (newObj.getCoreType().equals(CORE_TYPE.SERVICE)) {
            	newMsg = "newService";
            	try{
            		ehmiProxy.addGrammar(newObj.getUserType(), newObj.getGrammarDescription());
            	}catch(EHMIDependencyException comException) {
            		logger.warn("Resolution failled for send to client service dependency, no message will be sent.");
            	}
            } else if (newObj.getCoreType().equals(CORE_TYPE.SIMULATED_DEVICE)) {
            	newMsg = "newSimulatedDevice";
                //TODO manage the simulated device
                logger.debug("Simulated device core type not supported yet for EHMI");
            } else if (newObj.getCoreType().equals(CORE_TYPE.SIMULATED_SERVICE)) {
            	newMsg = "newSimulatedService";
                //TODO manage the simulated service
                logger.debug("Simulated service core type not supported yet for EHMI");
            }
            
            try{
            	sendToClientService.send(newMsg, getObjectDescription(newObj, ""));
        	}catch(ExternalComDependencyException comException) {
        		logger.warn("Resolution failled for send to client service dependency, no message will be sent.");
        	}
            
        } catch (Exception ex) {
            logger.error("If getCoreType method error trace appeare below it is because the service or the device doesn't implement all methode in"
                    + "the CoreObjectSpec interface but this error doesn't impact the EHMI.");
            ex.printStackTrace();
        }
    }

    /**
     * Called by ApAM when an undefined instance is removed from the set
     *
     * @param inst , the removed undefined instance
     */
    public void removedAbstractObject(Instance inst) {
        logger.debug("Abstract device removed: " + inst.getName());
        String deviceId = inst.getProperty("deviceId");
        JSONObject obj = new JSONObject();
        try {
            obj.put("objectId", deviceId);
        } catch (JSONException e) {
            // No exception is thrown
        }
        CoreObjectSpec rmObj = (CoreObjectSpec) inst.getServiceObject();

        synchronized(this){
        	String newMsg ="";
        	if (rmObj.getCoreType().equals(CORE_TYPE.DEVICE)) {
        		newMsg ="removeDevice";
        	} else if (rmObj.getCoreType().equals(CORE_TYPE.SERVICE)) {
        		newMsg ="removeService";
        	} else if (rmObj.getCoreType().equals(CORE_TYPE.SIMULATED_DEVICE)) {
        		newMsg ="removeSimulatedDevice";
        		//TODO manage the simulated device
        	} else if (rmObj.getCoreType().equals(CORE_TYPE.SIMULATED_SERVICE)) {
        		newMsg ="removeSimulatedService";
        		//TODO manage the simulated service
        	}
        	
            try{
            	sendToClientService.send(newMsg, obj);
        	}catch(ExternalComDependencyException comException) {
        		logger.warn("Resolution failled for send to client service dependency, no message will be sent.");
        	}
        	
        }
    }

    /**
     * Get the AbstractObjectSpec reference corresponding to the id objectID
     *
     * @param objectID the AbstractObjectSpec identifier
     * @return an AbstractObjectSpec object that have objectID as identifier
     */
    public Object getObjectRefFromID(String objectID) {
        Iterator<CoreObjectSpec> it = abstractDevice.iterator();
        CoreObjectSpec tempAbstarctObjet = null;
        String id;
        boolean notFound = true;

        while (it.hasNext() && notFound) {
            tempAbstarctObjet = it.next();
            id = tempAbstarctObjet.getAbstractObjectId();
            if (objectID.equalsIgnoreCase(id)) {
                notFound = false;
            }
        }

        if (!notFound) {
            return tempAbstarctObjet;
        } else {
            return null;
        }
    }

    /**
     * Get a command description, resolve the target reference and make the
     * call.
     *
     * @param clientId client identifier
     * @param objectId abstract object identifier
     * @param methodName method to call on objectId
     * @param args arguments list form method methodName
     * @param paramType argument type list
     * @param callId the remote call identifier
     */
    @SuppressWarnings("rawtypes")
    public Runnable executeCommand(int clientId, String objectId, String methodName, ArrayList<Object> args, ArrayList<Class> paramType, String callId) {
        Object obj;
        if (objectId.contentEquals("main")) {
            logger.info("retreive AppsGate reference: " + ehmiProxy.toString());
            obj = ehmiProxy;
        } else {
            obj = getObjectRefFromID(objectId);
        }
        return new GenericCommand(args, paramType, obj, objectId, methodName, callId, clientId, sendToClientService);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public GenericCommand executeCommand(String objectId, String methodName, ArrayList<Object> args, ArrayList<Class> paramType) {
        Object obj;
        if (objectId.contentEquals("main")) {
            logger.info("retreive AppsGate reference: " + ehmiProxy.toString());
            obj = ehmiProxy;
        } else {
            obj = getObjectRefFromID(objectId);
        }
        return new GenericCommand(args, paramType, obj, methodName);
    }

    @Override
    public GenericCommand executeCommand(String objectId, String methodName, JSONArray args) {
        ArrayList<Object> arguments = new ArrayList<Object>();
        @SuppressWarnings("rawtypes")
        ArrayList<Class> argumentsType = new ArrayList<Class>();

        commandListener.loadArguments(args, arguments, argumentsType);

        return executeCommand(objectId, methodName, arguments, argumentsType);
    }

    /**
     * Called by ApAM when Notification message comes and forward it to client
     * part by calling the sendService
     *
     * @param notif the notification message from ApAM
     */
    public void gotNotification(NotificationMsg notif) {
        logger.debug("Notification message received, " + notif.JSONize());
        
        try{
        	sendToClientService.send(notif.JSONize().toString());
    	}catch(ExternalComDependencyException comException) {
    		logger.warn("Resolution failled for send to client service dependency, no message will be sent.");
    	}
    }

    /**
     * Send all the devices description to one client
     */
    @Override
    public JSONArray getDevices() {
        Iterator<CoreObjectSpec> devices = abstractDevice.iterator();

        if (devices != null) {
            JSONArray jsonDeviceList = new JSONArray();

            while (devices.hasNext()) {
                CoreObjectSpec adev = devices.next();
                jsonDeviceList.put(getObjectDescription(adev, ""));
            }

            return jsonDeviceList;

        } else {
            logger.debug("No smart object detected.");
            return new JSONArray();
        }
    }

    @Override
    public JSONObject getDevice(String objectId) {

        Object obj = getObjectRefFromID(objectId);

        if (obj != null) {
            CoreObjectSpec objSpec = (CoreObjectSpec) obj;
            return getObjectDescription(objSpec, "");
        }

        return new JSONObject();
    }

    @Override
    public JSONArray getDevices(String type) {
        Iterator<CoreObjectSpec> devices = abstractDevice.iterator();

        if (devices != null) {
            JSONArray jsonDeviceList = new JSONArray();

            while (devices.hasNext()) {
                CoreObjectSpec adev = devices.next();
                if (type.contentEquals(adev.getUserType())) {
                    jsonDeviceList.put(getObjectDescription(adev, ""));
                }
            }

            return jsonDeviceList;

        } else {
            logger.debug("No smart object detected.");
            return new JSONArray();
        }
    }

    /**
     * This method get the auto description of an object and add the contextual
     * information associate to this object for a specified user
     *
     * @param obj the object from which to get the description
     * @param user the user from who to get the context
     * @return the complete contextual description of an object
     */
    private JSONObject getObjectDescription(CoreObjectSpec obj, String user) {
        JSONObject JSONDescription = null;
        try {
            // Get object auto description
            JSONDescription = obj.getDescription();
            //Add context description for this abject
			JSONDescription.put("name", ehmiProxy.getUserObjectName(obj.getAbstractObjectId(), user));
			JSONDescription.put("placeId", ehmiProxy.getCoreObjectPlaceId(obj.getAbstractObjectId()));

        } catch (JSONException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error("ApAM error");
            logger.error(e.getMessage());
        }
        return JSONDescription;
    }

}
