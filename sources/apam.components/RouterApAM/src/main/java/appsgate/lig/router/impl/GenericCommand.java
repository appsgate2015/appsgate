package appsgate.lig.router.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsGate.lig.manager.communication.service.send.SendWebsocketsService;

public class GenericCommand implements Runnable {

	/**
	 * Static class member uses to log what happened in each instances
	 */
	private static Logger logger = LoggerFactory
			.getLogger(GenericCommand.class);

	private ArrayList<Object> args;
	@SuppressWarnings("rawtypes")
	private ArrayList<Class> paramType;
	private Object obj;
	private String methodName;
	private String callId;
	private int clientId;
	private SendWebsocketsService sendToClientService;

	@SuppressWarnings("rawtypes")
	public GenericCommand(ArrayList<Object> args, ArrayList<Class> paramType,
			Object obj, String methodName, String callId, int clientId,
			SendWebsocketsService sendToClientService) {

		this.args = args;
		this.paramType = paramType;
		this.obj = obj;
		this.methodName = methodName;
		this.callId = callId;
		this.clientId = clientId;
		this.sendToClientService = sendToClientService;
	}

	/**
	 * This method allow the router to invoke methods on an abstract java
	 * object.
	 * 
	 * @param obj
	 *            , the abstract object on which the method will be invoke
	 * @param args
	 *            , all arguments for the method call
	 * @param methodName
	 *            , the method to invoke
	 * @return the result of dispatching the method represented by this object
	 *         on obj with parameters args
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public Object abstractInvoke(Object obj, Object[] args, ArrayList<Class> paramTypes, String methodName)
			throws Exception {
		
		Class[] tabTypes = new Class[paramTypes.size()];
		int i = 0;
		Iterator<Class> itc = paramTypes.iterator();
		
		while(itc.hasNext()) {
			tabTypes[i] = itc.next(); 
			i++;
		}
	
		Method m = obj.getClass().getMethod(methodName, tabTypes);
		return m.invoke(obj, args);
	}

	@Override
	public void run() {
		try {
			Object ret = abstractInvoke(obj, args.toArray(), paramType, methodName);
			if (ret != null) {
				logger.debug("remote call, " + methodName + " returns "
						+ ret.toString() + " / return type: "
						+ ret.getClass().getName());
				JSONObject msg = new JSONObject();
				msg.put("value", ret.toString());
				msg.put("callId", callId);
				sendToClientService.send(clientId, msg.toString());
			}
		} catch (Exception e) {
			logger.debug("The generic method invocation failed --> ");
			e.printStackTrace();
		}
	}

}
