package appsgate.ard.protocol.adaptor;

import appsgate.ard.protocol.controller.ARDController;
import appsgate.ard.protocol.model.Constraint;
import appsgate.ard.protocol.model.command.listener.ARDMessage;
import appsgate.ard.protocol.model.command.request.*;
import appsgate.lig.ard.badge.door.messages.ARDBadgeDoorContactNotificationMsg;
import appsgate.lig.ard.badge.door.spec.CoreARDBadgeDoorSpec;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.core.object.spec.CoreObjectBehavior;
import appsgate.lig.core.object.spec.CoreObjectSpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ARDBadgeDoor extends CoreObjectBehavior implements ARDMessage, CoreObjectSpec, CoreARDBadgeDoorSpec { //ARDWatchDogSpec

    private static final Integer ARD_MAX_INDEX_FETCH=10;
    private static Logger logger = LoggerFactory.getLogger(ARDController.ARD_LOGNAME);
    private String sensorName;
    private String sensorId;
    private String sensorType;
    private String isPaired;
    private String signal;
    private String currentStatus;
    private String userType;
    /**
     * 0 = Off line or out of range
     * 1 = In validation mode (test range for sensor for instance)
     * 2 = In line or connected
     */
    private String status;
    private String pictureId;
    private Integer doorID=-1;
    private Integer lastCard=-1;
    private Boolean authorized=false;
    private String ardClass="";
    private String lastMessage="";
    private JSONArray zonesCache=null;
    private JSONArray inputsCache=null;

    private ARDController controller;

    public String getAbstractObjectId() {
        return sensorId;
    }

    public String getUserType() {
        return userType;
    }

    public int getObjectStatus() {
        return 0;
    }

    public String getPictureId() {
        return pictureId;
    }

    public JSONObject getDescription() throws JSONException {

        JSONObject descr = new JSONObject();

        descr.put("id", sensorId);
        descr.put("type", userType);
        descr.put("status", status);
        descr.put("contact", currentStatus);
        descr.put("deviceType", sensorType);
        descr.put("lastCard", lastCard);
        descr.put("authorized", authorized);
        descr.put("lastMessage", lastMessage);

        /*
        JSONObject zone1 = new JSONObject();
        JSONObject zone2 = new JSONObject();
        zone1.put("zone_idx",1);
        zone1.put("zone_name","exterieur");
        zone2.put("zone_idx",2);
        zone2.put("zone_name","interieur");

        descr.append("zones", zone1);
        descr.append("zones", zone2);
        */
        fillUpZones(descr);
        fillUpInputs(descr);
        return descr;
    }

    public void setPictureId(String pictureId) {

    }

    public CORE_TYPE getCoreType() {
        return CORE_TYPE.DEVICE;
    }

    public void newInst() {
        logger.info("New contact sensor detected, "+sensorId);
        controller.getMapRouter().put(new Constraint() {
                                          @Override
                                          public boolean evaluate(JSONObject jsonObject) throws JSONException {
                                              try {
                                                  Boolean alarm=jsonObject.getJSONObject("event").getBoolean("alarm");
                                                  Boolean active=jsonObject.getJSONObject("event").getBoolean("active");
                                                  return alarm && active;

                                              } catch (JSONException e){
                                                  return false;
                                              }
                                          }
                                      },
                new ARDMessage() {
                    @Override
                    public void ardMessageReceived(JSONObject json) throws JSONException {
                        triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("alarmFired", "false", "true", ARDBadgeDoor.this));
                    }
                }
        );
    }

    public void deleteInst() {
        logger.info("Contact sensor disappeared, " + sensorId);
    }

    @Override
    public boolean getContactStatus() {
        return false;
    }

    @Override
    public Integer getLastCard() {
        return lastCard;
    }

    @Override
    public String getARDClass() {
        return null;
    }

    @Override
    public Integer getDoorID() {
        return doorID;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getLastMessage() {
        return lastMessage;
    }

    @Override
    public void zoneActivate(int zone) {
        logger.info("zoneActivate invoked: Zone to be activated {},", zone);
        try {
            JSONObject result=controller.sendSyncRequest(new ActivateZoneRequest(zone)).getResponse();
            logger.info("Response: {}",result);
        } catch (JSONException e) {
            logger.error("Failed invoking zoneActivate for zone {}", zone);
        }
    }

    @Override
    public void forceInput(int input,boolean value) {
        logger.info("forceInput invoked: Input to be forced {} with the value {}", value);
        try {
            JSONObject request1=controller.sendSyncRequest(new ForceInputRequest(input,value,false)).getResponse();
            logger.info("Response: {}",request1);
        } catch (JSONException e) {
            logger.error("Failed invoking Force Input for zone {}",input);
        }
    }

    @Override
    public void zoneDesactivate(int zone) {
        logger.info("zoneDesactivate invoked: Zone to be desactivated {},", zone);
        try {
            JSONObject result=controller.sendSyncRequest(new DeactivateZoneRequest(zone)).getResponse();
            logger.info("Response: {}",result);
        } catch (JSONException e) {
            logger.error("Failed invoking zoneDesactivate for zone {}",zone);
        }
    }

    public NotificationMsg triggerApamMessage(ARDBadgeDoorContactNotificationMsg apamMessage){
        logger.info("Forwarding ARDMessage as ApamMessage, {}:{})", apamMessage.getVarName(), apamMessage.getNewValue());
        return apamMessage;
    }

    private void fillUpZones(final JSONObject descr){

        if(zonesCache==null){
            zonesCache=new JSONArray();
            for(int index=1;index<ARD_MAX_INDEX_FETCH;index++){
                try {
                    JSONObject response=controller.sendSyncRequest(new GetZoneRequest(index)).getResponse();

                    if(response!=null&&!response.getString("name").trim().equals("")){
                        String zoneName=response.getString("name");
                        JSONObject zone = new JSONObject();
                        zone.put("zone_idx",index);
                        zone.put("zone_name",zoneName);
                        zonesCache.put(zone);
                        //descr.append("zones",zone);
                    }
                } catch (JSONException e) {
                    logger.error("Failed to recover zones recorded in the HUB ARD");
                }
            }
        }

        try {
            descr.put("zones",zonesCache);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void syncAppsgateContactWithARDInput(String name,Boolean state){
        logger.trace("Looking for a ARD input with name {} to set status to {}",name,state);
        for(int index=1;index<ARD_MAX_INDEX_FETCH;index++){
            logger.trace("Checking input in the index {}", index);
            try {
                JSONObject response=controller.sendSyncRequest(new GetInputRequest(index)).getResponse();

                if(response!=null&&response.getString("name").trim().equals(name)){
                    logger.trace("Found input with name {} configuring its state to {}", name, state);
                    forceInput(index,state);
                    return;
                }
            } catch (JSONException e) {
                logger.error("Failed to recover zones recorded in the HUB ARD");
            }

        }
    }

    private void fillUpInputs(final JSONObject descr){

        if(inputsCache==null){
            inputsCache=new JSONArray();
            for(int index=1;index<ARD_MAX_INDEX_FETCH;index++){
                try {
                    JSONObject response=controller.sendSyncRequest(new GetInputRequest(index)).getResponse();

                    if(response!=null&&!response.getString("name").trim().equals("")){
                        String inputName=response.getString("name");
                        JSONObject input = new JSONObject();
                        input.put("input_idx", index);
                        input.put("input_name", inputName);
                        inputsCache.put(input);
                    }
                } catch (JSONException e) {
                    logger.error("Failed to recover zones recorded in the HUB ARD");
                }

            }
        }
        try {
            descr.put("inputs",inputsCache);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public JSONObject getInputsAvailable(){
        JSONObject descr = new JSONObject();
        fillUpInputs(descr);
        return descr;

    }

    public JSONObject getZonesAvailable(){

        JSONObject descr = new JSONObject();

        fillUpZones(descr);


        return descr;

        /* Example of zone
            try {
                zone1.put("zone_idx",1);
                zone1.put("zone_name","exterieur");
                zone2.put("zone_idx",2);
                zone2.put("zone_name","interieur");
                descr.append("zones", zone1);
                descr.append("zones", zone2);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        */
    }

    public void ardMessageReceived(JSONObject json)  {

        try {

            JSONObject eventNode=json.getJSONObject("event");

            Integer newDoorID=eventNode.getInt("door_idx");
            String newArdClass=eventNode.getString("class");

            triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("door_idx",doorID.toString(),newDoorID.toString(),this));
            triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("ardClass",ardClass,newArdClass,this));

            doorID=newDoorID;
            ardClass=newArdClass;

            Integer newCard=-1;
            try {
                newCard=eventNode.getInt("card_idx");
            }catch(JSONException e){
                logger.warn("No ID CARD received.",e);
            }finally {
                triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("lastCard",lastCard.toString(),newCard.toString(),this));
                triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("card_idx",lastCard.toString(),newCard.toString(),this));
                lastCard=newCard;
            }

            String newLastMessage="";
            try {
                newLastMessage=eventNode.getString("cause");
            }catch(JSONException e){
                logger.warn("No cause received.",e);
            }finally {
                triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("lastMessage",lastMessage.toString(),newLastMessage.toString(),this));
                lastMessage=newLastMessage;
            }

            Boolean newAuthorized=Boolean.FALSE;
            try {
                newAuthorized=eventNode.getString("status").equalsIgnoreCase("ok")?true:false;
            }catch(JSONException e){
                logger.warn("No status received.",e);
            } finally {
                triggerApamMessage(new ARDBadgeDoorContactNotificationMsg("authorized",authorized.toString(),newAuthorized.toString(),this));
                authorized=newAuthorized;
            }

        }catch(JSONException e){
            logger.error("Failed parsing ARD JSON message.",e);
        }

    }

    public void apamMessageReceived(NotificationMsg mesg){

            if(mesg.getVarName().equals("contact")){
                syncAppsgateContactWithARDInput(mesg.getSource().getAbstractObjectId(),!Boolean.parseBoolean(mesg.getNewValue()));
                logger.debug("Apam Message received var name {} old value {} new value {}", mesg.getVarName(), mesg.getOldValue(),mesg.getNewValue());
            }

    }

}
