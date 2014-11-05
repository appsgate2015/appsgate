package appsgate.lig.ehmi.impl.listeners;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.chmi.spec.listeners.CoreEventsListener;
import appsgate.lig.ehmi.impl.EHMIProxyImpl;
import appsgate.lig.ehmi.impl.Entry;
import appsgate.lig.ehmi.spec.listeners.CoreListener;
import appsgate.lig.ehmi.spec.trace.TraceManSpec;

public class ObjectEventListener implements CoreEventsListener {

    /**
     *
     * static class logger member
     */
    private final static Logger logger = LoggerFactory.getLogger(ObjectEventListener.class);

    private String sourceId = "";
    private String varName = "";
    private String value = "";

    private EHMIProxyImpl EHMIProxy;
    private TraceManSpec traceManager;

    public ObjectEventListener(EHMIProxyImpl eHMIProxy) {
        super();
        EHMIProxy = eHMIProxy;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String varName() {
        return varName;
    }

    @Override
    public String getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notifyEvent(String srcId, String varName, String value) {
        logger.debug("Event notification received for:  {" + srcId + ", " + varName + ", " + value + "}");
        //trace the last event received
        if(traceManager != null && notFiltered(srcId, varName, value)) {
        	traceManager.coreEventNotify(EHMIProxy.getCurrentTimeInMillis(), srcId, varName, value);
        }

        this.sourceId = srcId;
        this.varName = varName;
        this.value = value;

        Entry eventKey = new Entry(srcId, varName, value);

        // Copy the listener just to avoid concurrent exception with program
        // when daemon try to add listener again when they restart
        ArrayList<Entry> keys = EHMIProxy.getCoreEventListenerCopy();

        for (Entry key : keys) {
            if (eventKey.match(key)) {
                logger.debug("Event is followed, retreiving listeners...");

                ArrayList<CoreListener> coreListenerList;
                synchronized (this) {
                    // The clone method is used because is necessary if in the notifyEvent
                    // override method someone call the deleteListener method that made deadlock
                    coreListenerList = (ArrayList<CoreListener>) EHMIProxy.getEventsListeners().get(key).clone();
                }
                for (CoreListener listener : coreListenerList) {
                    logger.debug("Notify listener.");
                    listener.notifyEvent();
                }
            }
        }

    }

    /**
     * Filter trace that not need to be trace in EHMI point view
     * @param srcId the equipement identifier
     * @param varName the vriable name thaht change
     * @param value the new value to the variable
     * @return true if the trace can be trace, false otherwise
     */
    private boolean notFiltered(String srcId, String varName, String value) {
    	//Filter on those conditions
    	if(varName.contentEquals("x")  	   ||
    	   varName.contentEquals("y")  	   ||
    	   varName.contentEquals("ct")	   ||
    	   varName.contentEquals("speed")  ||
    	   varName.contentEquals("mode")
    	 )return false;
    	
    	//Trace no need to be filtered
		return true;
	}

	public void setTraceManager(TraceManSpec traceManager) {
        this.traceManager = traceManager;
    }

}
