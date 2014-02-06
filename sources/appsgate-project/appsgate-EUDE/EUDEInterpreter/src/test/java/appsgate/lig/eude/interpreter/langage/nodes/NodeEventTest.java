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
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jr
 */
public class NodeEventTest extends NodeTest {

    private NodeEvent eventTest;

    public NodeEventTest() throws JSONException {
        super();
        JSONObject o = new JSONObject();
        o.put("type", "device");
        o.put("id", "test");
        ruleJSON.put("type", "event");
        ruleJSON.put("source", o);
        ruleJSON.put("eventName", "test");
        ruleJSON.put("eventValue", "test");

        context.checking(new Expectations() {
            {
                allowing(mediator).notifyChanges(with(any(ProgramStateNotificationMsg.class)));
                allowing(mediator).addNodeListening(with(any(NodeEvent.class)));
            }
        });
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.eventTest = new NodeEvent(ruleJSON, null);
        this.instance = this.eventTest;
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

}
