/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.ClockProxy;
import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import org.jmock.Expectations;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author jr
 */
public class NodeEventsAndTest extends NodeTest {

    public NodeEventsAndTest() throws Exception {
        JSONObject desc = new JSONObject();
        desc.put("id", "12");
        final ClockProxy appsGate = new ClockProxy(desc);

        context.checking(new Expectations() {
            {
                allowing(mediator).getClock();
                will(returnValue(appsGate));
                allowing(mediator).addNodeListening(with(any(NodeEvent.class)));
                allowing(mediator).removeNodeListening(with(any(NodeEvent.class)));
                allowing(mediator).getTime();
                will(returnValue(new Long(2000)));
            }
        });
        ruleJSON.put("type", "eventsAnd");
        ruleJSON.put("events", new JSONArray());
        ruleJSON.put("duration", 0);

    }

    @Before
    public void setUp() throws Exception {
        this.instance = new NodeEventsAnd(this.ruleJSON, null);
    }

    @Test
    public void testGetManyEvents() throws Exception {
        this.printTestName("GetManyEvents");
        NodeEventTest t = new NodeEventTest();
        t.setUp();
        NodeEvent nodeEvent = (NodeEvent) t.instance;

        JSONObject json = new JSONObject();
        json.put("type", "events");
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(nodeEvent.getJSONDescription());
        json.put("events", jsonArray);
        json.put("nbEventToOccur", 2);
        json.put("duration", 0);
        NodeEvents n = new NodeEventsOr(json, programNode);

        n.call();
        n.endEventFired(new EndEvent(nodeEvent));
        Assert.assertTrue("Node should be started", n.isStarted());
        n.endEventFired(new EndEvent(nodeEvent));
        Assert.assertFalse("Node should be stopped", n.isStarted());
    }

}
