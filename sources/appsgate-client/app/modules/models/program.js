define([
    "app",
    "models/brick"
], function(App, Brick) {

    var Program = {};

    /**
     * Universe model class, representing a universe in AppsGate
     */
    Program = Brick.extend({
        // default values
        defaults: {
            runningState: "DEPLOYED",
            modified: true,
            userSource: "",
            name: "",
            parameters: [],
            header: {},
            definitions: [],
			      body : {},
            nodesCounter: {},
            activeNodes: {}
		},
        /**
         * Extract the name and the daemon attributes from the source to simplify their usage w/ backbone and in the templates
         *
         * @constructor
         */
        initialize: function() {
            var self = this;

            // name
            if (typeof this.get("name") === "undefined") {
                this.set("name", "FIXME");
            }

            // when the source has been updated, update the attributes of the program model
            this.on("change:source", function() {
                this.set("body", this.get("source").body);
            });

            if(typeof this.get("id") !== "undefined"){
              this.attachChangeListener(this.get("id"));
            }
        },
        /**
         * Listens to messages related to changes of this program
         */
        attachChangeListener: function (id) {
          var self = this;
          // each program listens to the event whose id corresponds to its own id
          dispatcher.on(id, function(updatedVariableJSON) {
            if(typeof updatedVariableJSON.activeNodes !== 'undefined' &&  typeof updatedVariableJSON.nodesCounter !== 'undefined'){
              self.set('activeNodes',updatedVariableJSON.activeNodes);
              self.set('nodesCounter',updatedVariableJSON.nodesCounter);
            } else {
              self.set(updatedVariableJSON.varName, updatedVariableJSON.value);
            }
          });
        },
        scheduleProgram: function(start, stop) {
          var eventName = $.i18n.t("programs.scheduled-event") + " " + this.get("name");
          this.remoteCall("scheduleProgram", [{type: "String", value: eventName },{type: "String", value: this.get("id")},{type: "boolean", value: start},{type: "boolean", value: stop}]);
        },
        /**
         * Send a message to the server to perform a remote call
         *
         * @param method Remote method name to call
         * @param args Array containing the argument taken by the method. Each entry of the array has to be { type : "", value "" }
         */
        remoteCall: function(method, args) {
            communicator.sendMessage({
                method: method,
                args: args,
                TARGET: "EHMI"
            });
        },
        // override its synchronization method to send a notification on the network
        sync: function(method, model) {
            var self = this;
            console.log(model.toJSON());
            switch (method) {
                case "create":
                    // create an id to the program
                    var id;
                    do {
                        id = "program-" + Math.round(Math.random() * 10000).toString();
                    } while (programs.where({id: id}).length > 0);
                    model.set("id", id);

                    model.attachChangeListener(id);

                    this.remoteCall("addProgram", [{type: "JSONObject", value: model.toJSON()}]);
                    break;
                case "delete":
                    this.remoteCall("removeProgram", [{type: "String", value: model.get("id")}]);
                    break;
                case "update":
                    model.remoteCall("updateProgram", [{type: "JSONObject", value: model.toJSON()}]);
                    break;
            }
        },
        /**
         * Converts the model to its JSON representation
         */
        toJSON: function() {
            return {
                id: this.get("id"),
                runningState: this.get("runningState"),
                name:this.get("name"),
                modified: this.get("modified"),
            	body: this.get("body"),
            	header: this.get("header"),
            	definitions: this.get("definitions"),
            	userSource: this.get("userSource")
            };
        }
    });
    return Program;
});
