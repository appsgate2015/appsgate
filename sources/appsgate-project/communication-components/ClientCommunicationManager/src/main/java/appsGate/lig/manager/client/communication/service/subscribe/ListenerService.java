package appsGate.lig.manager.client.communication.service.subscribe;

/**
 * This interface is the specification of service for subscribe notification, message
 * and command response to client application.
 * 
 * @author Cédric Gérard
 * @since February 8, 2013
 * @version 1.0.0
 *
 */
public interface ListenerService {
	
	/**
	 * This method allow all the caller to subscribe for all commands and events except configuration
	 * command.
	 * @param cmdListener the listener for subscription
	 * @param the target corresponding to the listener
	 * @return  true if the command listener is new, false if it has been replaced
	 */
	public boolean addCommandListener(CommandListener cmdListener, String target);
	
	/**
	 * Remove the command listener associated to a target
	 * @param target the target from witch remove the command listener
	 * @return true if the command listener has been removed, false otherwise
	 */
	public boolean removeCommandListener(String target);
	
	/**
	 * This method allow the caller to subscribe for all configuration command.
	 * @param target the target identifier
	 * @param configListener the listener for subscription
	 * @return true if the configuration listener is new, false if it has been replaced
	 */
	public boolean addConfigListener(String target, ConfigListener configListener);
	
	/**
	 * This method allow the caller to unsubscribe the specify target for all configuration command.
	 * @param target the target identifier
	 * @return true if the listener is unregistered, false otherwise
	 */
	public boolean removeConfigListener(String target);

}
