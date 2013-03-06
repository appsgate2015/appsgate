package appsgate.lig.proxy.enocean.source.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.pem.event.EnterPairingModeEvent;
import org.ubikit.pem.event.ExitPairingModeEvent;

import appsgate.lig.proxy.enocean.EnOceanProxy;

public class PairingModeEvent implements EnterPairingModeEvent.Listener, ExitPairingModeEvent.Listener  {
	
	/**
	 * class logger member
	 */
	private static Logger logger = LoggerFactory.getLogger(EnOceanProxy.class);
	
	private EnOceanProxy enocean;

	public PairingModeEvent(EnOceanProxy enocean) {
		super();
		this.enocean = enocean;
	}

	/**
	 * This method is a listener method call when Ubikit activate its pairing mode
	 * 
	 * @see ExitPairingModeEvent
	 */
	//@Override
	public void onEvent(EnterPairingModeEvent arg0) {
		logger.info("Pem EnOcean start pairing mode.");
		enocean.pairingModeChanged(true);
	}

	/**
	 * This method is a listener method call when Ubikit deactivate its pairing mode
	 * 
	 * @see ExitPairingModeEvent
	 */
	//@Override
	public void onEvent(ExitPairingModeEvent arg0) {
		logger.info("Pem EnOcean stopped pairing mode.");
		enocean.pairingModeChanged(false);
	}

}
