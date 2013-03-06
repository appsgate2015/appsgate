package appsgate.lig.proxy.enocean.source.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.proxy.enocean.EnOceanProxy;

import fr.imag.adele.apam.Instance;
import fr.immotronic.ubikit.pems.enocean.event.out.IlluminationEvent;

public class LumEvent implements IlluminationEvent.Listener {
	
	// class logger member
	private static Logger logger = LoggerFactory.getLogger(LumEvent.class);
	
	private EnOceanProxy enocean;

	public LumEvent(EnOceanProxy enocean) {
		super();
		this.enocean = enocean;
	}

	//@Override
	public void onEvent(IlluminationEvent arg0) {
		logger.info("This is illumination from " + arg0.getSourceItemUID()
				+ " and in the desk the illumination is about "
				+ arg0.getIllumination() +" lux");
		Instance instRef = enocean.getSensorInstance(arg0.getSourceItemUID());
		instRef.setProperty("currentIllumination", String.valueOf(arg0.getIllumination()));
	}

}
