/*
 * #%L
 * OW2 Chameleon - Fuchsia Framework
 * %%
 * Copyright (C) 2009 - 2014 OW2 Chameleon
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package appsgate.ard.protocol;

import appsgate.ard.protocol.controller.ARDController;
import appsgate.ard.protocol.model.command.listener.ARDMessage;
import appsgate.ard.protocol.model.command.request.SubscriptionRequest;
import appsgate.ard.protocol.model.Constraint;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Main for testing ARD protocol integration
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, JSONException {

        ARDController ard= ARDController.getInstance("192.168.3.110", 2001);

        //Open the socket with ard box
        ard.connect();

        //connect to listen the binary events from ardbox
        ard.monitoring();

        //subscribe for JSON events (if your are not subscribed you receive only binary messages)
        ard.sendRequest(new SubscriptionRequest());

        //Listener that will be called if a valid message (message that respect the constraint defined later) is received
        ARDMessage listenerForARDMessages=new ARDMessage() {
            public void ardMessageReceived(JSONObject json) throws JSONException {
                System.out.println("Router"+json.toString());
            }
        };

        //Constraint that evaluates if a json messaged received from ARDBus should be notified for the higher layers
        Constraint constraint=new Constraint() {
            public boolean evaluate(JSONObject jsonObject) throws JSONException {
                return jsonObject.getJSONObject("event").getString("class").equals("card");
            }
        };

        //Register
        ard.getMapRouter().put(constraint,listenerForARDMessages);

        Thread.sleep(60000);

        ard.disconnect();

    }
}
