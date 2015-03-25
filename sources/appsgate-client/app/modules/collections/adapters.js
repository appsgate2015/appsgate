define([
    "app",
    "models/adapter/adapter",
    "models/adapter/enocean",
    "models/adapter/weather"

], function(App, Adapter, EnoceanAdapter, WeatherAdapter) {

    var Adapters = {};

    // collection
    Adapters = Backbone.Collection.extend({
        model: Adapter,
        /**
         * Fetch the devices from the server
         *
         * @constructor
         */
        initialize: function() {
            var self = this;

            this.comparator = function(device) {
                if (device.get("name")) {
                    return device.get("name").toUpperCase();
                }
                return "";
            };

            // listen to the event when the list of devices is received
            dispatcher.on("listDevices", function(devices) {
                _.each(devices, function(device) {
                    if (device && device.coreType === 'ADAPTER') {
                        self.addAdapter(device);
                    }
                });
                dispatcher.trigger("adaptersReady");
            });



            // listen to the backend notifying when a device appears and add it
            dispatcher.on("newAdapter", function(device) {
                if (device && device.coreType === 'ADAPTER') {
                    self.addAdapter(device);
                }
                self.addAdapter(adapter);
            });

            dispatcher.on("removeAdapter", function(adapter) {
              var adapterModel = adapters.findWhere({id: adapter.objectId});
              self.removeAdapter(adapterModel);
            });

            dispatcher.trigger("adaptersCollectionWaiting");


        },


        addAdapter: function(brick) {
            var self = this;
            var adapter = null;
            switch (brick.type) {
                case "UbikitAdapterService":
                    console.log("found an EnOcean(Ubikit) Adapter")
                    adapter = new EnoceanAdapter(brick);
                    break;
                case "WeatherAdapterSpec":
                    console.log("found a Weather Adapter")
                    adapter = new WeatherAdapter(brick);
                    break;
                default:
                    console.log("unknown type of ADAPTER : ", brick.type, brick);
                    break;
            }
            if (adapter != null) {
                self.add(adapter);
            }
        },


        removeAdapter: function(adapter) {

          adapters.remove(adapter);
        },


        getTypes: function() {

            var types=[];

            alavailabletypes=this.groupBy(function(adapter) {
                return adapter.get("type");
            });

            _.each(alavailabletypes,function(box){
                types[types.length]=box[0].get("type");
            });

            sortedTypes= _.sortBy(types,
                function(type){
                    var i18=this.getTypeLabelPrefix(type);
                    return $.i18n.t(i18+"singular").toLowerCase();
                },this);
            return sortedTypes;
        },

        getTypeLabelPrefix:function(type){
            var i18;
            if (type == "1001") {
                i18="devices.temperature.name.";
            }
            return i18;
        },

        getEnoceanAdapter: function() {
            return adapters.findWhere({type: "1001"});
        },
        getWeatherAdapter: function() {
            return adapters.findWhere({type: "WeatherAdapterSpec"});
        }
    });

    return Adapters;

});
