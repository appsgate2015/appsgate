package appsgate.ard.protocol.adaptor;

import appsgate.ard.protocol.controller.ARDController;
import appsgate.ard.protocol.model.command.listener.ARDMessage;
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

    private static Logger logger = LoggerFactory.getLogger(ARDBadgeDoor.class);
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

        JSONObject zone1 = new JSONObject();
        JSONObject zone2 = new JSONObject();
        zone1.put("zone_idx",1);
        zone1.put("zone_name","exterieur");
        zone2.put("zone_idx",2);
        zone2.put("zone_name","interieur");

        descr.append("zones", zone1);
        descr.append("zones", zone2);

        return descr;
    }

    public void setPictureId(String pictureId) {

    }

    public CORE_TYPE getCoreType() {
        return CORE_TYPE.DEVICE;
    }

    public void newInst() {
        logger.info("New contact sensor detected, "+sensorId);
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
        logger.warn("zoneActivate: This function is not implemented yet. Zone to be activated {},",zone);
    }

    @Override
    public void zoneDesactivate(int zone) {
        logger.debug("zoneDesactivate: This function is not implemented yet. Zone to be activated {},",zone);
    }

    public NotificationMsg triggerApamMessage(ARDBadgeDoorContactNotificationMsg apamMessage){
        logger.info("Forwarding ARDMessage as ApamMessage, {}:{})",apamMessage.getVarName(),apamMessage.getNewValue());
        return apamMessage;
    }

    public JSONObject getZonesAvailable(){

        JSONObject descr = new JSONObject();
        JSONObject zone1 = new JSONObject();
        JSONObject zone2 = new JSONObject();

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

        return descr;
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

}
