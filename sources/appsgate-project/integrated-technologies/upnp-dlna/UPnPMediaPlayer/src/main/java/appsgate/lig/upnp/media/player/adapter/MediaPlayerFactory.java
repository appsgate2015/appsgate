

package appsgate.lig.upnp.media.player.adapter;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.upnp.UPnPDevice;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.InstanceImpl;

public class MediaPlayerFactory {
	
	public void mediaRendererBound(Instance device) {
		

		try {
			Implementation adapterImplementtation = CST.apamResolver.findImplByName(null,"MediaPlayer");

			String deviceId = device.getProperty(UPnPDevice.ID);
			String deviceName 	= device.getProperty(UPnPDevice.FRIENDLY_NAME);

			Map<String,Object> configuration = new Hashtable<String,Object>();
			configuration.put(UPnPDevice.ID,deviceId);
			configuration.put(UPnPDevice.FRIENDLY_NAME,deviceName);
			adapterImplementtation.getApformImpl().addDiscoveredInstance(configuration);

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public void mediaRendererUnbound(Instance device) {

		Implementation adapterImplementation = CST.componentBroker.getImpl("MediaPlayer");
		for (Instance player : adapterImplementation.getInsts()) {
			if (player.getProperty(UPnPDevice.ID).equals(device.getPropertyObject(UPnPDevice.ID)))
				((InstanceImpl)player).unregister();
		}
	}

}
