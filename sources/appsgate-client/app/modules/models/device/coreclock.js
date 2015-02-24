define([
  "app",
  "models/device/device",
  "text!templates/program/nodes/clockEventNode.html",
  "text!templates/program/nodes/clockPropertyNode.html"
], function(App, Device, EventTemplate, ClockPropertyTemplate) {

  var CoreClock = {};

  /**
   * Implementation of the Core Clock
   *
   * @class Device.CoreClock
   */
  CoreClock = Device.extend({
    /**
     * @constructor
     */
    initialize: function() {
      var self = this;

      CoreClock.__super__.initialize.apply(this, arguments);
      // setting default friendly name if none exists
      if (this.get("name") === "" ||  this.get("name") == undefined) {
        this.set("name", $.i18n.t("devices.clock.name.singular"));
      }

      self.set("reset", false);
      self.set("hidden", true);

      // when the flow rate changes, update the interval that controls the local time
      this.on("change:flowRate", function() {
        //clearInterval(this.intervalLocalClockValue);
        var localthis = this;
        var time = (new Date()).getTime();
        clearTimeout(localthis.timeout);
        var fctCB = function() {
          self.updateClockValue();
          var time = ((new Date()).getTime() - self.anchorSysTime) * self.get("flowRate"); // Temps écoulé en terme de l'horloge par rapport à son ancre AnchorTimeSys
          var dt = (Math.floor((time + 60000) / 60000) * 60000 - time) / self.get("flowRate");
          localthis.timeout = setTimeout(fctCB, dt + 5);
        };
        this.timeout = setTimeout(fctCB, (Math.floor((time + 60000) / 60000) * 60000 - time + 5) / self.get("flowRate"));
      });
      // when the ClockSet changes, resynchornize with the server
      this.on("change:ClockSet", function() {
        self.synchronizeCoreClock();
      });
      this.on("change:resetClock", function() {
        self.resetClock();
      });
      dispatcher.on("updateClockClientSide", function(timePassed) {
        self.set("moment", self.get("moment").add('milliseconds', timePassed * self.get("flowRate")));
        self.updateClockDisplay();
      });
      // synchronize the core clock with the server every 10 minutes
      dispatcher.on("systemCurrentTime", function(timeInMillis) {
        self.set("moment", moment(parseInt(timeInMillis)));
        self.anchorSysTime = (new Date()).getTime();
        self.anchorTime = parseInt(timeInMillis);
        self.updateClockDisplay();
      });
      dispatcher.on("systemCurrentFlowRate", function(flowRate) {
        self.set("flowRate", flowRate);
      });
      self.synchronizeCoreClock();
      self.synchronizeFlowRate();

      // bind the method to this model to avoid this keyword pointing to the window object for the callback on setInterval
      this.synchronizeCoreClock = _.bind(this.synchronizeCoreClock, this);
      this.intervalClockValue = setInterval(this.synchronizeCoreClock, 600000);
      this.intervalClockValueLocal = setInterval(this.updateClockClientSide, 1000);

      // update the local time every minute
      this.updateClockValue = _.bind(this.updateClockValue, this);
    },
    updateClockClientSide: function() {
      dispatcher.trigger("updateClockClientSide", 1000);
    },
    /**
     * Callback to update the clock value - increase the local time of one minute
     */
    updateClockValue: function() {
      if (this.anchorSysTime) {
        var delta_ms = ((new Date()).getTime() - this.anchorSysTime) * parseInt(this.get("flowRate"));
        var ms = this.anchorTime + delta_ms;
        this.set("moment", moment(ms));
        this.updateClockDisplay();
      }
    },

  /**
     * Updates clock display values from internal moment
     */
    updateClockDisplay: function() {
      this.set("year", this.get("moment").year().toString(), {
        silent: true
      });
      this.set("month", this.get("moment").month().toString(), {
        silent: true
      });
      this.set("day", this.get("moment").day().toString(), {
        silent: true
      });
      this.set("hour", this.get("moment").hour().toString(), {
        silent: true
      });
      if (this.get("hour").length === 1) {
        this.set("hour", "0" + this.get("hour"), {
          silent: true
        });
      }
      this.set("minute", this.get("moment").minute().toString(), {
        clockRefresh: true
      });
      if (this.get("minute").length === 1) {
        this.set("minute", "0" + this.get("minute"), {
          clockRefresh: true
        });
      }
      this.set("second", this.get("moment").second().toString(), {
        clockRefresh: true
      });
      if (this.get("second").length === 1) {
        this.set("second", "0" + this.get("second"), {
          clockRefresh: true
        });
      }
    },
    /**
     * Send a request synchronization with the core clock of the system
     */
    synchronizeCoreClock: function() {
      this.remoteCall("getCurrentTimeInMillis", [], "systemCurrentTime");
    },
    synchronizeFlowRate: function() {
      this.remoteCall("getTimeFlowRate", [], "systemCurrentFlowRate");
    },
    /**
     * Remove the automatic synchronization with the server
     */
    unsynchronize: function() {
      clearInterval(this.intervalClockValue);
      clearInterval(this.intervalLocalClockValue);
      clearInterval(this.intervalClockValueLocal);
    },

    getProperties: function() {
      return ["checkCurrentTimeOfDay"];
    },
    getKeyboardForProperty: function(property) {
      var btn = jQuery.parseHTML("<button class='btn btn-default btn-keyboard specific-node' group-id='" + this.get("type") + "'></button>");
      var v ={
        "type": "property",
        "iid": "X",
        "target": {
          "iid": "X",
          'type': 'device',
          'value': this.get("id"),
          "deviceType": this.get("type")
        },
        "args": []
      };

      switch (property) {
        case "checkCurrentTimeOfDay":
          $(btn).append("<span>" + $.i18n.t('devices.clock.keyboard.checkCurrentTimeOfDay', {
            myVar: "<span class='highlight-placeholder'>" + $.i18n.t('devices.clock.keyboard.before') + "</span>",
            myVar2: "<span class='highlight-placeholder'>" + $.i18n.t('devices.clock.keyboard.after') + "</span>"
          }));

          v.methodName = "checkCurrentTimeOfDay";
          v.phrase = "devices.clock.language.checkCurrentTimeOfDay.between";
            v.args = [{
            "type": "long",
            "value": 11*60*60*1000
          }, {
            "type": "long",
            "value": 12*60*60*1000
          }];
          v.returnType = "boolean";

          $(btn).attr("json", JSON.stringify(v));
          break;
        default:
          console.error("unexpected device property found for clock: " + property);
          btn = null;
          break;
      }
      return btn;
    },
    /**
     * @returns event template for clock
     */
    getTemplateProperty: function() {
      return _.template(ClockPropertyTemplate);
    },




      /**
     * return the list of available events
     */
    getEvents: function() {
      return ["ClockAlarm"];
    },
    /**
     * return the keyboard code for a given event
     */
    getKeyboardForEvent: function(evt) {
      var btn = jQuery.parseHTML("<button class='btn btn-default btn-keyboard specific-node' group-id='" + this.get("type") + "'></button>");
      switch (evt) {
        case "ClockAlarm":
          $(btn).append("<span>" + $.i18n.t('keyboard.clock-event', {
            myVar: "<span class='highlight-placeholder'>" + $.i18n.t('keyboard.time-placeholder') + "</span>",
          }));
          o = {
            'type': 'event',
            'eventName': 'ClockAlarm',
            'source': {
              'type': 'device',
              'value': this.get("id"),
              'iid': 'X',
              'deviceType': this.get("type")
            },
            'eventValue': this.getClockAlarm(11, 0),
            'iid': 'X',
            'phrase': "language.clock-event"
          };
          $(btn).attr("json", JSON.stringify(o));
          break;
        default:
          console.error("unexpected event found for Clock: " + evt);
          btn = null;
          break;
      }
      return btn;
    },
    getClockAlarm: function(hour, minute) {
      var time = this.get("moment").clone();
      time.set("hour", hour);
      time.set("minute", minute);
      time.set("second", 0);
      return time.valueOf().toString();
    },
    /**
     * @returns event template for clock
     */
    getTemplateEvent: function() {
      return _.template(EventTemplate);
    },
    /**
     * Send a message to the backend the core clock time
     */
    sendTimeInMillis: function() {
      this.remoteControl("setCurrentTimeInMillis", [{
        type: "long",
        value: this.get("moment").valueOf()
      }]);
    },
    resetClock: function() {
      this.remoteControl("resetSystemTime", []);
    },
    /**
     * Send a message to the backend the core clock flow rate
     */
    sendTimeFlowRate: function() {
      this.remoteControl("setTimeFlowRate", [{
        type: "double",
        value: this.get("flowRate")
      }]);
    }
  });
  return CoreClock;
});
