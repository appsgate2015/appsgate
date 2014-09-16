package appsgate.lig.scheduler;

import appsgate.lig.clock.sensor.messages.ClockSetNotificationMsg;
import appsgate.lig.clock.sensor.messages.FlowRateSetNotification;
import appsgate.lig.clock.sensor.spec.AlarmEventObserver;

/**
 */
public interface SchedulerSpec extends AlarmEventObserver{

	/**
	 * This constant define the t0 -> t1 interval in which we observe the Calendar Events (in ms)
	 */
	public static final long TIME_INTERVAL = 24*60*60*1000;

	/**
	 * Even if no events from the clock, the scheduler will scan again all events in the calendar
	 * at this rate in ms
	 * (it should be defined depends on the time flow rate, 10 minutes intervals in real time should be fine)
	 */
	public static final long BASE_REFRESH = 10*60*1000;

	/**
	 * A single scheduler monitor a single calendar, this is the getter
	 * @return
	 */
	public String getCalendarId();

	/**
	 * A single scheduler monitor a single calendar, this is the setter
	 * @param calendarId
	 */
	public void setCalendarId(String calendarId);
	
	/**
	 * This method implements the main behavior of the scheduler
	 * that polls the calendar to get new events or update existing ones
	 */
	public void refreshScheduler() throws SchedulingException;
	
	/**
	 * Clear all events registered and then refresh 
	 */
	public void resetScheduler() throws SchedulingException;


	/**
	 * The method implements what to do when a registered events occurs
	 */
	@Override
	public void alarmEventFired(int alarmId);


	/**
	 * Called by ApAM when clock changes flow rate
	 * part by calling the sendService
	 *
	 * @param notif the notification message from ApAM
	 */
	public void clockFlowRateChanged(FlowRateSetNotification notif);

	/**
	 * Called by ApAM when clock changes changed current time
	 *
	 * @param notif the notification message from ApAM
	 */
	public void clockSetChanged(ClockSetNotificationMsg notif);
}
