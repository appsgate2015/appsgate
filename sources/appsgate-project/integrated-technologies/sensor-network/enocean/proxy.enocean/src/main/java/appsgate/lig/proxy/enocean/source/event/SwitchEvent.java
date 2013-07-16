package appsgate.lig.proxy.enocean.source.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.proxy.enocean.EnOceanProxy;

import fr.imag.adele.apam.Instance;
import fr.immotronic.ubikit.pems.enocean.event.out.SwitchOnEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.SwitchOffEvent;

/**
 * This class is a wrapper of enocean pem (Ubikit) events for switch sensors.
 * 
 * @author Cédric Gérard
 * @since January 8, 2013
 * @version 1.0.0
 * 
 */
public class SwitchEvent implements SwitchOnEvent.Listener,
		SwitchOffEvent.Listener {

	/**
	 * class logger member
	 */
	private static Logger logger = LoggerFactory.getLogger(SwitchEvent.class);

	/**
	 * EnOcean iPojo Adapter
	 */
	private EnOceanProxy enocean;

	/**
	 * Build a new switch event
	 * 
	 * @param enocean
	 */
	public SwitchEvent(EnOceanProxy enocean) {
		super();
		this.enocean = enocean;
	}

	// @Override
	public void onEvent(SwitchOnEvent arg0) {
		logger.info("The switch " + arg0.getSourceItemUID()
				+ ", state changed to on with button  "
				+ arg0.getSwitchNumber());
		Instance instRef = enocean.getSensorInstance(arg0.getSourceItemUID());
		instRef.setProperty("buttonStatus", "true");
		instRef.setProperty("switchNumber",
				String.valueOf(arg0.getSwitchNumber()));
		instRef.setProperty("switchState", true);
	}

	// @Override
	public void onEvent(SwitchOffEvent arg0) {
		logger.info("The switch " + arg0.getSourceItemUID()
				+ ", state changed to off with button "
				+ arg0.getSwitchNumber());
		Instance instRef = enocean.getSensorInstance(arg0.getSourceItemUID());
		instRef.setProperty("buttonStatus", "false");
		instRef.setProperty("switchNumber",
				String.valueOf(arg0.getSwitchNumber()));
		instRef.setProperty("switchState", true);
	}

}