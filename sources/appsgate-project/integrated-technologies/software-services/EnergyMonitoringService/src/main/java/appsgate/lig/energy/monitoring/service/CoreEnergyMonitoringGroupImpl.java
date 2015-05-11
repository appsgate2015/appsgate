/**
 * 
 */
package appsgate.lig.energy.monitoring.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.clock.sensor.spec.CoreClockSpec;
import appsgate.lig.core.object.messages.CoreNotificationMsg;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectBehavior;
import appsgate.lig.core.object.spec.CoreObjectSpec;
import appsgate.lig.energy.monitoring.group.CoreEnergyMonitoringGroup;
import appsgate.lig.energy.monitoring.service.models.ActiveEnergySensor;
import appsgate.lig.scheduler.ScheduledInstruction;
import appsgate.lig.scheduler.ScheduledInstruction.Commands;
import appsgate.lig.scheduler.SchedulerEvent;
import appsgate.lig.scheduler.SchedulerEvent.BasicRecurrencePattern;
import appsgate.lig.scheduler.SchedulerSpec;
import appsgate.lig.scheduler.SchedulingException;
import appsgate.lig.scheduler.utils.DateFormatter;

/**
 * @author thibaud
 *
 */
public class CoreEnergyMonitoringGroupImpl extends CoreObjectBehavior 
	implements CoreObjectSpec,
		CoreEnergyMonitoringGroup {
	
	public static final String IMPL_NAME = "CoreEnergyMonitoringGroupImpl";

	private final static Logger logger = LoggerFactory.getLogger(CoreEnergyMonitoringGroupImpl.class);
	
    /**
     * CoreObject Stuff
     */
	private String serviceId;
	private String userType;
	private int status;
	
	/**
	 * This field should be injected at startup
	 */
	private String name;
	
	/**
	 * Should be injected by ApAM
	 */
	private SchedulerSpec scheduler;
	
	
	
	/**
	 * The String is the sensorID, the value holds the measures, index and state
	 */
	private Map<String, ActiveEnergySensor> sensors;
	double budgetTotal;
	double budgetUnit;
	ArrayList<String> periods;
	
	double lastTotal = 0;
	double lastEnergyDuringPeriod = 0;
	//Remaining values are used when an existing group is restored for the database
	double remainingDuringPeriod = 0;
	double remainingTotal=0;
	
	private boolean isMonitoring = false;
	
	long lastResetTimestamp;
	long lastStartTimeStamp;
	long lastStopTimeStamp;
	
	private CoreClockSpec clock;
	
	private Map<String, String> annotations;
	private JSONArray annotationsAsJSON; //duplicate collection to improve performance
	
	private JSONArray energyHistory; // This is a write only collection
	
	
	
	public CoreEnergyMonitoringGroupImpl() {
    	userType = CoreEnergyMonitoringGroup.class.getSimpleName();
    	status = 2;
	}

	/**
	 * Callback when new apam Instance is created
	 */
	public void onInit() {
		logger.trace("onInit()");
		sensors = new HashMap<String, ActiveEnergySensor>();
	}
	
	/**
	 * This one configures a new Energy monitoring group, with no period attached
	 * @param sensors
	 * @param budgetTotal
	 * @param budgetUnit
	 */
	public void configureNew(JSONArray sensors,
			double budgetTotal, double budgetUnit) {
		logger.trace("configureNew(JSONArray sensors : {}, "
				+ "double budgetTotal : {}, double budgetUnit : {})",
				sensors, budgetTotal, budgetUnit);
		// The configuration of serviceId and name MUST have already be injected during instance creation 
		
		setEnergySensorsGroup(sensors);
		this.budgetTotal = budgetTotal;
		this.budgetUnit = budgetUnit;
		
		this.periods = new ArrayList<String>();
		this.annotations = new HashMap<String, String>();
		annotationsAsJSON = new JSONArray();
		energyHistory = new JSONArray();
		
		lastResetTimestamp = clock.getCurrentTimeInMillis();
	}

	public void configureFromJSON(JSONObject configuration) {
		logger.trace("configureFromJSON(JSONObject configuration : {})",
				configuration);
		// The configuration of serviceId and name MUST have already be injected during instance creation 

		remainingTotal = configuration.optDouble(RAW_ENERGY_KEY, 0);
		remainingDuringPeriod = configuration.optDouble(RAW_ENERGYDURINGPERIOD_KEY, 0);
		
		lastTotal = remainingTotal;
		lastEnergyDuringPeriod = remainingDuringPeriod;
		
		budgetTotal = configuration.optDouble(BUDGETTOTAL_KEY, -1);
		budgetUnit = configuration.optDouble(BUDGETUNIT_KEY, 1);
				
		setEnergySensorsGroup(configuration.optJSONArray(SENSORS_KEY));
		setPeriods(configuration.optJSONArray(PERIODS_KEY));
		
		energyHistory = configuration.optJSONArray(HISTORY_KEY);
		if(energyHistory == null) {
			energyHistory = new JSONArray();
		}
		
		
		this.annotations = new HashMap<String, String>();
		annotationsAsJSON = new JSONArray();
		if(configuration.optJSONArray(ANNOTATIONS_KEY) != null) {
			JSONArray array = configuration.getJSONArray(ANNOTATIONS_KEY);
			for(int i = 0; i < array.length(); i++) {
				JSONObject obj = array.optJSONObject(i);
				if(obj != null
						&& obj.length()>0) {
					this.annotations.put(obj.names().getString(0), 
							obj.getString(obj.names().getString(0)));
					annotationsAsJSON.put(obj);
				}
				
			}			
		}
		
		lastResetTimestamp = configuration.optLong(LASTRESET_KEY, clock.getCurrentTimeInMillis());
		lastStartTimeStamp = configuration.optLong(PERIOD_START, -1);
		lastStopTimeStamp = configuration.optLong(PERIOD_STOP, -1);
		
		// (beware we may have miss information and the overall total may be wrong)
	}	
	
	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		logger.trace("setName(String name : {})",name);
		stateChanged(NAME_KEY, this.name, name);
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#getEnergySensorsGroup()
	 */
	@Override
	public JSONArray getEnergySensorsGroup() {
		return new JSONArray(sensors.keySet());
	}
	
	/**
	 * method to help using contains
	 * @return
	 */
	public Set<String> getEnergySensorsGroupAsSet() {
		return sensors.keySet();
	}


	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#setEnergySensorsGroup(org.json.JSONArray)
	 */
	@Override
	public void setEnergySensorsGroup(JSONArray sensors) {
		logger.trace("setEnergySensorsGroup(JSONArray sensors : {})",sensors);
		this.sensors = new HashMap<String, ActiveEnergySensor>();
		for(int i = 0; sensors!= null
				&& i<sensors.length(); i++) {
			String s = sensors.optString(i);
			if(s!=null) {
				privateAddEnergySensor(s);
			}
		}
		computeEnergy();
		stateChanged(SENSORS_KEY, null, getEnergySensorsGroup().toString());
	}
	
	private double getActiveEnergy(String sensorID) {
		double value = EnergySensorPool.getInstance().getEnergyMeasure(sensorID);
		// We do not call the real sensor to gets its latest energy measure
		if(value >= 0) {
			// If the sensor is known we get the reported latest value
			return value;
		} else {
			// if the sensor is unkonwn 
			// or we may have lost the latest update that was done before adding the sensor
			// We get the max value, just to be sure to reset the sensor on next measure			
			return Double.MAX_VALUE;
		}
	}
	
	private synchronized void privateAddEnergySensor(String sensorID) {	
		sensors.put(sensorID, new ActiveEnergySensor(sensorID,
				getActiveEnergy(sensorID), this));
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#addEnergySensor(java.lang.String)
	 */
	@Override
	public void addEnergySensor(String sensorID) {
		logger.trace("addEnergySensor(String sensorID : {})", sensorID);

		privateAddEnergySensor(sensorID);
		stateChanged(SENSORS_KEY, null, getEnergySensorsGroup().toString());
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#removeEnergySensor(java.lang.String)
	 */
	@Override
	public void removeEnergySensor(String sensorID) {
		logger.trace("removeEnergySensor(String sensorID : {})", sensorID);
		sensors.remove(sensorID);		
		stateChanged(SENSORS_KEY, null, getEnergySensorsGroup().toString());
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#resetEnergy()
	 */
	@Override
	public void resetEnergy() {
		logger.trace("resetEnergy()");
		JSONObject historyLog = getCurrentDescription();
		energyHistory.put(historyLog);
		
		for(ActiveEnergySensor sensor : sensors.values()) {
			sensor.resetEnergy();
		}
		lastResetTimestamp = clock.getCurrentTimeInMillis();		
		remainingTotal = 0;
		remainingDuringPeriod = 0;


		
		stateChanged(BUDGETRESETED_KEY, null, BUDGETRESETED_KEY);
		computeEnergy();
	}
	
	private void computeEnergy() {
		logger.trace("computeEnergy()");
		
		double total = 0;
		double energyDuringPeriod = 0;
		for(ActiveEnergySensor sensor : sensors.values()) {
			total+=sensor.getTotalEnergy();
			energyDuringPeriod+=sensor.getEnergyDuringPeriod();
		}
		
		if(total != lastTotal) {
			logger.trace("computeEnergy(), total energy as changed since last Time");
			stateChanged(ENERGY_KEY, String.valueOf((lastTotal+remainingTotal)*budgetUnit), String.valueOf((total+remainingTotal)*budgetUnit));
			lastTotal = total;
		}
		if(energyDuringPeriod != lastEnergyDuringPeriod) {
			logger.trace("computeEnergy(), energy during period as changed since last Time");
			stateChanged(BUDGETREMAINING_KEY, String.valueOf((lastEnergyDuringPeriod+remainingDuringPeriod)*budgetUnit), String.valueOf((energyDuringPeriod+remainingDuringPeriod)*budgetUnit));
			
			lastEnergyDuringPeriod = energyDuringPeriod;
		}
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#getTotalEnergy()
	 */
	@Override
	public double getTotalEnergy() {
		logger.trace("getTotalEnergy(), returning total: {} x unit : {}", (lastTotal+remainingTotal), budgetUnit);
		return (lastTotal+remainingTotal)*budgetUnit;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#geEnergyDuringTimePeriod()
	 */
	@Override
	public double getEnergyDuringTimePeriod() {
		logger.trace("getEnergyDuringPeriod(), returning energy: {} x unit : {}", lastEnergyDuringPeriod, budgetUnit);
		return (lastEnergyDuringPeriod+remainingDuringPeriod)*budgetUnit;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#getRemainingBudget()
	 */
	@Override
	public double getRemainingBudget() {
		double total = getEnergyDuringTimePeriod();
		logger.trace("getRemainingBudget(), returning budgetTotal: {} - energyConsumed : {}", budgetTotal, total);
		return budgetTotal-total;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#getBudgetTotal()
	 */
	@Override
	public double getBudget() {
		logger.trace("getBudgetTotal(), returning budgetTotal: {} ", budgetTotal);
		return budgetTotal;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#getBudgetUnit()
	 */
	@Override
	public double getBudgetUnit() {
		logger.trace("getBudgetUnit(), returning budgetUnit: {} ", budgetUnit);
		return budgetUnit;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.energy.monitoring.CoreEnergyMonitoringGroup#setBudget(double, double)
	 */
	@Override
	public void setBudget(double budgetTotal) {
		logger.trace("setBudget(double budgetTotal : {}, double budgetUnit : {})", budgetTotal, budgetUnit);
		stateChanged(BUDGETTOTAL_KEY, String.valueOf(this.budgetTotal), String.valueOf(budgetTotal));
		this.budgetTotal = budgetTotal;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.core.object.spec.CoreObjectSpec#getAbstractObjectId()
	 */
	@Override
	public String getAbstractObjectId() {
		return serviceId;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.core.object.spec.CoreObjectSpec#getUserType()
	 */
	@Override
	public String getUserType() {
		return userType;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.core.object.spec.CoreObjectSpec#getObjectStatus()
	 */
	@Override
	public int getObjectStatus() {
		// This is a local service, it is always available
		return status;
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.core.object.spec.CoreObjectSpec#getDescription()
	 */
	@Override
	public JSONObject getDescription() throws JSONException {
		JSONObject descr = getCurrentDescription();
		descr.put("id", getAbstractObjectId());
		descr.put("type", getUserType());
		descr.put("coreType", getCoreType());
		descr.put("status", getObjectStatus());
		descr.put(HISTORY_KEY, String.valueOf(getEnergyHistory()));
		
		return descr;
	}
	
	/**
	 * this one helps preventing recursive loop with reset energy storing all history of the service
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getCurrentDescription() throws JSONException {
		JSONObject descr = new JSONObject();
		
		descr.put(NAME_KEY, getName());
		descr.put(SENSORS_KEY, getEnergySensorsGroup());
		descr.put(ENERGY_KEY, String.valueOf(getTotalEnergy()));
		descr.put(BUDGETTOTAL_KEY, String.valueOf(getBudget()));
		descr.put(BUDGETUNIT_KEY, String.valueOf(getBudgetUnit()));
		descr.put(BUDGETREMAINING_KEY, String.valueOf(getRemainingBudget()));		
		descr.put(PERIODS_KEY, String.valueOf(getPeriods()));
		descr.put(ISMONITORING_KEY, isMonitoring());
		descr.put(LASTRESET_KEY, getLastResetTimestamp());
		descr.put(ANNOTATIONS_KEY, String.valueOf(getAnnotations()));

		/**
		 * Those raw index are mostly used for persistance
		 */
		descr.put(RAW_ENERGY_KEY, lastTotal+remainingTotal);
		descr.put(RAW_ENERGYDURINGPERIOD_KEY, lastEnergyDuringPeriod+remainingDuringPeriod);
		descr.put(PERIOD_START, getLastStartTimestamp());
		descr.put(PERIOD_STOP, getLastStopTimestamp());
		
		return descr;
	}
	

	/* (non-Javadoc)
	 * @see appsgate.lig.core.object.spec.CoreObjectSpec#getCoreType()
	 */
	@Override
	public CORE_TYPE getCoreType() {
		return CORE_TYPE.SERVICE;
	}
	

	@Override
	public JSONArray getPeriods() {
		return new JSONArray(periods);
	}

	@Override
	public String addPeriod(long startDate, long endDate, boolean resetOnStart,
			boolean resetOnEnd, String recurrence) {
		logger.trace("addPeriod(long startDate : {}, long endDate : {}, boolean resetOnStart : {},"
			+" boolean resetOnEnd : {}, String recurrence : {})",startDate, endDate, resetOnStart, resetOnEnd, recurrence);
		
		if(scheduler == null) {
			logger.error("No scheduler found, won't add the period");
			return null;
		}
		
		Set<ScheduledInstruction> onBeginInstructions= new HashSet<ScheduledInstruction>();
		Set<ScheduledInstruction> onEndInstructions= new HashSet<ScheduledInstruction>();
		
		if (startDate > 0) {
			logger.trace("addPeriod(...), adding instruction to start monitoring");
			onBeginInstructions.add(formatInstruction("startMonitoring", null, ScheduledInstruction.ON_BEGIN));
		}
		
		if (endDate > 0) {
			logger.trace("addPeriod(...), adding instruction to stop monitoring");
			onEndInstructions.add(formatInstruction("stopMonitoring", null, ScheduledInstruction.ON_END));
		}
		
		if(resetOnStart) {
			logger.trace("addPeriod(...), adding instruction to reset monitoring at start of period");
			onBeginInstructions.add(formatInstruction("resetEnergy", null, ScheduledInstruction.ON_BEGIN));			
		}
		
		if(resetOnEnd) {
			logger.trace("addPeriod(...), adding instruction to reset monitoring at stop of period");
			onEndInstructions.add(formatInstruction("resetEnergy", null, ScheduledInstruction.ON_END));			
		}
		
		BasicRecurrencePattern pattern;
		try {
			pattern = BasicRecurrencePattern.fromName(recurrence);
		} catch (IllegalArgumentException e1) {
			logger.warn("Recurrence pattern is unknown ",e1 );
			pattern= BasicRecurrencePattern.NONE;
		}
		
		String eventId=null;
		try {
			eventId = scheduler.createEvent("AppsGate Energy Monitoring", onBeginInstructions, onEndInstructions,
					DateFormatter.format(startDate), DateFormatter.format(endDate), pattern);
			periods.add(eventId);
			stateChanged(PERIODS_KEY, null, new JSONArray(this.periods.toString()).toString());
			
		} catch (SchedulingException e) {
			logger.error("Error when trying to add event in the scheduler : ", e);
		}
		return eventId;

	}
	
	private ScheduledInstruction formatInstruction(String methodName, JSONArray args, String trigger) {
		JSONObject target = new JSONObject();
		target.put("objectId", serviceId);
		target.put("method", methodName);
		target.put("args", (args==null?new JSONArray():args));
		target.put("TARGET", "EHMI");
		
		return new ScheduledInstruction(Commands.GENERAL_COMMAND.getName(), target.toString(), trigger);
	}


	@Override
	public void removePeriodById(String eventID) {
		if(scheduler == null) {
			logger.error("No scheduler found, won't remove the period");
			return;
		}
		
		scheduler.removeEvent(eventID);
		periods.remove(eventID);
		stateChanged(PERIODS_KEY, null, new JSONArray(this.periods.toString()).toString());
	}


	@Override
	public JSONObject getPeriodInfo(String eventID) {
		SchedulerEvent period = scheduler.getEvent(eventID);
		if(!periods.contains(eventID) ||period == null) {
			logger.error("getPeriodInfo(String eventID : {}), event not found in the scheduler or in the group events");
			return null;
		}
		
		JSONObject result = new JSONObject();
		
		//deletable ONLY if it contains:
		// one start monitoring instruction
		// one stop monitoring instruction
		// optional one if reset on start (by default considered true)
		// optional one if reset on stop (by default considered true)
		int expected = 2;		
		int found = 0;
		
		// The 'easy' parameters direct from the scheduler 
		result.put(PERIOD_ID, period.getId());
		result.put(PERIOD_NAME, period.getName());
		result.put(PERIOD_START, period.getStartTime());
		result.put(PERIOD_STOP, period.getEndTime());
		result.put(PERIOD_RECURRENCE, period.getRecurrencePattern());
		
		// The 'instructions' parameters parsed from the scheduler
		if(period.instructionsMatchingPattern(
				formatInstruction("resetEnergy", null, ScheduledInstruction.ON_BEGIN).toString()
				).size()>0) {
			result.put(PERIOD_RESETSTART, true);
			expected++;
			found++;
		}
		if(period.instructionsMatchingPattern(
				formatInstruction("resetEnergy", null, ScheduledInstruction.ON_END).toString()
				).size()>0) {
			result.put(PERIOD_RESETSTOP, true);
			expected++;
			found++;
		}
		
		// The 'computed' parameters from the scheduler
		if(period.instructionsMatchingPattern(
				formatInstruction("startMonitoring", null, ScheduledInstruction.ON_BEGIN).toString()
				).size()>0) {
			found++;
		}
		if(period.instructionsMatchingPattern(
				formatInstruction("stopMonitoring", null, ScheduledInstruction.ON_END).toString()
				).size()>0) {
			found++;
		}
		
		if(expected == found) {
			result.put(PERIOD_DELETABLE, true);			
		} else {
			result.put(PERIOD_DELETABLE, false);						
		}
		
		logger.trace("getPeriodInfo(String eventID : {}), returning",eventID, result);

		return result;
	}
	
	@Override
	public void setPeriods(JSONArray periods) {
		logger.trace("setPeriods(JSONArray periods : {})",periods);
		this.periods = new ArrayList<String>();
		for(int i = 0; periods!= null
				&& i<periods.length(); i++) {
			String s = periods.optString(i);
			if(s!=null
					&& scheduler.getEvent(s)!= null
					) {
				this.periods.add(s);
			}
		}
		stateChanged(PERIODS_KEY, null, new JSONArray(this.periods.toString()).toString());
	}
	
	
	private synchronized void addActiveEnergyMeasure(String sensorID, double value) {
		// Here is the core business function, 
		// 1° we have to compare the value with the previous one from this sensor (if any)
		// and accordingly start a new counter
		// 2° increase the total energy from this sensor
		// 3° check if we are in a monitoring period
		// and decrease the budget accordingly
		logger.trace("addActiveEnergyMeasure(String sensorID : {}, double value : {})",sensorID, value);
				
		sensors.get(sensorID).newEnergyMeasure(value);
		computeEnergy();
	}
	

	
	
	private NotificationMsg stateChanged(String varName, String oldValue, String newValue) {
		return new CoreNotificationMsg(varName, oldValue, newValue, this.getAbstractObjectId());
	}


	@Override
	public boolean isMonitoring() {
		return isMonitoring;
	}


	@Override
	public synchronized void startMonitoring() {
		logger.trace("startMonitoring()");
		if(!isMonitoring){
			logger.trace("startMonitoring(), starting monitoring status");
			isMonitoring = true;
			lastStartTimeStamp = clock.getCurrentTimeInMillis();
			stateChanged(ISMONITORING_KEY, "false", "true");
			
		} else {
			logger.trace("startMonitoring(), already monitoring, does nothing");
		}
	}

	@Override
	public synchronized void stopMonitoring() {
		logger.trace("stopMonitoring()");
		logger.trace("startMonitoring()");
		if(isMonitoring){
			logger.trace("stopMonitoring(), stoppping monitoring status");
			isMonitoring = false;
			lastStopTimeStamp = clock.getCurrentTimeInMillis();
			stateChanged(ISMONITORING_KEY, "true","false");
		} else {
			logger.trace("stopMonitoring(), already stopped, does nothing");
		}
	}
	
	public void energyChanged(String sensorId) {
		logger.trace("energyChanged(String sensorId : {})",
				sensorId);
		// basic filtering
		// Maybe useless, this callback should be called only if relevant
		if(sensorId != null && sensors.keySet().contains(sensorId)) {
			addActiveEnergyMeasure(sensorId,
					EnergySensorPool.getInstance().getEnergyMeasure(sensorId));
		} else {
			logger.trace("energyChanged(..), empty sensorID or energyIndex irrelevant");
		}		
	}


	@Override
	public void addAnnotation(String annotation) {
		logger.trace("addAnnotation(String annotation : {})", annotation);
		String key = String.valueOf(clock.getCurrentTimeInMillis());
		updateAnnotation(key, annotation);
	}


	@Override
	public JSONArray getAnnotations() {
		return annotationsAsJSON;
	}
	
	private void computeAnnotationAsJSON() {
		annotationsAsJSON = new JSONArray();
		for(String ts : annotations.keySet()) {
			annotationsAsJSON.put(new JSONObject().put(ts, annotations.get(ts)));
		}
	}
	

	@Override
	public void setBudgetUnit(double budgetUnit) {
		logger.trace("setBudgetUnit(double budgetUnit : {})", budgetUnit);
		stateChanged(BUDGETUNIT_KEY, String.valueOf(this.budgetUnit), String.valueOf(budgetUnit));
		this.budgetUnit = budgetUnit;
	}

	@Override
	public long getLastResetTimestamp() {
		return lastResetTimestamp;
	}
	
	@Override
	public long getLastStartTimestamp() {
		return lastResetTimestamp;
	}
	
	@Override
	public long getLastStopTimestamp() {
		return lastResetTimestamp;
	}	

	@Override
	public void deleteAnnotation(String timestamp) {
		logger.trace("deleteAnnotation(String timestamp : {})", timestamp);
		if(timestamp!=null
				&& timestamp.length()>0
				&& annotations.containsKey(timestamp)) {
			annotations.remove(timestamp);
			computeAnnotationAsJSON();
			stateChanged(ANNOTATIONS_KEY, null, getAnnotations().toString());			
		}
	}

	@Override
	public void updateAnnotation(String timestamp, String annotation) {
		logger.trace("updateAnnotation(String timestamp : {}, String annotation : {})", timestamp, annotation);
		annotations.put(timestamp, annotation);	
		computeAnnotationAsJSON();
		stateChanged(ANNOTATIONS_KEY, null, getAnnotations().toString());
	}

	@Override
	public JSONArray getEnergyHistory() {
		return energyHistory;
	}
	
}
