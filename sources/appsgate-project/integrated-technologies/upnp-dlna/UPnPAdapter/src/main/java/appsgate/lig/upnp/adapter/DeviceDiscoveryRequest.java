package appsgate.lig.upnp.adapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

import appsgate.lig.core.object.spec.CoreObjectSpec;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.impl.InstanceImpl;

/**
 * The handler of the discovery request, It look for a proxy for each service and create the
 * appropriate instance.
 * 
 */
public class DeviceDiscoveryRequest implements Runnable {

	/**
	 * The discovered device
	 */
	private final UPnPDevice device;
	
	private ApamResolver resolver;
	private final BundleContext context;
	
	
	/**
	 * The list of created proxies for each discovered device
	 * 
	 */
	private Map<UPnPDevice,List<ApformInstance>> discoveredDeviceProxies = new HashMap<UPnPDevice, List<ApformInstance>>() ;
	
	
	public DeviceDiscoveryRequest(UPnPDevice device, Map<UPnPDevice,List<ApformInstance>> discoveredDeviceProxies,
			ApamResolver resolver, BundleContext context) {
		this.device = device;
		this.discoveredDeviceProxies = discoveredDeviceProxies;
		this.resolver = resolver;
		this.context = context;		
	}
	
	@Override
	public void run() {

		String deviceId		= (String)device.getDescriptions(null).get(UPnPDevice.ID);
		String deviceType	= (String)device.getDescriptions(null).get(UPnPDevice.TYPE);
		

		/*
		 * IMPORTANT Because we are processing this event asynchronously, we need to verify that the device is
		 * still available, and abort the processing as soon as possible.
		 */
		synchronized (discoveredDeviceProxies) {
			if (! discoveredDeviceProxies.containsKey(device))
				return;
		}
		
		/*
		 * IMPORTANT Because we are processing this event asynchronously, we need to verify that APAM is
		 * still available, and abort the processing as soon as possible.
		 */
		synchronized (this) {
			if (resolver == null)
				return;
		}

		System.err.println("[UPnP Apam Discovery] Device ("+deviceType+") discovered :"+deviceId);

		/*
		 * Look for an implementation 
		 */
		
		Implementation implementation	= resolver.resolveSpecByName(null,CoreObjectSpec.class.getSimpleName(),
														Collections.singleton("("+UPnPDevice.TYPE+"="+deviceType+")"),null);
		
		if (implementation == null) {
			System.err.println("[UPnP Apam Discovery] Proxy not found for device type  "+deviceType);
		}
		else {
			try {
				
				System.err.println("[UPnP Apam Discovery] Proxy found for device type "+deviceType+" : "+implementation.getName());
	
				/*
				 * Create an instance of the proxy, and configure it for the appropriate device id
				 */
	
				Map<String,Object> configuration = new Hashtable<String,Object>();
				configuration.put(UPnPDevice.ID,deviceId);
	
				ApformInstance proxy = implementation.getApformImpl().addDiscoveredInstance(configuration);
				
				/*
				 * Ignore errors creating the proxy
				 */
				if (proxy == null) {
					System.err.println("[UPnP Apam Discovery] Proxy could not be instantiated  "+implementation.getName());
				}
				else {
					/*
					 * Update the service map
					 */
					synchronized (discoveredDeviceProxies) {
						
						/*
						 * If the device is no longer available, just dispose the created proxy and abort processing 
						 */
						if (! discoveredDeviceProxies.containsKey(device)) {
							if (proxy.getApamComponent() != null)
								((InstanceImpl)proxy.getApamComponent()).unregister();
							return;
						}
		
						/*
						 * otherwise add it to the map
						 */
						discoveredDeviceProxies.get(device).add(proxy);
					}
				}
	
			} catch (Exception e) {
				System.err.println("[UPnP Apam Discovery] Proxy could not instantiated  "+implementation.getName());
				e.printStackTrace();
			}
		
		}
		/*
		 * Iterate over all declared service of the device, creating the associated proxy
		 */
		
		for (UPnPService service : device.getServices()) {
			
			/*
			 * IMPORTANT Because we are processing this event asynchronously, we need to verify that the device is
			 * still available, and abort the processing as soon as possible.
			 */
			synchronized (discoveredDeviceProxies) {
				if (! discoveredDeviceProxies.containsKey(device))
					return;
			}
			
			/*
			 * IMPORTANT Because we are processing this event asynchronously, we need to verify that APAM is
			 * still available, and abort the processing as soon as possible.
			 */
			synchronized (this) {
				if (resolver == null)
					return;
			}


			/*
			 * Look for an implementation 
			 */
			implementation	= resolver.resolveSpecByName(null,CoreObjectSpec.class.getSimpleName(),
											Collections.singleton("("+UPnPService.TYPE+"="+service.getType()+")"),null);
			
			if (implementation == null) {
				System.err.println("[UPnP Apam Discovery] Proxy not found for service type  "+service.getType());
				continue;
			}
			
			try {
				
				System.err.println("[UPnP Apam Discovery] Proxy found for service type "+service.getType()+" : "+implementation.getName());

				final String serviceFilter =	"(&" +
													"("+UPnPDevice.ID+"="+deviceId+")" +
													"("+UPnPService.ID+"="+service.getId()+")" +
												")";

				/*
				 * Create an instance of the proxy, and configure it for the appropriate device and service
				 */

				Map<String,Object> configuration = new Hashtable<String,Object>();
				configuration.put(UPnPDevice.ID,deviceId);
				configuration.put(UPnPService.ID,service.getId());
				configuration.put(UPnPEventListener.UPNP_FILTER, context.createFilter(serviceFilter));
				configuration.put("requires.filters", new Hashtable<String,String>(Collections.singletonMap(UPnPDevice.ID,serviceFilter)) );

				ApformInstance proxy = implementation.getApformImpl().addDiscoveredInstance(configuration);
				
				/*
				 * Ignore errors creating the proxy
				 */
				if (proxy == null) {
					System.err.println("[UPnP Apam Discovery] Proxy could not be instantiated  "+implementation.getName());
					continue;
				}
				
				/*
				 * Update the service map
				 */
				synchronized (discoveredDeviceProxies) {
					
					/*
					 * If the device is no longer available, just dispose the created proxy and abort processing 
					 */
					if (! discoveredDeviceProxies.containsKey(device)) {
						if (proxy.getApamComponent() != null)
							((InstanceImpl)proxy.getApamComponent()).unregister();
						return;
					}

					/*
					 * otherwise add it to the map
					 */
					discoveredDeviceProxies.get(device).add(proxy);
				}

			} catch (Exception e) {
				System.err.println("[UPnP Apam Discovery] Proxy could not instantiated  "+implementation.getName());
				e.printStackTrace();
			}
			
		}
		
	}
	
}
