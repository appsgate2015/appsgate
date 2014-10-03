define([
    "app",
    "text!templates/services/list/servicesListByCategory.html",
], function(App, serviceListByCategoryTemplate) {

    var ServiceByTypeView = {};
    /**
     * Render the list of services of a given type
     */
    ServiceByTypeView = Backbone.View.extend({
        tpl: _.template(serviceListByCategoryTemplate),
        events: {
            "click button.delete-meteo-button": "onDeleteMeteoButton",
            "keyup #add-weather-modal input": "validWeatherName",
            "click #add-weather-modal button.valid-button": "addWeatherName"
        },
        /**
         * Listen to the updates on the services of the category and refresh if any
         *
         * @constructor
         */
        initialize: function() {
            var self = this;

            services.getServicesByType()[this.id].forEach(function(service) {
                self.listenTo(service, "change", self.render);
                self.listenTo(service, "remove", self.render);
            });
            dispatcher.on("checkLocation", function(l) {
                if (l!= undefined && l.locality1 != undefined) {
                    $("#add-weather-modal input[name='name']").val(l.locality1);
                    $("#add-weather-modal input[name='country']").val(l.country);
                    $("#add-weather-modal .valid-button").removeClass("disabled");
                    $("#add-weather-modal .valid-button").removeClass("valid-disabled");
                } else {
                    $("#add-weather-modal .valid-button").addClass("disabled");
                    $("#add-weather-modal .valid-button").addClass("valid-disabled");
                }
            });
        },
        /**
         * Render the list
         */
        render: function() {
            if (!appRouter.isModalShown) {
                this.$el.html(this.tpl({
                    type: this.id,
                    places: places
                }));
                this.$(".delete-popover").popover({
                    html: true,
                    content: "<button type='button' class='btn btn-danger delete-meteo-button'>" + $.i18n.t("form.delete-button") + "</button>",
                    placement: "bottom"
                });


                // translate the view
                this.$el.i18n();

                // resize the list
                this.resize($(".scrollable"));

                return this;
            }
            return this;
        },
        /**
         *
         */
        validWeatherName: function(e) {
            var loc = $("#add-weather-modal input[name='inputValue']").val();
            if (loc != undefined && loc.length > 2) {
                //code
                communicator.sendMessage({
                    "method":"checkLocation",
                    "args":[{"type":"String","value":loc}],
                    "callId":"checkLocation",
                    "TARGET":"EHMI"
                    });
            } else {
                $("#add-weather-modal .valid-button").addClass("disabled");
                $("#add-weather-modal .valid-button").addClass("valid-disabled");
            }
        },

        addWeatherName: function() {
            var loc = $("#add-weather-modal input[name='name']").val();
            console.log(loc);
            communicator.sendMessage({
                method: "addLocationObserver",
                args: [{type:"String", value:loc}],
                TARGET: "EHMI",
                id:"addLocationObserver"
                }
            );
        },
        /**
         *
         */
        onDeleteMeteoButton: function(e) {
            var actuator = services.get($(e.currentTarget).parents(".pull-right").children(".delete-popover").attr("id"));
            communicator.sendMessage({
                method: "removeLocationObserver",
                args: [{type:"String", value:actuator.attributes.location}],
                TARGET: "EHMI",
                id:"removeLocationObserver"
            });
        }
        
    });
    return ServiceByTypeView;
});
