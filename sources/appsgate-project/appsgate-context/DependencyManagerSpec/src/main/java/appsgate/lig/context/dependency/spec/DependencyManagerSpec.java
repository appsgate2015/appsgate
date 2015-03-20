package appsgate.lig.context.dependency.spec;

import appsgate.lig.ehmi.spec.SpokObject;

/**
 *
 * @author jr
 */
public interface DependencyManagerSpec {

    /**
     *
     * @return the current graph of dependencies
     */
    public SpokObject getGraph();

    /**
     *
     * @param pid the program id
     * @return
     */
    public Dependencies getDependencies(String pid);

    /**
     *
     */
    public void buildGraph();

    /**
     *
     * @param srcId
     * @param varName
     * @param value
     */
    public void updateDeviceStatus(String srcId, String varName, String value);

    /**
     *
     * @param deviceId
     */
    public void updateProgramStatus(String deviceId);

    /**
     *
     * @param id the id of the entity from which we get the dependencies
     * @param timestamp the date when to have the dependencies
     * @return the dependencies, null if does not exists
     */
    public Dependencies getDependenciesAt(String id, Long timestamp);

}
