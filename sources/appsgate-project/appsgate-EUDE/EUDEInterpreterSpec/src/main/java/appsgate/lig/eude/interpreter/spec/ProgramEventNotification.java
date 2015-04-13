/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package appsgate.lig.eude.interpreter.spec;

/**
 *
 * @author jr
 */
public class ProgramEventNotification extends ProgramTraceNotification{

    
    /**
     * Constructor
     * @param source
     * @param nodeId
     * @param deviceId
     * @param desc 
     * @param value 
     */
    public ProgramEventNotification(ProgramDesc source, String nodeId, String deviceId, String desc, String value) {
        super(source, nodeId, deviceId, source.getId(), desc, Type.WRITE);
        setResult(value);
    }
    
    @Override
    public String getDeviceId() {
        return this.getSourceId();
    }
    
}
