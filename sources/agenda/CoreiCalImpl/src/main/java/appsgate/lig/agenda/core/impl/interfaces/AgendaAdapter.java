package appsgate.lig.agenda.core.impl.interfaces;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;

public interface AgendaAdapter {
	
	public Calendar getAgenda(String agenda, String account, String password);
	public VEvent addEvent(VEvent newEvent);
	public boolean delEvent (VEvent oldEvent);
	public boolean addAlarm(VAlarm newAlarm);
	public boolean delAlarm(VAlarm oldAlarm);

}
