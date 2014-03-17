/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.TestUtilities;
import appsgate.lig.eude.interpreter.langage.components.SpokObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jr
 */
public class NodeListsTest extends NodeTest {

    public NodeListsTest() throws JSONException {
        ruleJSON.put("type", "lists");
        ruleJSON.put("left", new JSONObject("{'type':'device', 'id':'a'}"));
        ruleJSON.put("right", new JSONObject("{'type':'device', 'id':'b'}"));
        ruleJSON.put("operator", "U");
    }

    @Before
    public void setUp() throws Exception {
        this.instance = new NodeLists(ruleJSON, programNode);
    }

    @Test
    public void testIntersection() throws Exception {
        NodeLists inter = new NodeLists(TestUtilities.loadFileJSON("src/test/resources/node/intersection.json"), null);
        Assert.assertNotNull(inter);
        JSONObject call = inter.call();
        Assert.assertNull(call);
        SpokObject result = inter.getResult();
        Assert.assertNotNull(result);
        System.out.println(result);
        List<NodeValue> elements = inter.getElements();
        Assert.assertNotNull(elements);
        Assert.assertEquals("", 1, elements.size());

    }
    @Test
    public void testUnion() throws Exception {
        JSONObject json = TestUtilities.loadFileJSON("src/test/resources/node/intersection.json");
        json.put("operator", "U");
        NodeLists inter = new NodeLists(json, null);
        Assert.assertNotNull(inter);
        JSONObject call = inter.call();
        Assert.assertNull(call);
        SpokObject result = inter.getResult();
        Assert.assertNotNull(result);
        System.out.println(result);
        List<NodeValue> elements = inter.getElements();
        Assert.assertNotNull(elements);
        Assert.assertEquals("", 3, elements.size());

    }
    @Test
    public void testNotin() throws Exception {
        JSONObject json = TestUtilities.loadFileJSON("src/test/resources/node/intersection.json");
        json.put("operator", "N");
        NodeLists inter = new NodeLists(json, null);
        Assert.assertNotNull(inter);
        JSONObject call = inter.call();
        Assert.assertNull(call);
        SpokObject result = inter.getResult();
        Assert.assertNotNull(result);
        System.out.println(result);
        List<NodeValue> elements = inter.getElements();
        Assert.assertNotNull(elements);
        Assert.assertEquals("", 1, elements.size());

    }
}
