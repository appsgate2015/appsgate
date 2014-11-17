define([
  "app",
  "models/device/device",
  "text!templates/program/nodes/ardActionNode.html"
], function(App, Device,ActionTemplate) {

  var ARDLock = {};
  /**
   * Implementation of a Contact sensor
   * @class Device.ContactSensor
   */
  ARDLock = Device.extend({
    /**
     * @constructor
     */
    initialize: function() {
      ARDLock.__super__.initialize.apply(this, arguments);
        var self = this;
      // setting default friendly name if none exists
      if (typeof this.get("name") === "undefined" || this.get("name") === "") {
            this.generateDefaultName($.i18n.t("devices.ard.name.singular"));
      }

      this.remoteControl("getZonesAvailable", [], "zonesavailable");

      dispatcher.on('zonesavailable', function(zones) {
            console.log("zones available received: "+JSON.stringify(zones,4,null));
            self.set("zones",zones);
      });

    },
    getEvents: function() {
          return ["isAuthorized","isNotAuthorized"];
      },
    getStates: function(which) {
          switch (which) {
              case "state":
                  return []; //"getLastCard"
              default:
                  return [];
          }
      },
    getActions: function() {
          return ["zoneActivate", "zoneDesactivate"];
      },
      getKeyboardForAction: function(act){
          var btn = jQuery.parseHTML("<button class='btn btn-default btn-keyboard specific-node' ></button>");
          var v = this.getJSONAction("mandatory");

          switch(act) {
              case "zoneActivate":
                  $(btn).append("<span data-i18n='devices.ard.keyboard.zone-activate'/>");
                  v.methodName = "zoneActivate";
                  v.phrase = "devices.ard.action.zone-activate";
                  v.args = [ {"type":"int", "value": "0"}];
                  $(btn).attr("json", JSON.stringify(v));
                  break;
              case "zoneDesactivate":
                  $(btn).append("<span data-i18n='devices.ard.keyboard.zone-desactivate'/>");
                  v.methodName = "zoneDesactivate";
                  v.phrase = "devices.ard.action.zone-desactivate";
                  v.args = [ {"type":"int", "value": "0"}];
                  $(btn).attr("json", JSON.stringify(v));
                  break;
              default:
                  console.error("unexpected action found for ARD: " + act);
                  btn = null;
                  break;
          }
          return btn;
      },
    getKeyboardForState: function(state, which){
      if (which !== "state") {
          console.error('Unsupported type of state: ' + which);
          return null;
      }
      var btn = jQuery.parseHTML("<button class='btn btn-default btn-keyboard specific-node' ></button>");
      var v = this.getJSONState("mandatory");
      switch(state) {
          case "getLastCard":
              $(btn).append("<span data-i18n='devices.ard.state.authorized'/>");
              v.phrase = "devices.ard.state.opened";
              v.name = "getLastCard";
              $(btn).attr("json", JSON.stringify(v));
              break;
          default:
              console.error("unexpected state found for ARD Lock: " + state);
              btn = null;
              break;
      }
      return btn;
    },
    getKeyboardForEvent: function(evt){
          var btn = jQuery.parseHTML("<button class='btn btn-default btn-keyboard specific-node' ></button>");
          var v = this.getJSONEvent("mandatory");
          switch(evt) {
              case "isAuthorized":
                  $(btn).append("<span data-i18n='devices.ard.event.authorized'/>");
                  v.eventName = "authorized";
                  v.eventValue = "true";
                  v.phrase = "devices.ard.state.authorized";
                  $(btn).attr("json", JSON.stringify(v));
                  break;
              case "isNotAuthorized":
                  $(btn).append("<span data-i18n='devices.ard.event.non_authorized'/>");
                  v.eventName = "authorized";
                  v.eventValue = "false";
                  v.phrase = "devices.ard.state.non_authorized";
                  $(btn).attr("json", JSON.stringify(v));
                  break;
              default:
                  console.error("unexpected event found for Contact Sensor: " + evt);
                  btn = null;
                  break;
          }
          return btn;
      },
    getTemplateAction: function() {
      return _.template(ActionTemplate);
    },
    getTemplateParameter: function(){

      console.log("Actual zones:"+JSON.stringify(this.get("zones"),4,null));
      return {zones:this.get("zones")};//{zones:[{'zone_idx':1,'zone_name':"exterieur"}]};

    }

  });
  return ARDLock;
});
