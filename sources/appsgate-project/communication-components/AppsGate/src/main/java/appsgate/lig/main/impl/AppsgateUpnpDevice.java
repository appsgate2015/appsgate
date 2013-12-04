package appsgate.lig.main.impl;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.control.QueryListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.context.device.name.table.spec.DeviceNameTableSpec;
import appsgate.lig.context.userbase.spec.UserBaseSpec;
import appsgate.lig.eude.interpreter.spec.EUDE_InterpreterSpec;
import appsgate.lig.main.spec.AppsGateSpec;
import appsgate.lig.manager.place.spec.PlaceManagerSpec;
import appsgate.lig.router.spec.RouterApAMSpec;

/**
 * This class is the central component for AppsGate server. It allow client part
 * to make methods call from HMI managers.
 * 
 * It expose Appsgate server as an UPnP device to gather informations about it
 * through the SSDP discovery protocol
 * 
 * @author Cédric Gérard
 * @since April 23, 2013
 * @version 1.0.0
 * 
 */
public class AppsgateUpnpDevice extends Device implements ActionListener,
		QueryListener {

	/**
	 * 
	 * static class logger member
	 */
	private static Logger logger = LoggerFactory.getLogger(AppsgateUpnpDevice.class);


	/**
	 * UPnP device description xml file relative path.
	 */
	private static String descriptionFileName = "/conf/device/description.xml";

	/**
	 * default web socket connection port
	 */
	private static String wsPort = "8087";


	/**
	 * Default constructor for Appsgate java object. it load UPnP device and
	 * services profiles and subscribes the corresponding listeners.
	 * 
	 * @throws InvalidDescriptionException
	 */
	public AppsgateUpnpDevice() throws InvalidDescriptionException {
		super(System.getProperty("user.dir") + "/" + descriptionFileName);

		// Set UPnP action listening
		Action action = getAction("getIP");
		action.setActionListener(this);

		action = getAction("getURL");
		action.setActionListener(this);

		action = getAction("getWebsocket");
		action.setActionListener(this);

		// initiate UPnP state variables
		try {
			StateVariable stateVar;
			StateVariable serverIP;
			// server IP initialization
			stateVar = getStateVariable("serverIP");

			Inet4Address localAddress = (Inet4Address) InetAddress
					.getLocalHost();
			Enumeration<NetworkInterface> nets = NetworkInterface
					.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				if (!netint.isLoopback() && !netint.isVirtual()
						&& netint.isUp()) { // TODO check also if its the local
											// network. but It will difficult to
											// find automatically the right
											// network interface
					Enumeration<InetAddress> addresses = netint
							.getInetAddresses();
					for (InetAddress address : Collections.list(addresses)) {
						if (address instanceof Inet4Address) {
							localAddress = (Inet4Address) address;
							break;
						}
					}
				}
			}
			stateVar.setValue(localAddress.getHostAddress());
			stateVar.setQueryListener(this);
			serverIP = stateVar;

			// server access URL initialization
			stateVar = getStateVariable("serverURL");
			stateVar.setValue("http://" + serverIP.getValue() + "/index.html");
			stateVar.setQueryListener(this);

			// server web socket connection entry variable initialization
			stateVar = getStateVariable("serverWebsocket");
			stateVar.setValue("http://" + serverIP.getValue() + ":" + wsPort
					+ "/");
			stateVar.setQueryListener(this);

		} catch (UnknownHostException e) {
			logger.debug("Unknown host: ");
			e.printStackTrace();
		} catch (SocketException e) {
			logger.debug("Socket exception for UPnP: ");
			e.printStackTrace();
		}

		logger.info("AppsGate UpNP device instanciated");
	}


	/**
	 * Method call when an UPnP state variable is modify
	 */
	@Override
	public boolean queryControlReceived(StateVariable stateVar) {
		String varName = stateVar.getName();
		if (varName.contentEquals("serverIP")) {

			// stateVar.setValue(currTimeStr);
			return true;
		}

		// stateVar.setStatus(UPnP::INVALID_VAR, “.....”);
		return false;
	}

	/**
	 * Method call when an UPnP action is triggered
	 */
	@Override
	public boolean actionControlReceived(Action action) {
		ArgumentList argList = action.getArgumentList();
		String actionName = action.getName();

		if (actionName.contentEquals("getIP")) {
			Argument out_serverIP = argList.getArgument("serverIP");
			out_serverIP.setValue(getStateVariable("serverIP").getValue());
			return true;

		} else if (actionName.contentEquals("getURL")) {
			Argument out_serverURL = argList.getArgument("serverURL");
			out_serverURL.setValue(getStateVariable("serverURL").getValue());
			return true;
		} else if (actionName.contentEquals("getWebsocket")) {
			Argument out_serverWS = argList.getArgument("serverWebsocket");
			out_serverWS.setValue(getStateVariable("serverWebsocket")
					.getValue());
			return true;
		}

		action.setStatus(401, "invalid action");
		return false;
	}
}
