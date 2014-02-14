package appsgate.lig.eude.interpreter.langage.nodes;

import java.util.Collection;
import junit.framework.Assert;
import org.json.JSONObject;
import org.junit.Before;
import static org.junit.Assert.*;
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
        o.put("id", "test");
        ruleJSON.put("target", o);
        ruleJSON.put("methodName", "test");
        ruleJSON.put("args", (Collection) null);
        ruleJSON.put("returnType", "");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

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
        assertEquals(expResult, result);
    }

    @Test
    @Override
        public void testCall() throws Exception {
        printTestName("call");
        JSONObject expResult = null;
        JSONObject result = this.instance.call();
        assertEquals(expResult, result);
        Assert.assertFalse("Simple action can not be stopped, so once the action is done, it is stopped", this.instance.isStarted());

        }
}
