package appsgate.lig.ehmi.impl.listeners;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.manager.client.communication.service.subscribe.CommandListener;
import appsgate.lig.ehmi.exceptions.CoreDependencyException;
import appsgate.lig.ehmi.impl.EHMIProxyImpl;

/**
 * Listener for EHMI commands and events from clients.
 * 
 * @author Cédric Gérard
 * @since April 22, 2014
 * @version 1.0.0
 * 
 */
public class EHMICommandListener implements CommandListener {

	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory.getLogger(EHMICommandListener.class);
	
	/**
	 * Service that launch method call in a dedicated thread pool
	 */
	private ScheduledExecutorService executorService;

	/**
	 * The parent proxy of this listener
	 */
	private EHMIProxyImpl ehmiProxy;

	/**
	 * Constructor to initialize the listener with its parent
	 * 
	 * @param router the parent of this listener
	 */
	public EHMICommandListener(EHMIProxyImpl ehmiProxy) {
		this.ehmiProxy = ehmiProxy;
		executorService = Executors.newScheduledThreadPool(10);
	}

	/**
	 * This is the handler for command events from client.
	 * 
	 * @param obj The JSON object description for the cmd parameter
	 */
	@Override
	public void onReceivedCommand(JSONObject obj) {
		logger.debug("Client send : " + obj.toString());
		logger.debug("Application domain level message (EHMI)");
		try {
			int clientId = obj.getInt("clientId");
			String method = obj.getString("method");
			JSONArray args = obj.getJSONArray("args");
			String callId = null;
			ArrayList<Object> arguments = new ArrayList<Object>();
			@SuppressWarnings("rawtypes")
			ArrayList<Class> types = new ArrayList<Class>();
			
			try {
				loadArguments(args, arguments, types);
				if(obj.has("callId")) {
					callId = obj.getString("callId");
					logger.debug("method with return call");
				} else {
					logger.debug("method without return");
				}
				
				if(!obj.has("objectId")){
					executorService.execute(ehmiProxy.executeCommand(clientId, method, arguments, types, callId));
				}else{
					executorService.execute(ehmiProxy.executeRemoteCommand(obj.getString("objectId"), method, arguments, types, clientId, callId));
				}
			} catch (IllegalArgumentException e) {
				logger.debug("Inappropriate argument: " + e.getMessage());
			} catch(CoreDependencyException coreException) {
				String objId = obj.getString("objectId");
			}
		} catch (JSONException e1) {
			logger.error(e1.getMessage());
		}
		
	}

	/**
	 * Load argument describe in a JSONArray with their value and java type to two 
	 * ArrayList one for values and the other for types
	 * @param args the arguments JSONArray
	 * @param arguments the value ArrayList
	 * @param types the java types ArrayList
	 */
	@SuppressWarnings("rawtypes")
	public void loadArguments(JSONArray args, ArrayList<Object> arguments, ArrayList<Class> types) {
		try {
			// Get all arguments types and values
			int l = args.length();
			int cpt = 0;
			JSONObject JSONObj;
			String value, type;
		
			while (cpt < l) {
				JSONObj = args.getJSONObject(cpt);
				// This one has been modified : JSONObj.getString("value") not working properly with JSONObjects
				value = JSONObj.get("value").toString();
				type = JSONObj.getString("type");
				addArguments(type, value, arguments, types);
				cpt++;
			}
		}catch (ClassNotFoundException e) {
			logger.debug("The argument type is unknown from \"java/lang\" package: "
					+ e.getMessage());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the Class java object from the String type, instantiate it and add
	 * the generate java object to the parameters list.
	 * 
	 * @param type the parameter java type (e.g.: java.lang.String)
	 * @param value the value of the parameter
	 * @param arguments the arguments list
	 * 
	 * @throws ClassNotFoundException throw if no java Class corresponding to type parameter is  found.
	 * @throws IllegalArgumentException throw if the argument does not corresponding to the constructor type.
	 */
	@SuppressWarnings("unchecked")
	private void addArguments(String type, String value,
			ArrayList<Object> arguments, @SuppressWarnings("rawtypes")ArrayList<Class> types) throws ClassNotFoundException,
			IllegalArgumentException {

		// Type referred to a basic java type
		try {
			if (type.matches("\\p{javaUpperCase}.*")) {
				// Java wrapper for basic type
				logger.debug("Wrapper type detected");
				@SuppressWarnings("rawtypes")
				Class argClass;
				if (type.contains("JSON")) {
					argClass = Class.forName("org.json." + type);
				} else {
					argClass = Class.forName("java.lang." + type);
				}
				Object param = argClass.getConstructor(String.class)
						.newInstance(value);
				arguments.add(param);
				types.add(param.getClass());

			} else {
				// Java primitive type
				logger.debug("Full primitive type detected");
				if (type.contentEquals("int")) {
					int intParam = new Integer(value).intValue();
					arguments.add(intParam);
					types.add(int.class);
				} else if (type.contentEquals("float")) {
					float floatParam = new Float(value).floatValue();
					arguments.add(floatParam);
					types.add(float.class);
				} else if (type.contentEquals("long")) {
					long longParam = new Long(value).longValue();
					arguments.add(longParam);
					types.add(long.class);
				} else if (type.contentEquals("double")) {
					double doubleParam = new Double(value).doubleValue();
					arguments.add(doubleParam);
					types.add(double.class);
				} else if (type.contentEquals("boolean")) {
					boolean boolParam = new Boolean(value).booleanValue();
					arguments.add(boolParam);
					types.add(boolean.class);
				} else {
					throw new ClassNotFoundException("Primitive type (" + type+ ") not defined");
				}
			}

		} catch (SecurityException e) {
			logger.debug("Security violation: " + e.getMessage());
		} catch (InstantiationException e) {
			logger.debug("Instanciation error, wrong type description: "
					+ e.getMessage());
		} catch (IllegalAccessException e) {
			logger.debug("illegal access detected:  " + e.getMessage());
		} catch (InvocationTargetException e) {
			logger.debug("Probleme with the targeted object : "
					+ e.getMessage());
		} catch (NoSuchMethodException e) {
			logger.debug("No constructor with \"String\" parameter : "
					+ e.getMessage());
		}
	}

}
