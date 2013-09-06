package appsgate.lig.clock.sensor.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.clock.sensor.messages.ClockSetNotificationMsg;
import appsgate.lig.clock.sensor.spec.AlarmEventObserver;
import appsgate.lig.clock.sensor.spec.CoreClockSpec;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectSpec;

/**
 * This java interface is an ApAM specification shared by all ApAM AppsGate
 * application to provide current Time and Date information
 */
public class SimpleClockImpl implements CoreClockSpec, CoreObjectSpec,
	ChangeListener {

    /**
     * Lag between the real current Date and the one setted in the GUI
     */
    long currentLag;

    /**
     * current flow rate, "1" is the default value
     */
    double flowRate;
    long timeFlowBreakPoint;

    private int currentAlarmId;

    /**
     * A sorted map between the times in millis at which are registered alarms
     * and the corresponding alarm Id and observers
     */
    private SortedMap<Long, Map<Integer,AlarmEventObserver>> alarms;

    /**
     * Convenient to unregister alarms (avoid complexity)
     */
    private Map<Integer, Long> reverseAlarmMap;

    // Calendar currentCalendar;

    JFrame frameClock;
    JSpinner spinDay;
    JSpinner spinMonth;
    JSpinner spinYear;

    JSpinner spinHour;
    JSpinner spinMinute;
    JTextField fieldSecond;

    int oldDay;
    int oldMonth;
    int oldYear;
    int oldHour;
    int oldMinute;

    long nextAlarm;
    Timer timer = new Timer();
    

    /**
     * Static class member uses to log what happened in each instances
     */
    private static Logger logger = LoggerFactory.getLogger(SimpleClockImpl.class);

    public SimpleClockImpl() {
	oldDay = -1;
	oldMonth = -1;
	oldYear = -1;
	oldHour = -1;
	oldMinute = -1;
	initAppsgateFields();

	resetClock();
    }

    /**
     * Called by APAM when an instance of this implementation is created
     */
    public void start() {
	logger.info("New swing clock created");


	refreshClock();
	Timer refreshTimer = new Timer();
	refreshTimer.scheduleAtFixedRate(refreshtask, Calendar.getInstance()
		.getTime(), 1000);
    }

    public void stop() {
	logger.info("Swing Clock removed");
	frameClock.dispose();

    }

    public void show() {
	if (frameClock == null || !frameClock.isVisible()) {

	    frameClock = new JFrame("AppsGate Swing Clock");

	    JPanel datePanel = new JPanel();
	    datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.X_AXIS));
	    datePanel.setBorder(BorderFactory.createTitledBorder("Date"));

	    JPanel timePanel = new JPanel();
	    timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));
	    timePanel.setBorder(BorderFactory.createTitledBorder("Time"));

	    frameClock.setLayout(new BoxLayout(frameClock.getContentPane(),
		    BoxLayout.Y_AXIS));
	    frameClock.add(datePanel);
	    frameClock.add(timePanel);

	    SpinnerModel dayModel = new SpinnerNumberModel(1, 1, 31, 1);
	    datePanel.add(new JLabel("Day :"));
	    spinDay = new JSpinner(dayModel);
	    spinDay.addChangeListener(this);
	    datePanel.add(spinDay);

	    SpinnerModel monthModel = new SpinnerNumberModel(1, 1, 12, 1);
	    datePanel.add(new JLabel(" Month :"));
	    spinMonth = new JSpinner(monthModel);
	    spinMonth.addChangeListener(this);
	    datePanel.add(spinMonth);

	    SpinnerModel yearModel = new SpinnerNumberModel(1971, 1971, 2050, 1);
	    datePanel.add(new JLabel(" Year :"));
	    spinYear = new JSpinner(yearModel);
	    spinYear.addChangeListener(this);
	    datePanel.add(spinYear);

	    SpinnerModel hourModel = new SpinnerNumberModel(0, 0, 23, 1);
	    spinHour = new JSpinner(hourModel);
	    spinHour.addChangeListener(this);
	    timePanel.add(spinHour);
	    timePanel.add(new JLabel("H   "));

	    SpinnerModel minuteModel = new SpinnerNumberModel(0, 0, 59, 1);
	    spinMinute = new JSpinner(minuteModel);
	    spinMinute.addChangeListener(this);
	    timePanel.add(spinMinute);
	    timePanel.add(new JLabel("m   "));

	    fieldSecond = new JTextField(2);
	    fieldSecond.setEditable(false);
	    timePanel.add(fieldSecond);
	    timePanel.add(new JLabel("s "));

	    refreshClock();

	    frameClock.pack();
	    frameClock.setVisible(true);
	}
    }

    /**
     * Called by APAM when an instance of this implementation is removed
     */
    public void hide() {
	if (frameClock != null && frameClock.isVisible()) {
	    frameClock.setVisible(false);
	    frameClock.dispose();
	    spinDay = null;
	    spinMinute = null;
	    spinMonth = null;
	    spinYear = null;
	    spinHour = null;
	    fieldSecond = null;
	    frameClock = null;
	}
    }

    private void refreshClock() {

	if (frameClock != null && frameClock.isVisible()) {

	    Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(cal.getTimeInMillis() + currentLag);

	    if (oldDay != cal.get(Calendar.DAY_OF_MONTH)) {
		spinDay.setValue(cal.get(Calendar.DAY_OF_MONTH));
	    }
	    if (oldMonth != cal.get(Calendar.MONTH)) {
		spinMonth.setValue(cal.get(Calendar.MONTH) + 1);
	    }
	    if (oldYear != cal.get(Calendar.YEAR)) {
		spinYear.setValue(cal.get(Calendar.YEAR));
	    }
	    if (oldHour != cal.get(Calendar.HOUR_OF_DAY)) {
		spinHour.setValue(cal.get(Calendar.HOUR_OF_DAY));
	    }
	    if (oldMinute != cal.get(Calendar.MINUTE)) {
		spinMinute.setValue(cal.get(Calendar.MINUTE));
	    }
	    fieldSecond.setText(String.valueOf(cal.get(Calendar.SECOND)));

	    oldDay = cal.get(Calendar.DAY_OF_MONTH);
	    oldMonth = cal.get(Calendar.MONTH);
	    oldYear = cal.get(Calendar.YEAR);
	    oldHour = cal.get(Calendar.HOUR_OF_DAY);
	    oldMinute = cal.get(Calendar.MINUTE);
	}

    }

    public NotificationMsg fireClockSetNotificationMsg(Calendar currentTime) {
	return new ClockSetNotificationMsg(this, currentTime.getTime()
		.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */
    @Override
    public void stateChanged(ChangeEvent event) {
	try {
	    Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(cal.getTimeInMillis() + currentLag);
	    if (event.getSource().equals(spinDay))
		cal.set(Calendar.DAY_OF_MONTH,
			Integer.parseInt(spinDay.getValue().toString()));
	    if (event.getSource().equals(spinMonth))
		cal.set(Calendar.MONTH,
			Integer.parseInt(spinMonth.getValue().toString()) - 1);
	    if (event.getSource().equals(spinYear))
		cal.set(Calendar.YEAR,
			Integer.parseInt(spinYear.getValue().toString()));
	    if (event.getSource().equals(spinHour))
		cal.set(Calendar.HOUR_OF_DAY,
			Integer.parseInt(spinHour.getValue().toString()));
	    if (event.getSource().equals(spinMinute))
		cal.set(Calendar.MINUTE,
			Integer.parseInt(spinMinute.getValue().toString()));
	    currentLag = cal.getTimeInMillis()
		    - Calendar.getInstance().getTimeInMillis();

	    if (currentLag > 500) {
		logger.info("Clock lag updated to simulate a false date : "
			+ (long) currentLag / 1000);
		refreshClock();
		fireClockSetNotificationMsg(cal);
	    }

	} catch (NumberFormatException exc) {
	    logger.warn("Number Format Exception : " + exc.getMessage()
		    + ", reset Clock !");
	    resetClock();
	    refreshClock();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.clock.sensor.spec.CoreClockSpec#resetClock()
     */
    @Override
    public void resetClock() {
	synchronized (appsgateObjectId) {
	    currentLag = 0;
	    flowRate = 1;
	    currentAlarmId = 0;
	    alarms = new TreeMap<Long, Map<Integer,AlarmEventObserver>>();
	    reverseAlarmMap = new HashMap<Integer, Long>();
	    nextAlarm = -1;
	    timer.purge();
	    timeFlowBreakPoint=-1;
	}
	fireClockSetNotificationMsg(Calendar.getInstance());
    }

    /**
     * The task that is executed automatically to refresh the local calendar
     */
    TimerTask refreshtask = new TimerTask() {
	@Override
	public void run() {
	    refreshClock();
	}
    };

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.clock.sensor.spec.CoreClockSpec#getCurrentDate()
     */
    @Override
    public Calendar getCurrentDate() {
	Calendar cal = Calendar.getInstance();
	cal.setTimeInMillis(getCurrentTimeInMillis());
	return cal;
    }

    /*	if

     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.clock.sensor.spec.CoreClockSpec#getCurrentTimeInMillis()
     */
    @Override
    public long getCurrentTimeInMillis() {
	// TODO with flow might be more difficult
	
	long systemTime = System.currentTimeMillis();
	long simulatedTime = currentLag;
	if(timeFlowBreakPoint>0 && timeFlowBreakPoint<=systemTime) {
	    long elapsedTime = (long)((systemTime - timeFlowBreakPoint)*flowRate);
		logger.debug("getCurrentTimeInMillis(), system time : "+systemTime
			+", time flow breakpoint : "+ timeFlowBreakPoint
			+", elasped time : "+elapsedTime);
	    
	    simulatedTime += timeFlowBreakPoint+elapsedTime;
	} else 
	    simulatedTime+=systemTime;
	logger.debug("getCurrentTimeInMillis(), simulated time : "+simulatedTime);
	return simulatedTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.clock.sensor.spec.CoreClockSpec#setCurrentDate(java.util
     * .Calendar)
     */
    @Override
    public void setCurrentDate(Calendar calendar) {
	setCurrentTimeInMillis(calendar.getTimeInMillis());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.clock.sensor.spec.CoreClockSpec#setCurrentTimeInMillis(long)
     */
    @Override
    public void setCurrentTimeInMillis(long millis) {
	currentLag = millis - Calendar.getInstance().getTimeInMillis();
	setTimeFlowRate(flowRate);
	calculateNextTimer();
    }

    private String appsgatePictureId;
    private String appsgateObjectId;
    private String appsgateUserType;
    private String appsgateStatus;
    private String appsgateServiceName;

    private void initAppsgateFields() {
	appsgatePictureId = null;
	appsgateServiceName = "Swing Clock";
	appsgateUserType = "21";
	appsgateStatus = "2";
	appsgateObjectId = appsgateUserType + String.valueOf(this.hashCode());
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.core.object.spec.CoreObjectSpec#getAbstractObjectId()
     */
    @Override
    public String getAbstractObjectId() {
	return appsgateObjectId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.core.object.spec.CoreObjectSpec#getDescription()
     */
    @Override
    public JSONObject getDescription() throws JSONException {
	JSONObject descr = new JSONObject();

	// mandatory appsgate properties
	descr.put("id", appsgateObjectId);
	descr.put("type", appsgateUserType); // 21 for clock
	descr.put("status", appsgateStatus);
	descr.put("name", appsgateServiceName);

	descr.put("varName", "ClockSet");
	Calendar cal = Calendar.getInstance();
	cal.setTimeInMillis(cal.getTimeInMillis() + currentLag);
	descr.put("value", cal.getTime().toString());

	return descr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.core.object.spec.CoreObjectSpec#getObjectStatus()
     */
    @Override
    public int getObjectStatus() {
	return Integer.parseInt(appsgateStatus);
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.core.object.spec.CoreObjectSpec#getPictureId()
     */
    @Override
    public String getPictureId() {
	return appsgatePictureId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.core.object.spec.CoreObjectSpec#getUserType()
     */
    @Override
    public String getUserType() {
	return appsgateUserType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.core.object.spec.CoreObjectSpec#setPictureId(java.lang.String
     * )
     */
    @Override
    public void setPictureId(String pictureId) {
	this.appsgatePictureId = pictureId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.clock.sensor.spec.CoreClockSpec#goAlongUntil(long)
     */
    @Override
    public void goAlongUntil(long millis) {
	synchronized (appsgateObjectId) {
		Long currentTime = getCurrentTimeInMillis();
		SortedMap<Long, Map<Integer,AlarmEventObserver>> alarmsToFire= alarms.subMap(currentTime, currentTime+millis);
		if(alarmsToFire!=null)
		    for(Map<Integer, AlarmEventObserver> obs : alarmsToFire.values())
			fireClockAlarms(obs);
		
		setCurrentTimeInMillis(currentTime+millis);
	}
	calculateNextTimer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.clock.sensor.spec.CoreClockSpec#registerAlarm(java.util.
     * Calendar, appsgate.lig.clock.sensor.spec.AlarmEventObserver)
     */
    @Override
    public int registerAlarm(Calendar calendar, AlarmEventObserver observer) {
	if (calendar != null && observer != null) {
	    synchronized (appsgateObjectId) {
		Long time = getCurrentTimeInMillis();
		if (alarms.containsKey(time)) {
		    logger.debug("registerAlarm(...), alarm events already registered for this time, adding this one");
		    Map<Integer, AlarmEventObserver> observers = alarms
			    .get(time);
		    observers.put(++currentAlarmId, observer);
		    reverseAlarmMap.put(currentAlarmId, time);
		} else {
		    logger.debug("registerAlarm(...), alarm events not registered for this time, creating a new one");
		    /**
		     * An map between the alarm event ID and the associated
		     * observers
		     */
		    Map<Integer, AlarmEventObserver> observers = new HashMap<Integer, AlarmEventObserver>();
		    observers.put(++currentAlarmId, observer);
		    alarms.put(time, observers);
		    reverseAlarmMap.put(currentAlarmId, time);
		}
		logger.debug("registerAlarm(...), alarm events id created : "
			+ currentAlarmId);
		calculateNextTimer();
		return currentAlarmId;
	    }
	} else {
	    logger.debug("registerAlarm(...), calendar or oberver is null, does not register the alarm");
	    return -1;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.clock.sensor.spec.CoreClockSpec#unregisterAlarm(int)
     */
    @Override
    public void unregisterAlarm(int alarmEventId) {
	synchronized (appsgateObjectId) {
	    Long time = reverseAlarmMap.get(alarmEventId);
	    Map<Integer, AlarmEventObserver> observers = alarms.get(time);
	    observers.remove(alarmEventId);
	    if (observers.size() < 1)
		alarms.remove(time);
	    reverseAlarmMap.remove(alarmEventId);
	    calculateNextTimer();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.clock.sensor.spec.CoreClockSpec#setTimeFlowRate(double)
     */
    @Override
    public double setTimeFlowRate(double rate) {
	if(rate>0 && rate!=1) {
	    // avoid value that could lead to strange behavior
	    timeFlowBreakPoint=System.currentTimeMillis();
	    if(flowRate<0.01)
		flowRate=0.01;
	    if(flowRate>100)
		flowRate=100;
	    else flowRate=rate;
	} else {
		flowRate = 1;
		timeFlowBreakPoint=-1;
	}
	logger.debug("setTimeFlowRate(double rate), new time flow rate : "+flowRate);
	return flowRate;

    }

    private void calculateNextTimer() {
	synchronized (appsgateObjectId) {
	    Long currentTime = getCurrentTimeInMillis();
	    if (alarms != null && !alarms.isEmpty()
		    && alarms.lastKey() >= currentTime) {
		nextAlarm = alarms.tailMap(currentTime).firstKey();
		long nextAlarmDelay = nextAlarm - currentTime;
		nextAlarmDelay = (long) (nextAlarmDelay / flowRate);
		logger.debug("calculateNextTimer(), next alarm should ring in : "
			+ nextAlarmDelay / 1000 + "s");
		timer.cancel();
		timer.schedule(alarmFiringTask, nextAlarmDelay);
	    }
	}

    }

    TimerTask alarmFiringTask = new TimerTask() {
	@Override
	public void run() {
	    logger.debug("Firing current clock alarms");
	    if (nextAlarm > 0) {
		Map<Integer, AlarmEventObserver> observers = alarms
			.remove(nextAlarm);
		fireClockAlarms(observers);
		nextAlarm = -1;
		calculateNextTimer();
	    }
	}
    };
    
    public void fireClockAlarms(Map<Integer, AlarmEventObserver> observers) {
	for (Integer i : observers.keySet()) {
	    logger.debug("fireClockAlarms(...), AlarmEventId : " + i);
	    AlarmEventObserver obs = observers.remove(i);
	    if (obs != null)
		obs.alarmEventFired(i.intValue());
	}
    }

}
