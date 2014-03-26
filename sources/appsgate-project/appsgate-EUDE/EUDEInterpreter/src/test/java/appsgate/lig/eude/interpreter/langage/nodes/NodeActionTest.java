package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.TestUtilities;
import java.util.Collection;
import org.jmock.Expectations;
import org.jmock.States;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jr
 */
public class NodeActionTest extends NodeTest {

    private NodeAction actionTest;

    public NodeActionTest() throws Exception {
        super();
        ruleJSON.put("type", "action");
        JSONObject o = new JSONObject();
        o.put("type", "device");
        o.put("value", "test");
        ruleJSON.put("target", o);
        ruleJSON.put("methodName", "test");
        ruleJSON.put("args", (Collection) null);
        ruleJSON.put("returnType", "");
    }

    @Before
    public void setUp() throws Exception {
        this.actionTest = new NodeAction(ruleJSON, null);
        this.instance = this.actionTest;
    }

    /**
     * Test of getResult method, of class NodeAction.
     */
    @Test
    @Override
    public void testGetResult() {
        System.out.println("getResult");
        Object expResult = null;
        Object result = this.actionTest.getResult();
        Assert.assertEquals(expResult, result);
    }

    @Test
    @Override
    public void testCall() throws Exception {
        printTestName("call");
        JSONObject expResult = null;
        JSONObject result = this.instance.call();
        Assert.assertEquals(expResult, result);
        Assert.assertFalse("Simple action can not be stopped, so once the action is done, it is stopped", this.instance.isStarted());
    }

    @Test
    public void testCallOnVariable() throws Exception {
        final States tested = context.states("NotYet");
        context.checking(new Expectations() {
            {
                allowing(mediator).executeCommand(with(any(String.class)), with(any(String.class)), with(any(JSONArray.class)));
                then(tested.is("Yes"));

            }
        });

        printTestName("Call on Variable");
        programNode.setVariable("test", new NodeValue(new JSONObject("{'type':'device', 'value':'tt'}"), programNode));
        NodeAction a = new NodeAction(TestUtilities.loadFileJSON("src/test/resources/node/actionVariable.json"), programNode);
        Assert.assertNotNull(a);
        JSONObject res = a.call();
        synchroniser.waitUntil(tested.is("Yes"), 500);
        tested.become("no");

        Assert.assertNull(res);
        programNode.setVariable("ref", new NodeValue(new JSONObject("{'type':'device', 'value':'tt'}"), programNode));
        programNode.setVariable("test", new NodeValue(new JSONObject("{'type':'variable', 'value':'ref'}"), programNode));
        res = a.call();
        Assert.assertNull(res);
        synchroniser.waitUntil(tested.is("Yes"), 500);
    }
}
