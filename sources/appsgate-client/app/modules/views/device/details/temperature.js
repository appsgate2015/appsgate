define([
  "app",
  "views/device/details/details",
  "text!templates/devices/details/temperature.html"
  ], function(App, DeviceDetailsView, temperatureDetailTemplate) {

    var TemperatureSensorView = {};
    // detailled view of a device
    TemperatureSensorView = DeviceDetailsView.extend({
      tplTemperature: _.template(temperatureDetailTemplate),
      initialize: function() {
        var self = this;
        TemperatureSensorView.__super__.initialize.apply(this, arguments);

        $.extend(self.__proto__.events, TemperatureSensorView.__super__.events);
      },
      autoupdate: function() {
        TemperatureSensorView.__super__.autoupdate.apply(this);

        this.$el.find("#temperature-value").html(this.model.getValue() + " &deg;C");

        // translate the view
        this.$el.i18n();
      },
      /**
      * Render the detailled view of a device
      */
      render: function() {
        var self = this;

        if (!appRouter.isModalShown) {
          this.$el.html(this.template({
            device: this.model,
            sensorImg: ["app/img/sensors/temperature_intern.png", "app/img/sensors/temperature_extern.png"],
            sensorCaption: [$.i18n.t("devices.temperature.caption.intern"), $.i18n.t("devices.temperature.caption.extern")],
            sensorType: $.i18n.t("devices.temperature.name.singular"),
            places: places,
            deviceDetails: this.tplTemperature
          }));
        }

        this.resize($(".scrollable"));

        // translate the view
        this.$el.i18n();

        return this;
      }
    });
    return TemperatureSensorView
  });
