package appsgate.lig.google.scheduler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.clock.sensor.messages.ClockSetNotificationMsg;
import appsgate.lig.clock.sensor.messages.FlowRateSetNotification;
import appsgate.lig.clock.sensor.spec.AlarmEventObserver;
import appsgate.lig.clock.sensor.spec.CoreClockSpec;
import appsgate.lig.google.helpers.GoogleCalendarReader;
import appsgate.lig.google.helpers.GoogleCalendarWriter;
import appsgate.lig.google.services.GoogleAppsGateEvent;


/**
 */
public class GoogleScheduler implements AlarmEventObserver {


	private static Logger logger = LoggerFactory.getLogger(GoogleScheduler.class);
	
	/**
	 * This constant define the t0 -> t1 interval in which we observe the Calendar Events (in ms)
	 */
	public static final long TIME_INTERVAL = 24*60*60*1000;
	
	/**
	 * Even if no events from the clock, the scheduler will scan again all events in the calendar
	 * at this rate in ms
	 * (it should be defined depends on the time flow rate, 10 minutes intervals in real time should be fine)
	 */
	public long currentRefresh = BASE_REFRESH;
	public static final long BASE_REFRESH = 10*60*1000;


	Object lock;
	CoreClockSpec clock;
	
	Set<Integer> currentAlarms=new HashSet<Integer>();
	Map<Integer, String> currentEventsId = new HashMap<Integer, String>();
	Map<String, GoogleAppsGateEvent> eventMap = new HashMap<String, GoogleAppsGateEvent>();

	public GoogleScheduler() {
		lock=new Object();
	}

	public void start() {		
		//        SynchroObserverTask nextRefresh = new SynchroObserverTask(this);
		//        timer = new Timer();
		// Next refresh will in 30secs (DB and web service should be available by then)
		//        timer.schedule(nextRefresh, 30 * 1000);
		//        logger.trace("started successfully, waiting for SynchroObserverTask to wake up");
	}
	
	public void refresh() {
		// TODO
	}
	
	public void resetScheduler() {
		if(clock!=null) {
			logger.warn("No CoreClock bound, aborting refreshTimer and reseting current Alarms");
			return;
		}
		
		synchronized (lock) {
			// Step 1: unregister alarms 
			for(Integer i : currentAlarms) {
				clock.unregisterAlarm(i.intValue());
			}
			currentAlarms.clear();
			currentEventsId.clear();
			eventMap.clear();
			
			// Step 2: get the current clockTime
			long currentTime = clock.getCurrentTimeInMillis();
			long endPeriod = currentTime + TIME_INTERVAL;
			
			Map<String, String> urlParameters=new HashMap<String, String>();			
			SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			urlParameters.put(GoogleCalendarReader.PARAM_TIMEMIN, dateFormat.format(new Date(currentTime)));
			urlParameters.put(GoogleCalendarReader.PARAM_TIMEMIN, dateFormat.format(new Date(endPeriod)));
			

			
			// Step 3: Get the events in the time Interval
//			GoogleCalendarReader.getEvents(apiKey, accessTokenType, accessTokenValue, calendarId, urlParameters);
			
			
			
			// Step 4: Register all the events whose start time between the nearest one and one minute later
			
			// Idee, utiliser les update pour voir si l'évenement a changé
			
		}
		
	}

	@Override
	public void alarmEventFired(int alarmId) {
		synchronized (lock) {
			if(currentAlarms.contains(new Integer(alarmId))) {
				logger.warn("Ignoring alarmId :"+alarmId+", not part of current AlarmId");
			} else {
				logger.debug("alarmId : "+alarmId+" triggered, calling the corresponding program");
				//TODO : call the program action corresponding to the alarm Id
				//TODO: Unregister the event
				
				//TODO: Maybe refresh the whole agenda ? Or how to check ?
				
				
			}
			
			
		}		
	}
	
	
	
    /**
     * Called by ApAM when clock changes flow rate
     * part by calling the sendService
     *
     * @param notif the notification message from ApAM
     */
    public void clockFlowRateChanged(FlowRateSetNotification notif) {
        logger.debug("clockFlowRateChanged(FlowRateSetNotification notif =  " + notif.JSONize()+")");
        currentRefresh = BASE_REFRESH  * Long.parseLong(notif.getNewValue());
        refreshTask(currentRefresh);
        logger.debug("refresh Task successfully changed");
        
    }
    
    Timer timer=null;
    
	private synchronized void refreshTask(long nextRefresh) {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		ScheduleAutoRefresh refreshTask = new ScheduleAutoRefresh(this);
		timer = new Timer();
		timer.schedule(refreshTask, nextRefresh);
	}    
    
    
    /**
     * Called by ApAM when clock changes changed current time
     *
     * @param notif the notification message from ApAM
     */
    public void clockSetChanged(ClockSetNotificationMsg notif) {
        logger.debug("clockSetChanged(ClockSetNotificationMsg notif =  " + notif.JSONize()+")");
        resetScheduler();
        logger.debug("All schedules have been computed again");
    }	
	
	

}