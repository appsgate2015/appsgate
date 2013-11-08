/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.ProgramStateNotificationMsg;
import org.jmock.Expectations;
import static org.jmock.Expectations.any;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jr
 */
public class NodeEventTest extends NodeTest {

    private NodeEvent eventTest;

    public NodeEventTest() {
        context.checking(new Expectations() {
            {
                allowing(interpreter).notifyChanges(with(any(ProgramStateNotificationMsg.class)));
                allowing(interpreter).addNodeListening(with(any(NodeEvent.class)));
            }
        });
    }

    @Before
    @Override
    public void setUp() {
        super.setUp();

        try {

            ruleJSON.put("sourceType", "test");
            ruleJSON.put("sourceId", "test");
            ruleJSON.put("eventName", "test");
            ruleJSON.put("eventValue", "test");
            this.eventTest = new NodeEvent(interpreter, ruleJSON);
            this.instance = this.eventTest;
        } catch (JSONException ex) {
            System.out.println("JSON Ex : " + ex.getMessage());
        }
    }

    /**
     * Test of getSourceId method, of class NodeEvent.
     */
    @Test
    public void testGetSourceId() {
        System.out.println("getSourceId");
        String expResult = "test";
        String result = this.eventTest.getSourceId();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEventName method, of class NodeEvent.
     */
    @Test
    public void testGetEventName() {
        System.out.println("getEventName");
        String expResult = "test";
        String result = this.eventTest.getEventName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEventValue method, of class NodeEvent.
     */
    @Test
    public void testGetEventValue() {
        System.out.println("getEventValue");
        String expResult = "test";
        String result = this.eventTest.getEventValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of coreEventFired method, of class NodeEvent.
     */
    @Test
    public void testCoreEventFired() {
        System.out.println("coreEventFired");
        this.eventTest.coreEventFired();
    }

    /**
     * Test of undeploy method, of class NodeAction.
     */
    @Test
    @Override
    public void testUndeploy() {
        System.out.println("undeploy : not implemented yet");
    }

    /**
     * Test of resume method, of class NodeAction.
     */
    @Test
    @Override
    public void testResume() {
        System.out.println("resume : not implemented yet");
    }

    /**
     * Test of getState method, of class NodeAction.
     */
    @Test
    @Override
    public void testGetState() {
        System.out.println("getState : not implemented yet");
    }

    /**
     * Test of startEventFired method, of class NodeProgram.
     */
    @Test
    @Override
    public void testStartEventFired() {
        System.out.println("startEventFired : NOT implemented YET");
    }

}
