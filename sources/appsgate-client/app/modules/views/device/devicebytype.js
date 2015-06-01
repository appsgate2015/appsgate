define([
  "app",
  "text!templates/devices/list/deviceListByCategory.html"
  ], function(App, deviceListByCategoryTemplate) {

    var DeviceByTypeView = {};
    /**
    * Render the list of devices of a given type
    */
    DeviceByTypeView = Backbone.View.extend({
      tpl: _.template(deviceListByCategoryTemplate),
      events: {
        "click button.toggle-plug-button": "onTogglePlugButton",
        "click button.blink-lamp-button": "onBlinkLampButton",
        "click button.toggle-lamp-button": "onToggleLampButton",
        "click button.toggle-actuator-button": "onToggleActuatorButton",
        "click button.group-on-button": "onGroupOnButton",
        "click button.group-off-button": "onGroupOffButton"
      },
      /**
      * Listen to the updates on the devices of the category and refresh if any
      *
      * @constructor
      */
      initialize: function() {
        var self = this;
        self.listenTo(devices, "add", self.reload);
        devices.getDevicesByType(this.id).forEach(function(device) {
          if(device.get("type") != 21) {
            self.listenTo(device, "change", self.autoupdate);
            self.listenTo(device, "remove", self.render);
          }
        });
      },
      /**
      * Callback to switch a group of actuators on
      * @param e JS mouse event
      */
      onGroupOnButton: function(e) {
        devices.getDevicesByType(this.id).forEach(function(device) {
          device.remoteControl("on", []);
        });
      },
      /**
      * Callback to switch a group of actuators off
      * @param e JS mouse event
      */
      onGroupOffButton: function(e) {
        devices.getDevicesByType(this.id).forEach(function(device) {
          device.remoteControl("off", []);
        });
      },
      /**
      * Callback to toggle a plug
      *
      * @param e JS mouse event
      */
      onTogglePlugButton: function(e) {
        e.preventDefault();

        var plug = devices.get($(e.currentTarget).attr("device-id"));

        if (plug.get("plugState") === "true" || plug.get("plugState") === true) {
          plug.switchOff();
        } else {
          plug.switchOn();
        }

        return false;
      },
      /**
      * Callback to toggle a lamp
      *
      * @param e JS mouse event
      */
      onToggleLampButton: function(e) {
        e.preventDefault();

        var lamp = devices.get($(e.currentTarget).attr("device-id"));

        if (lamp.get("state") === "true" || lamp.get("state") === true) {
          lamp.switchOff();
        } else {
          lamp.switchOn();
        }

        return false;
      },
      /**
      * Callback to blink a lamp
      *
      * @param e JS mouse event
      */
      onBlinkLampButton: function(e) {
        e.preventDefault();
        var lamp = devices.get($(e.currentTarget).attr("device-id"));
        // send the message to the backend
        lamp.remoteControl("blink30", []);

        return false;
      },
      /**
      * Callback to toggle an actuator
      *
      * @param e JS mouse event
      */
      onToggleActuatorButton: function(e) {
        e.preventDefault();

        var actuator = devices.get($(e.currentTarget).attr("device-id"));

        if (actuator.get("value") === "true" || actuator.get("value") === true) {
          actuator.switchOff();
        } else {
          actuator.switchOn();
        }

        return false;
      },
      autoupdate: function(device) {
        var type = device.get("type");
        if($("#"+type).find(".list-group-item #"+device.cid).length > 0){
        var place = places.get(device.get("placeId"));

        this.$el.find("#place-name-span").html(place.getName() !== "" ?place.getName() : $.i18n.t(places-details.place-no-name));

          switch (type) {
          case "0":
            $("#device-" + device.cid + "-value").text(Math.round(device.get("value")) + "°C");
            break;
          case "1":
            $("#device-" + device.cid + "-value").attr("data-i18n", "devices.illumination.scale." + s.get("label"));
            break;
          case "2":
            if (device.get("buttonStatus") === "true") {
              $("#device-" + device.cid + "-value").attr("class","label label-yellow");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.switch.value.opened");
            } else {
              $("#device-" + device.cid + "-value").attr("class","label label-default");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.switch.value.closed");
            }
            break;
          case "3":
            if (device.get("contact") !== "true") {
              $("#device-" + device.cid + "-value").attr("class","label label-yellow");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.contact.value.opened");
            } else {
              $("#device-" + device.cid + "-value").attr("class","label label-default");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.contact.value.closed");
            }
            break;
          case "4":
            if (device.get("inserted") === "true") {
              $("#device-" + device.cid + "-value").attr("class","label label-yellow");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.cardswitch.value.inserted");
            } else {
              $("#device-" + device.cid + "-value").attr("class","label label-default");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.cardswitch.value.not-inserted");
            }
            break;
          case "6":
            if (device.get("plugState") === "true" || device.get("plugState") === true) {
              $("#device-" + device.cid + "-button").attr("data-i18n", "devices.plug.action.turnOff");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.plug.status.turnedOn");
              $("#device-" + device.cid + "-value").attr("class","label label-yellow");
            } else {
              $("#device-" + device.cid + "-button").attr("data-i18n", "devices.plug.action.turnOn");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.plug.status.turnedOff");
              $("#device-" + device.cid + "-value").attr("class","label label-default");
            }
            $("#device-" + device.cid + "-consumption").text(device.get("consumption") + " W");

            break;
          case "7":
            if (device.get("state") === "true" || device.get("state") === true) {
              $("#device-" + device.cid + "-button").attr("data-i18n", "devices.lamp.action.turnOff");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.lamp.status.turnedOn");
              $("#device-" + device.cid + "-value").attr("class", "label label-yellow");
              $("#device-" + device.cid + "-color-information").attr("data-i18n", "devices.lamp.color-information.currentColor");
            } else {
              $("#device-" + device.cid + "-button").attr("data-i18n", "devices.lamp.action.turnOn");
              $("#device-" + device.cid + "-value").attr("data-i18n", "devices.lamp.status.turnedOff");
              $("#device-" + device.cid + "-value").attr("class", "label label-default");
              $("#device-" + device.cid + "-color-information").attr("data-i18n", "devices.lamp.color-information.lastColor");
            }
            $("#device-" + device.cid + "-color").attr("style", "background-color:" + device.getCurrentColor());

            break;
            case "8":
                if (device.get("value") === "true" || device.get("value") === true) {
                    $("#device-" + device.cid + "-button").attr("data-i18n", "devices.actuator.action.turnOff");
                    $("#device-" + device.cid + "-value").attr("data-i18n", "devices.actuator.status.turnedOn");
                    $("#device-" + device.cid + "-value").attr("class","label label-yellow");
                } else {
                    $("#device-" + device.cid + "-button").attr("data-i18n", "devices.actuator.action.turnOn");
                    $("#device-" + device.cid + "-value").attr("data-i18n", "devices.actuator.status.turnedOff");
                    $("#device-" + device.cid + "-value").attr("class","label label-default");
                }
                break;
            case "31": //Media Player : 31


                break;
            case "32":
                $("#device-" + device.cid + "-value").text(device.get("value"));
                break;
            case "124": //CoreTV : 124


                break;

            case "210":
            var activeFace = "";
            switch (device.get("activeFace")) {
              case "1":
                activeFace = "<img id='device-" + device.cid + "-value' src='app/img/domicube-work.svg' width='18px' class='img-responsive'>";
                break;
              case "2":
					activeFace = "<img id='device-" + device.cid + "-value' src='app/img/domicube-white.svg' width='18px' class='img-responsive'>";
                break;
              case "3":
                activeFace = "<img id='device-" + device.cid + "-value' src='app/img/domicube-music.png' width='18px' class='img-responsive'>";
                break;
              case "4":
                activeFace = "<img id='device-" + device.cid + "-value' src='app/img/domicube-question.svg' width='18px' class='img-responsive'>";
                break;
              case "5":
                activeFace = "<img id='device-" + device.cid + "-value' src='app/img/domicube-night.png' width='18px' class='img-responsive'>";
                break;
              case "6":
                activeFace = "<img id='device-" + device.cid + "-value' src='app/img/domicube-meal.png' width='18px' class='img-responsive'>";
                break;
              default:
                break;
            }
            this.$el.find("#device-" + device.cid + "-value").replaceWith(activeFace);
            break;
		  	case "CoreFairyLightsSpec":
			this.buildFairylightWidget(device);
		  	break;
          }
          if (device.get("status") === "0") {
            $("#device-" + device.cid + "-status").attr("class","label label-danger");
            $("#device-" + device.cid + "-status").attr("data-i18n", "devices.status.disconnected");
          } else if (device.get("status" === "1")) {
            $("#device-" + device.cid + "-status").attr("class","label label-warning");
            $("#device-" + device.cid + "-status").attr("data-i18n", "devices.status.waiting");
          } else {
            $("#device-" + device.cid + "-status").attr("class","label label-success");
            $("#device-" + device.cid + "-status").attr("data-i18n", "devices.status.connected");
          }

          this.updateGroupOnOff(type);

          // translate the view
          this.$el.i18n();
        } else {
          this.render();
        }
      },
      reload: function() {
        var self = this;
        devices.getDevicesByType(this.id).forEach(function(device) {
          self.listenTo(device, "change", self.autoupdate);
          self.listenTo(device, "remove", self.render);
        });

        this.render();
      },
      /**
       * Enable or disable the buttons toggle all on / toggle all off
       * depending on the type
       * @param type
       */
      updateGroupOnOff: function(type) {
        var allOn = true;
        var allOff = true;
        var state = "value";

        switch (type) {
          case "6": state = "plugState";
            break;
          case "7": state = "state";
            break;
        }

        devices.getDevicesByType(this.id).forEach(function(device) {
          if (device.get(state) === "true" || device.get(state) === true) {
            allOff = false;
          } else if (device.get(state) === "false" || device.get(state) === false) {
            allOn = false;
          }
        });

        $(".group-on-button").prop("disabled", allOn);
        $(".group-off-button").prop("disabled", allOff);

      },
		
		/**
		* Method to build the widget of fairylights
		*/
		buildFairylightWidget: function (device) {
			var self = this;

			var widthDiv = $("#div-fairylights-widget").width();
			var height = 25;
			
			var svg = d3.select("#div-fairylights-widget").select("svg")
				.attr("width", widthDiv)
				.attr("height", height);

			var nbCircle = 25;
			var spacement = 8;
			var circleWidthDefault = 18;
			var circleWidthAvailable = widthDiv / (nbCircle + spacement);
			var circleWidthFinal = (circleWidthAvailable < circleWidthDefault) ? circleWidthAvailable : circleWidthDefault;

			var arrayLed = device.get("leds");
			if (!Array.isArray(arrayLed)) {
				arrayLed = $.parseJSON(arrayLed);
			}

			nodesLED = svg.selectAll(".nodeLed")
				.data(arrayLed);

			nodesLED.enter()
				.append("circle")
				.attr("class", "nodeLed")
				.attr("cx", function (n) {
					var index = _.indexOf(arrayLed, n);
					return (spacement / 2) + (circleWidthFinal / 2) + ((spacement / 2) + circleWidthFinal) * index;
				})
				.attr("cy", height / 2)
				.attr("r", circleWidthFinal / 2);

			nodesLED.each(function (led) {
				d3.select(this)
					.attr("fill", led.color);
			});
		},
		
      /**
       * Using this function to get additionnal details about a device in other views
       *
       * @param type
       * @param places
       * @returns {{}}
       */
      createTemplate: function(type) {
        var temp = {};
        temp = this.tpl({
          type: type,
          places: places
        });
        return temp;
      },

      /**
      * Render the list
      */
      render: function() {
        
        if (!appRouter.isModalShown) {
          this.$el.html(
              this.createTemplate(this.id)
          );
			
		  // In fairylight case, need to build special widget
		  if (this.id === "CoreFairyLightsSpec") {
			this.buildFairylightWidget(devices.getDevicesByType(this.id)[0]);
		  }

          this.updateGroupOnOff(this.id);

          // translate the view
          this.$el.i18n();

          // resize the list
          this.resize($(".scrollable"));

          return this;
        }
        return null;
      }
    });
    return DeviceByTypeView;
  });
