package appsgate.lig.upnp.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

import appsgate.lig.core.object.spec.CoreObjectSpec;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.impl.InstanceImpl;

/**
 * This class listen for UPnPDevice service discovery events and creates the APAM deviceMap for all hosted
 * services in the device.
 * 
 * Proxies are looked up using Apam resolver capabilities, so they may be deployed as a side effcet of a
 * discovery.
 * 
 * @author vega
 *
 */
@Component(publicFactory=false)
@Instantiate(name="AppsgateUPnPAdapter")
public class ProxyDiscovery  {		

	/**
	 * The bundle context of the discovery
	 */
	private final BundleContext context;

	public ProxyDiscovery(BundleContext context) {
		this.context = context;
	}
	
    /**
     * The event executor. We use a pool of a threads to notify APAM of underlying platform events,
     * without blocking the platform thread.
     */
    static private final Executor executor      = Executors.newCachedThreadPool();
	
	/**
	 * Reference to the APAM resolver
	 */
	@SuppressWarnings("unused")
	@Requires(id="APAM", optional=true)
	private Apam 				apam;
	private ApamResolver 		resolver;

	/**
	 * The list of events registered before binding Apam
	 */
	private List<UPnPDevice> 	pending = new ArrayList<UPnPDevice>();
	
	@Bind(id="APAM")
	@SuppressWarnings("unused")
	private synchronized void  apamBound() {
		resolver = CST.apamResolver;
		System.err.println("[UPnP Apam Discovery] Bound to APAM resolver "+resolver);
		
		/*
		 * Schedule pending device discovery  requests
		 */
		for (UPnPDevice device : pending) {
			System.err.println("[UPnP Apam Discovery] ... scheduling pending request for "+device.getDescriptions(null).get(UPnPDevice.ID));
			executor.execute(new DeviceDiscoveryRequest(device));
		}
	}
	
	@Unbind(id="APAM")
	@SuppressWarnings("unused")
	private synchronized void apamUnbound() {
		resolver = null;
		System.err.println("[UPnP Apam Discovery] Unbound to APAM resolver "+resolver);
	}
	
	/**
	 * The list of created proxies for each discovered device
	 * 
	 */
	private Map<UPnPDevice,List<ApformInstance>> deviceMap = new HashMap<UPnPDevice, List<ApformInstance>>() ;


	/**
	 * Method invoked with a new device hosting the UPnP services is discovered.
	 * 
	 * WARNING IMPORTANT Notice that this is an iPojo callback, and we should not block inside,
	 * so we process the request asynchronously.
	 */
	@Bind(id=UPnPDevice.ID,aggregate=true,optional=true)
	@SuppressWarnings("unused")
	private void boundDevice(UPnPDevice device) {
		
		/*
		 * If APAM is not available schedule the request later
		 */
		synchronized (this) {
			if (resolver == null)
				pending.add(device);
		}
		
		/*
		 * first we update synchronously the device table, so we can respect the order of events
		 * (bound/unbound) for each device
		 */
		synchronized (deviceMap) {
			deviceMap.put(device,new ArrayList<ApformInstance>());
		}
		
		executor.execute(new DeviceDiscoveryRequest(device));
	}
	
	/**
	 * Method invoked when a device is no longer available. We dispose all created deviceMap.
	 * 
	 * WARNING IMPORTANT Notice that this is an iPojo callback, and we should not block inside,
	 * so we process the request asynchronously.
	 */
	@Unbind(id=UPnPDevice.ID)
	@SuppressWarnings("unused")
	private void unboundDevice(UPnPDevice device) {
		
		/*
		 * If APAM is not available, just remove any pending request
		 */
		synchronized (this) {
			if (resolver == null)
				pending.remove(device);
		}
		
		/*
		 * first we update synchronously the device table, so we can respect the order of events
		 * (bound/unbound) for each device
		 */
		List<ApformInstance> serviceProxies = null;
		synchronized (deviceMap) {
			serviceProxies = deviceMap.remove(device);
		}

		executor.execute(new DeviceLostRequest(device,serviceProxies));
	}
	
    
    /**
     * The handler of the discovery request, It look for a proxy for each service and create the
     * appropriate instance.
	 * 
     */
    private class DeviceDiscoveryRequest implements Runnable {

    	/**
    	 * The discovered device
    	 */
    	private final UPnPDevice device;
    	
    	public DeviceDiscoveryRequest(UPnPDevice device) {
			this.device = device;
		}
    	
		@Override
		public void run() {

			String deviceId		= (String)device.getDescriptions(null).get(UPnPDevice.ID);
			String deviceType	= (String)device.getDescriptions(null).get(UPnPDevice.TYPE);
			

			/*
			 * IMPORTANT Because we are processing this event asynchronously, we need to verify that the device is
			 * still available, and abort the processing as soon as possible.
			 */
			synchronized (deviceMap) {
				if (! deviceMap.containsKey(device))
					return;
			}
			
			/*
			 * IMPORTANT Because we are processing this event asynchronously, we need to verify that APAM is
			 * still available, and abort the processing as soon as possible.
			 */
			synchronized (ProxyDiscovery.this) {
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
						synchronized (deviceMap) {
							
							/*
							 * If the device is no longer available, just dispose the created proxy and abort processing 
							 */
							if (! deviceMap.containsKey(device)) {
								if (proxy.getApamComponent() != null)
									((InstanceImpl)proxy.getApamComponent()).unregister();
								return;
							}
			
							/*
							 * otherwise add it to the map
							 */
							deviceMap.get(device).add(proxy);
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
				synchronized (deviceMap) {
					if (! deviceMap.containsKey(device))
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
					synchronized (deviceMap) {
						
						/*
						 * If the device is no longer available, just dispose the created proxy and abort processing 
						 */
						if (! deviceMap.containsKey(device)) {
							if (proxy.getApamComponent() != null)
								((InstanceImpl)proxy.getApamComponent()).unregister();
							return;
						}

						/*
						 * otherwise add it to the map
						 */
						deviceMap.get(device).add(proxy);
					}

				} catch (Exception e) {
					System.err.println("[UPnP Apam Discovery] Proxy could not instantiated  "+implementation.getName());
					e.printStackTrace();
				}
				
			}
			
		}
    	
    }
		
    /**
     * The handler of the unbound request, It dispose all the created proxies associated to the device.
	 * 
     */
    private class DeviceLostRequest implements Runnable {

    	/**
    	 * The unbound device
    	 */
    	private final UPnPDevice device;
    	
    	/**
    	 * The proxies to dispose
    	 */
    	private final List<ApformInstance> proxies;
    	
    	public DeviceLostRequest(UPnPDevice device, List<ApformInstance> proxies) {
    		this.device		= device;
			this.proxies	= proxies;
		}
    	
		@Override
		public void run() {

			String deviceId		= (String)device.getDescriptions(null).get(UPnPDevice.ID);
			String deviceType	= (String)device.getDescriptions(null).get(UPnPDevice.TYPE);
			
			System.err.println("[UPnP Apam Discovery] Device ("+deviceType+") unbound :"+deviceId);
			
			if (proxies == null)
				return;
			
			for (ApformInstance proxy : proxies) {
				if (proxy.getApamComponent() != null)
					((InstanceImpl)proxy.getApamComponent()).unregister();
			}
		}
    	
    }
		
	
}
