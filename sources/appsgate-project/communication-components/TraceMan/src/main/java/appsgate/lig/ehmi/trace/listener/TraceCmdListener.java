package appsgate.lig.ehmi.trace.listener;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsGate.lig.manager.client.communication.service.subscribe.CommandListener;
import appsgate.lig.ehmi.trace.TraceMan;

/**
 * This class is use to get back command from debugguer web client.
 * 
 * @author Cedric Gerard
 * @since August 25, 2014
 * @version 1.0.0
 */
public class TraceCmdListener implements CommandListener{
	
	/**
     * The logger
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(TraceCmdListener.class);
    
    /**
     * Trace manager instance
     */
    private TraceMan traceMan;
    
    /**
     * Constructor
     */
    public TraceCmdListener(TraceMan traceMan) {
    	this.traceMan = traceMan;
    }

	@Override
	public void onReceivedCommand(JSONObject obj) {
		LOGGER.debug("debugger command received: "+obj.toString());
		
		if(obj.has("name")){
			String cmd;
			try {
				cmd = obj.getString("name");
				
				if(cmd.equalsIgnoreCase("historytrace")){
					
					JSONObject args   = obj.getJSONObject("args");
					long from 		  = args.getLong("from");
					long to 	 	  = args.getLong("to");
					int res 	 	  = args.getInt("screenResolution");
					int selector 	  = args.getInt("selectorResolution");
					boolean eventLine = args.getBoolean("withEventLine");
					
					//Set the delta time aggregation interval from client info
					if(args.has("brushResolution")){
						int brushRes = args.getInt("brushResolution");
						traceMan.setDeltaT((long)Math.ceil(((to-from)*selector*brushRes)/(res*res)));
					}else{
						traceMan.setDeltaT((long)Math.ceil(((to-from)*selector)/res));
					}
					
					//set the timeline aggregation value
					traceMan.setTimeLineDelta((long)Math.ceil((to-from)/res));
					
					//Set the grouping order
					if(args.has("order")){
						traceMan.setGroupingOrder(args.getString("order"));
					}else{
						traceMan.setGroupingOrder("type");
					}
					
					//If focus is defined trace are filtered with the focus
					//equipment identifier
					if(args.has("focus") && args.has("focusType")){
						traceMan.setFocusEquipment(args.getString("focus"), args.getString("focusType"));
					}else{
						traceMan.setFocusEquipment(TraceMan.NOFOCUS, "");
					}

					traceMan.getTracesBetweenInterval(from, to, eventLine, obj);
					
				}else if(cmd.equalsIgnoreCase("livetrace")){
					long deltaT = 0;
					long refreshRate = 0;
					
					if(obj.has("args")){
						JSONObject args = obj.getJSONObject("args");
						
						if(args.has("delta")){
							deltaT = args.getLong("delta");
						}
						
						if(args.has("refreshRate")){
							deltaT = 0; // Refresh rate can only by used alone without aggregation
										// so it force deltaT interval for aggregation to 0
							refreshRate = args.getLong("refreshRate");
						} 
					}
					
					traceMan.setDeltaT(deltaT);
					traceMan.initLiveTracer(refreshRate);
					
				}else if (cmd.equalsIgnoreCase("filetrace")){
					long deltaT = 0;
					if(obj.has("args")){
						JSONObject args = obj.getJSONObject("args");
						if(args.has("delta")){
							deltaT = args.getLong("delta");
						}
					}
					traceMan.setDeltaT(deltaT);
					traceMan.initFileTracer();
					
				}else if (cmd.equalsIgnoreCase("stoplive")){
					traceMan.stopLive();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

}
