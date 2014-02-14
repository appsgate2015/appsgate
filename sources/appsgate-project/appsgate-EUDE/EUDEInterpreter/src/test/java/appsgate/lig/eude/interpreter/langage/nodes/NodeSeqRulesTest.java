/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.impl.TestUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jr
 */
public class NodeSeqRulesTest extends NodeTest {

    public NodeSeqRulesTest() throws JSONException {
        ruleJSON.put("type", "instructions");
        ruleJSON.put("rules", new JSONArray());
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new NodeSeqRules(this.ruleJSON, null);
    }

    @Test
    public void testBuildFromJson() throws Exception {
        NodeSeqRules seq = new NodeSeqRules(TestUtilities.loadFileJSON("src/test/resources/node/seqRules.json"), null);
        Assert.assertNotNull(seq);
        System.out.println(seq.getExpertProgramScript());
        seq.call();
    }

    @Test
    @Override
    public void testCall() throws Exception {
        printTestName("call");
        JSONObject result = this.instance.call();
        Assert.assertNotNull(result);
        junit.framework.Assert.assertTrue("supposed to be started", this.instance.isStarted());

    }
}