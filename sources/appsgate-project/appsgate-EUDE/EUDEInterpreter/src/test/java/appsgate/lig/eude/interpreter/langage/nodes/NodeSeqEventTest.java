/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import org.json.JSONArray;
import org.junit.Before;

/**
 *
 * @author jr
 */
public class NodeSeqEventTest extends NodeTest {

    public NodeSeqEventTest() {
    }

    @Before
    @Override
    public void setUp() {
        try {
            this.instance = new NodeSeqEvent(null, new JSONArray());
        } catch (NodeException ex) {
            System.out.println("JSON Exception: " + ex);
        }

    }

}