import appsgate.ard.protocol.controller.ARDController;
import appsgate.ard.protocol.model.ARDFutureResponse;
import appsgate.ard.protocol.model.command.ARDRequest;
import appsgate.ard.protocol.model.command.request.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

@Ignore
public class ARDSuiteTest {

    private Logger logger = LoggerFactory.getLogger(ARDSuiteTest.class);

    ARDController ard;

    @Before
    public void before() throws IOException, JSONException {

        String host=System.getProperty("ard.host","192.168.1.7");
        Integer port=Integer.parseInt(System.getProperty("ard.port","2004"));

        logger.info("ARD, connecting to the host {}:{}",host,port);

        ard=new ARDController(host, port);

        //Open the socket with ard box
        ard.connect();

        //connect to listen the binary events from ardbox
        ard.monitoring();

    }

    @After
    public void after() throws IOException {
        ard.disconnect();
    }

    @Test
    public void subscriptionTest() throws JSONException, InterruptedException {
        //subscribe for JSON events (if your are not subscribed you receive only binary messages)
        ARDRequest request=new SubscriptionRequest();
        ARDFutureResponse response=ard.sendSyncRequest(request);
        logger.info("received {}",response.getResponse());
        Assert.assertTrue(response.getResponse() != null);
        Assert.assertTrue(response.getResponse().getInt("req_id")==request.getRequestId());
    }

    @Test
    public void getTimeTest() throws JSONException, InterruptedException {
        subscriptionTest();
        ARDRequest request=new GetTimeRequest();
        ARDFutureResponse response=ard.sendSyncRequest(request);
        logger.info("received {}",response.getResponse());
        Assert.assertTrue(response.getResponse() != null);
        Assert.assertTrue(response.getResponse().getInt("req_id")==request.getRequestId());
    }

    @Test
    public void multipleTimeRequest() throws JSONException, InterruptedException {
        getTimeTest();
        getTimeTest();
        getTimeTest();
    }

    @Test
    public void unknownMethodCall() throws JSONException, InterruptedException {
        subscriptionTest();
        ARDRequest request=new ARDRequest(0,"noMethod"){};
        ARDFutureResponse response=ard.sendSyncRequest(request);
        Assert.assertTrue(response.getResponse()!=null);
        Assert.assertTrue(response.getResponse().getInt("req_id")==request.getRequestId());
    }

    @Test
    public void getZoneTest() throws JSONException, InterruptedException {
        subscriptionTest();
        ARDRequest request=new GetZoneRequest(1);
        ARDFutureResponse response=ard.sendSyncRequest(request);

        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());
        Assert.assertTrue(response.getResponse().getInt("req_id")==request.getRequestId());

    }

    @Test
    public void forceInputTest() throws JSONException, InterruptedException {
        subscriptionTest();
        ARDRequest request=new ForceInputRequest(1,true,false);
        ARDFutureResponse response=ard.sendSyncRequest(request);

        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());
        Assert.assertTrue(response.getResponse().getInt("req_id")==request.getRequestId());

    }

    @Test
    public void getInputTest() throws JSONException, InterruptedException {
        subscriptionTest();
        ARDRequest request=new GetInputRequest(1);
        ARDFutureResponse response=ard.sendSyncRequest(request);

        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());
        Assert.assertTrue(response.getResponse().getInt("req_id")==request.getRequestId());

    }

    @Test
    public void getMultipleZoneTest() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        for(int x=1;x<2;x++){
            ard.sendRequest(new GetZoneRequest(5));
            ard.sendRequest(new GetZoneRequest(5));
            ard.sendRequest(new GetZoneRequest(5));
            ARDFutureResponse response=ard.sendSyncRequest(new GetZoneRequest(x));
            Assert.assertTrue(response.getResponse()!=null);
            System.out.println("Response:"+response.getResponse().toString());

        }

    }

    @Test
    public void getMultipleInputTest() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        for(int x=1;x<10;x++){
            ARDFutureResponse response=ard.sendSyncRequest(new GetInputRequest(x));
            Assert.assertTrue(response.getResponse()!=null);
            System.out.println("Response:"+response.getResponse().toString());

        }

    }

    @Test
    public void activateAlarm() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        ARDFutureResponse response=ard.sendSyncRequest(new ActivateZoneRequest(1));
        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());

        Thread.sleep(2000);

    }

    @Test
    public void fireAlarmZoneDesactivated() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        ARDFutureResponse response=ard.sendSyncRequest(new DeactivateZoneRequest(1));
        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());

        ard.sendRequest(new ForceInputRequest(4,false,true));


        Thread.sleep(2000);

    }

    @Test
    public void fireAlarmForced() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        ARDFutureResponse response=ard.sendSyncRequest(new ActivateZoneRequest(1));
        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());

        ard.sendRequest(new ForceInputRequest(4,true,false));
        ard.sendRequest(new GetTimeRequest());

        Thread.sleep(2000);

    }

    @Test
    public void fireAlarmZoneActivated() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        ARDFutureResponse response=ard.sendSyncRequest(new ActivateZoneRequest(1));
        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());

        ard.sendRequest(new ForceInputRequest(4,false,true));
        ard.sendRequest(new GetTimeRequest());

        Thread.sleep(10000);

    }

    @Test
    public void desactivateAlarm() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        ARDFutureResponse response=ard.sendSyncRequest(new DeactivateZoneRequest(1));
        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());

        Thread.sleep(2000);

    }

    @Test
    public void fetch10Elements() throws JSONException, InterruptedException, IOException {

        subscriptionTest();

        for(int index=1;index<2;index++){
            System.out.println("*** iterating index:"+index);
            try {
                JSONObject response=ard.sendSyncRequest(new GetInputRequest(index)).getResponse();
                //System.out.println("response:"+response.get);
                /*
                if(response!=null&&response.getString("name").trim().equals(name)){
                    System.out.println("name got from response:"+response.getString("name"));
                    System.out.println("name i ve been looking for:"+name);
                    System.out.println("*** found index:"+index);
                    //forceInput(index,state);
                    return;
                }
                */
            } catch (JSONException e) {
                logger.error("Failed to recover zones recorded in the HUB ARD");
            }

        }
/*
        ARDFutureResponse response=ard.sendSyncRequest(new DeactivateZoneRequest(1));
        Assert.assertTrue(response.getResponse()!=null);
        System.out.println("Response:"+response.getResponse().toString());
*/
        Thread.sleep(2000);

    }

}
