package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.langage.exceptions.SpokNodeException;
import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import java.util.Collection;
import junit.framework.Assert;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jr
 */
public class NodeRelationBoolTest extends NodeTest {

    private NodeRelationBool relationTest;

    public NodeRelationBoolTest() {
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        JSONObject op = new JSONObject();
        op.put("targetId", "p0");
        op.put("returnType", "boolean");
        op.put("targetType", "program");
        op.put("methodName", "test");
        op.put("args", (Collection) null);
        ruleJSON.put("operator", "test");
        ruleJSON.put("leftOperand", op);
        ruleJSON.put("rightOperand", op);
        this.relationTest = new NodeRelationBool(ruleJSON, null);
        this.instance = this.relationTest;

    }

    /**
     * Test of getBooleanResult method, of class NodeRelationBool.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    public void testGetResult() throws Exception {
        System.out.println("getResult");
        try {
            Boolean result = this.relationTest.getBooleanResult();
            Assert.fail("An exception is supposed to have been raised, instead a result has been returned: " + result);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Test of endEventFired method, of class NodeProgram.
     */
    @Test
    @Override
    public void testEndEventFired() {
        System.out.println("endEventFired");
        NodeAction ac;
        try {
            JSONObject action = new JSONObject();
            try {
                action.put("targetType", "test");
                action.put("targetId", "test");
                action.put("methodName", "test");
                action.put("args", (Collection) null);
            } catch (JSONException ex) {
                System.out.println("JsonEx");
            }

            ac = new NodeAction(action, null);
            EndEvent e = new EndEvent(ac);
            this.relationTest.endEventFired(e);
        } catch (SpokNodeException ex) {
            System.out.println("NodeException ex: " + ex.getMessage());
            Assert.fail("unable to create a NodeAction for test");
        }
    }

}
