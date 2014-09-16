define([
    "app",
    "models/service/service",
    "text!templates/program/nodes/defaultActionNode.html",
    "models/service/mediaplayer",
    "models/service/mediabrowser",
    "models/service/mail",
    "models/service/weather"
], function(App, Service, ActionTemplate, MediaPlayer, MediaBrowser, Mail, Weather) {

    var Services = {};

    // collection
    Services = Backbone.Collection.extend({
        model: Service,
        templates: {'action' : {}, 'event' : {}, 'state': {}, 'property' : {}},
        /**
         * Fetch the devices from the server
         *
         * @constructor
         */
        initialize: function() {
            var self = this;

            // listen to the event when the list of devices is received
            dispatcher.on("listDevices", function(devices) {
                _.each(devices, function(service) {
                    if (service) {
                        self.addService(service);
                    }
                });
                dispatcher.trigger("servicesReady");
            });
            // listen to the backend notifying when a device appears and add it
            dispatcher.on("newService", function(service) {
                self.addService(service);
            });

            dispatcher.on("removeService", function(serviceId) {
                var service = self.findWhere({id: serviceId});
                self.remove(service);

                console.log(service);
            });

        },
        /**
         * Check the type of device sent by the server, cast it and add it to the collection
         *
         * @param device
         */
        addService: function(brick) {
            var self = this;
            brick.type = parseInt(brick.type);
            var service = null;
            switch (brick.type) {
                case 31:
                    service = new MediaPlayer(brick);
                    break;
                case 102:
                    service = new Mail(brick);
                    break;
                case 103:
                    service = new Weather(brick);
                    break;
                default:
                    console.log("unknown type", brick.type, brick);
                    break;
            }
            if (service != null) {
                self.add(service);
                self.templates['action'][brick.type] = service.getTemplateAction();
                self.templates['event'][brick.type] = service.getTemplateEvent();
                self.templates['state'][brick.type] = service.getTemplateState();
                self.templates['property'][brick.type] = service.getTemplateProperty();
            }
        },
        /**
         * @return Array of the devices of a given type
         */
        getServicesByType: function() {
            return this.groupBy(function(service) {
                return service.get("type");
            });
        },
        /**
         * @return Core mail of the home - unique device
         */
        getCoreMail: function() {
            return services.findWhere({type: 102});
        },
        /**
         * @return Core weather of the home - unique device
         */
        getCoreWeather: function() {
            return services.findWhere({type: 103});
        },
        /**
         * @return Array of UPnP media players
         */
        getMediaPlayers: function() {
            return services.where({type: 31});
        },
        /**
         * @return Array of UPnP media browsers
         */
        getMediaBrowsers: function() {
            return services.where({type: 36});
        },
        /**
         * @returns the template corresponding to the device
         */
        getTemplateByType: function(word,type,param) {
            if (this.templates[word][type]) {
                return this.templates[word][type](param);
            } else {
                console.warn("No template is defined for type: " + type);
            }
            return _.template(ActionTemplate)(param);
        },
    });

    return Services;

});
