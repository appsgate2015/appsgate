package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.TestUtilities;
import appsgate.lig.ehmi.spec.SpokObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author jr
 */
public class NodeFunctionTest extends NodeTest {

    public NodeFunctionTest() throws JSONException {
        ruleJSON.put("id", "test");
        ruleJSON.put("type", "function");
        ruleJSON.put("params", new JSONArray());
    }

    @Before
    public void setUp() throws Exception {
        this.instance = new NodeFunction(ruleJSON, programNode);
    }

    @Test
    public void testBuildNodeFromJson() throws Exception {
        NodeFunctionDefinition defNode = new NodeFunctionDefinition(TestUtilities.loadFileJSON("src/test/resources/node/testFunction.json"), null);
        assertNotNull(defNode);
        JSONArray p = new JSONArray("[{'type':'number', 'value':'40'}, {'type':'boolean', 'value':'true'}, {'type':'string', 'value':'c'}]");
        NodeFunction func = new NodeFunction("test", defNode, p);
        func.setParent(programNode);
        assertNotNull(func);
        System.out.println(func.getExpertProgramScript());
        Assert.assertEquals(null, func.call());
        NodeVariableDefinition v1 = func.getVariableByName("v1");
        Assert.assertNotNull(v1);
        System.out.println(v1);
        SpokObject result = func.getResult();
        System.out.println(result);
        Assert.assertNotNull(result);
    }

    @Test
    @Override
    public void testCall() throws Exception {
        System.out.println("call");
        JSONObject result = this.instance.call();
        // No function definition exists for "test" function
        assertNotNull(result);
        assertEquals("SpokExecutionException", result.getString("exceptionType"));
    }

    @Test
    @Override
    public void testGetSymbolTable() {
        assertNotNull(this.instance.getSymbolTable());
    }
}
