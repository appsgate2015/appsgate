/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

/**
 *
 * @author jr
 */
public class NodeException extends Exception {

    NodeException(String name, String jsonParam, Exception ex) {
        super("Missing parameter [" + jsonParam + "] for " + name, ex);
    }

}
