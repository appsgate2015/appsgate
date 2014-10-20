define([
  "app",
  "modules/mediator",
  "text!templates/program/reader/reader.html"
  ], function(App, Mediator, programEditorTemplate) {

    var ProgramReaderView = {};
    /**
    * Render the editor view
    */
    ProgramReaderView = Backbone.View.extend({
      tplEditor: _.template(programEditorTemplate),
      events: {
        "shown.bs.modal #schedule-program-modal": "initializeModal",
        "hidden.bs.modal #schedule-program-modal": "toggleModalValue",
        "click #schedule-program-modal button.valid-button": "validScheduleProgram",
        "click button.start-program-button": "onStartProgramButton",
        "click button.stop-program-button": "onStopProgramButton",
        "click button.cancel-edit-program-button": "onCancelEditProgram",
        "click button.launch-edit-program-button": "onLaunchEditProgram",
        "click button.edit-popover-button": "onClickEditProgram",
        "click button.delete-program-button": "onDeleteProgramButton",
        "click button.delete-popover-button": "onClickDeleteProgram",
        "click button.cancel-delete-program-button": "onCancelDeleteProgram",
        "click button.open-calendar-button":"openCalendar",
      },
      /**
      * @constructor
      */
      initialize: function() {
        this.Mediator = new Mediator();
        if(typeof this.model !== "undefined"){
          this.Mediator.loadProgramJSON(this.model.get("body"));
          this.listenTo(this.model, "change", this.refreshDisplay);
        }
        this.Mediator.readonly = true;

        this.listenTo(devices, "change", this.refreshDisplay);
        this.listenTo(services, "change", this.refreshDisplay);
        this.listenTo(dispatcher, "refreshDisplay", this.refreshDisplay);
      },
      /**
       * Clear the input text, hide the error message, check the checkbox and disable the valid button by default
       */
      initializeModal: function() {
          // tell the router that there is a modal
          appRouter.isModalShown = true;
      },
      /**
       * Tell the router there is no modal anymore
       */
      toggleModalValue: function() {
          appRouter.isModalShown = false;
      },
      /**
       * Check if the name of the program does not already exist. If not, create the program
       * Hide the modal when done
       *
       * @param e JS event
       */
      validScheduleProgram: function(e) {
          var self = this;

          // hide the modal
          $("#schedule-program-modal").modal("hide");

          if($("input[name='schedule-program']:checked").val() == 'activate') {
            this.model.scheduleProgram(true,false);
          } else if ($("input[name='schedule-program']:checked").val() == 'deactivate') {
            this.model.scheduleProgram(false,true);
          } else {
            this.model.scheduleProgram(true,true);
          }

          // instantiate the program and add it to the collection after the modal has been hidden
          $("#schedule-program-modal").on("hidden.bs.modal", function() {
            // tell the router there is no modal any more
            appRouter.isModalShown = false;

            window.open("https://www.google.com/calendar");

          });
      },
      openCalendar: function(e) {
          window.open("https://www.google.com/calendar");
      },
      /**
      * Callback to start a program
      *
      * @param e JS mouse event
      */
      onStartProgramButton: function(e) {
        e.preventDefault();

        // get the program to start
        var program = programs.get($(e.currentTarget).attr("id"));

        program.set("runningState", "PROCESSING");
        program.remoteCall("callProgram", [{type: "String", value: program.get("id")}]);

        // refresh the menu
        this.render();

        return false;
      },
      /**
      * Callback to stop a program
      *
      * @param e JS mouse event
      */
      onStopProgramButton: function(e) {
        e.preventDefault();

        // get the program to stop
        var program = programs.get($(e.currentTarget).attr("id"));

        program.set("runningState", "DEPLOYED");
        program.remoteCall("stopProgram", [{type: "String", value: program.get("id")}]);
        // refresh the menu
        this.render();

        return false;
      },
      /**
      * Callback when the user has clicked on the button to cancel the deleting. Return to the program
      */
      onCancelDeleteProgram : function() {
        // destroy the popover
        this.$el.find("#delete-program-popover").popover('destroy');
      },
      /**
      * Callback when the user has clicked on the button to remove a program. Remove the program
      */
      onDeleteProgramButton: function() {
        // delete the program
        this.model.destroy();

        // navigate to the list of programs
        appRouter.navigate("#programs", {trigger: true});
      },
      /**
      * Callback when the user has clicked on the button delete.
      */
      onClickDeleteProgram : function(e) {
        var self = this;
        // create the popover
        this.$el.find("#delete-program-popover").popover({
            html: true,
            title: $.i18n.t("programs.warning-program-delete"),
            content: "<div class='popover-div'><button type='button' class='btn btn-default cancel-delete-program-button'>" + $.i18n.t("form.cancel-button") + "</button><button type='button' class='btn btn-danger delete-program-button'>" + $.i18n.t("form.delete-button") + "</button></div>",
            placement: "bottom"
        });
        // listen the hide event to destroy the popup, because it is created to every click on Edit
        this.$el.find("#delete-program-popover").on('hidden.bs.popover', function () {
            self.onCancelDeleteProgram();
        });
        // show the popup
        this.$el.find("#delete-program-popover").popover('show');
      },
      /**
      * Callback when the user has clicked on the button to cancel the launching of edition. Return to the program
      */
      onCancelEditProgram : function() {
        // destroy the popover
        this.$el.find("#edit-program-popover").popover('destroy');
      },
      /**
      * Callback when the user has clicked on the button edit and confirm it. Go to the editor
      */
      onLaunchEditProgram : function() {
        // navigate to the editor
        appRouter.navigate("#programs/editor/" + this.model.get('id'), {trigger: true});
      },
      /**
      * Callback when the user has clicked on the button edit. Go to the editor or show the popup
      */
      onClickEditProgram : function(e) {
        var self = this;
        // if program waiting, show the popup warning
        if (this.model.get('runningState') === "WAITING") {
            // create the popover
            this.$el.find("#edit-program-popover").popover({
                html: true,
                title: $.i18n.t("programs.warning-program-edition"),
                content: "<div class='popover-div'><button type='button' class='btn btn-default cancel-edit-program-button'>" + $.i18n.t("form.cancel-button") + "</button><button type='button' class='btn btn-primary launch-edit-program-button'>" + $.i18n.t("programs.edit-program") + "</button></div>",
                placement: "bottom"
            });
            // listen the hide event to destroy the popup, because create to every click on Edit
            this.$el.find("#edit-program-popover").on('hidden.bs.popover', function () {
                self.onCancelEditProgram();
            });
            // show the popup
            this.$el.find("#edit-program-popover").popover('show');
        } else {
            // else go to edit directly
            this.onLaunchEditProgram();
        }
      },
      refreshDisplay: function(e) {
        // To avoid to refresh the whole page at each second
        if (e!== undefined && e.updateClockValue !== undefined) {
          return;
        }
        var input = this.Mediator.getInputFromJSON();
        if (!this.Mediator.isValid) {
          this.model.set("runningState", "INVALID");
        }
        var self = this;
        _.defer(function() {
          input = self.applyReadMode(input);
          $(".programInput").html(input).addClass("read-only");
          $(".input-spot:not(.mandatory-spot)").remove();
          $(".mandatory-spot").text($.i18n.t("language.mandatory-readonly"));
          $(".rules-node").find(".separator").remove();

          var test = $(".while-keep-then").parent().next();

          if($(".while-keep-then").parent().next().hasClass("secondary-block-node")) {
            $(".while-keep-then").remove();
          }
          $(".secondary-block-node").remove();

          if($(".programInput").children(".seq-block-node").children().length <= 1){
            $(".programInput").children(".seq-block-node").remove();
            $(".programInput").children(".separator").remove();
          }
          if($(".programInput").children(".set-block-node").children().length <= 1){
            $(".programInput").children(".set-block-node").remove();
            $(".programInput").children(".separator").remove();
          }

          if(typeof self.model !== "undefined"){
            if (self.model.get("runningState") === "PROCESSING" || self.model.get("runningState") === "KEEPING" || self.model.get("runningState") === "WAITING") {
              $("#led-" + self.model.get("id")).addClass("led-yellow").removeClass("led-orange").removeClass("led-default");
              $(".start-program-button").hide();
              $(".stop-program-button").show();
            } else if (self.model.get("runningState") === "INVALID"){
              $("#led-" + self.model.get("id")).addClass("led-orange").removeClass("led-yellow").removeClass("led-default");
              $(".start-program-button").show();
              $(".start-program-button").prop('disabled', true);
              $(".stop-program-button").hide();
            } else{
              $("#led-" + self.model.get("id")).addClass("led-default").removeClass("led-yellow").removeClass("led-orange");
              $(".start-program-button").show();
              $(".stop-program-button").hide();
            }
          }
          $("body").i18n();

          // progress indicators should be updated at the end as they are sensitive to the sizes and positions of elements
          self.updateProgressIndicators();

        });

      },
      applyReadMode: function(input) {
        // setting selects in read mode
        $(input).find("select").replaceWith(function() {
          return '<span>' + this.selectedOptions[0].innerHTML + '</span>';
        });
        $(input).find("input").replaceWith(function() {
          return '<span>' + this.value + '</span>';
        });
        $(input).find("textarea").replaceWith(function() {
            return '<span>' + this.value.replace(/(\n)/gm,"<br/>") + '</span>';
        });

        return input;
      },
      updateProgressIndicators: function() {
        var self = this;
        var input = $(".programInput");
        var activeSet = $.map(this.model.get("activeNodes"), function(value,index){return [[index, value]];});

        // mark active nodes as locked
        if(activeSet.length > 0){
          activeSet.forEach(function(activeNodes) {
            if($(input).find("#active-" + activeNodes[0]).length > 0 && activeNodes[1] == true) {
                var workspace = $(".editorWorkspace");
                workspace.children("#active-" + activeNodes[0]).remove();
                var activeIndicator = $(input).find("#active-" + activeNodes[0]);
                var editorWidth = workspace.width();
                $(activeIndicator).width(editorWidth);

                activeIndicator = activeIndicator.detach();
                $(activeIndicator.first()).appendTo(workspace);

                var targetPosition = $("#" + activeIndicator.attr("target-node")).offset();
                if(targetPosition){
                  $(activeIndicator).offset({top:targetPosition.top - workspace.offset().top, left:0});
                }

                $(".editorWorkspace").find("#active-" + activeNodes[0]).removeClass("hidden");

                if(activeIndicator.attr("parent-node") !== null) {
                  $(".editorWorkspace").children("#active-" + activeIndicator.attr("parent-node")).addClass("hidden");
                }
            }
            else if($(input).find("#active-" + activeNodes[0]).length > 0 && activeNodes[1] == false){
              $(".editorWorkspace").children("#active-" + activeNodes[0]).addClass("hidden");
            }
          });
        }

        // updated counters
        var counterSet = $.map(this.model.get("nodesCounter"), function(value,index){return [[index, value]];});
        if(counterSet.length > 0){
          counterSet.forEach(function(nodeCounter) {
            var t = $(input).find("#progress-counter-" + nodeCounter[0]);
            $(input).find("#progress-counter-" + nodeCounter[0]).text(nodeCounter[1]);
          });
        }

        // update true/false nodes
        $(".progress-true-false-indicator").each(function(index) {
          var span = $(this);
          var nodeCounter = self.model.get("nodesCounter");
          var test =  nodeCounter[span.attr("true-node")];
          var test2 = nodeCounter[span.attr("false-node")];
          if(typeof nodeCounter[span.attr("true-node")] !== "undefined" && typeof nodeCounter[span.attr("false-node")] !== "undefined") {
            if(nodeCounter[span.attr("true-node")] > nodeCounter[span.attr("false-node")]){
              span.text($.i18n.t("debugger.yes"));
              span.addClass("progress-true-indicator");
            } else {
              span.text($.i18n.t("debugger.no"));
              span.addClass("progress-false-indicator");
            }
            span.removeClass("hidden");
          } else if ( typeof nodeCounter[span.attr("true-node")] !== "undefined" && typeof nodeCounter[span.attr("false-node")] === "undefined" ) {
            span.text($.i18n.t("debugger.yes"));
            span.addClass("progress-true-indicator");
            span.removeClass("hidden");
          } else if ( typeof nodeCounter[span.attr("true-node")] === "undefined" && typeof nodeCounter[span.attr("false-node")] !== "undefined" ) {
            span.text($.i18n.t("debugger.no"));
            span.addClass("progress-false-indicator");
            span.removeClass("hidden");
          }
          console.log( index + " : " + span.attr("id") + " true: " + span.attr("true-node") + " false: " + span.attr("false-node"));
        });

        return input;
      },
      /**
      * Render the editor view
      */
      render: function() {

        var self = this;

        // render the editor with the program
        this.$el.html(this.tplEditor({
          program: this.model
        }));

        if (this.model) {
          // put the name of the place by default in the modal to edit
          if (typeof this.model !== 'undefined') {
            $("#edit-program-name-modal .program-name").val(this.model.get("name"));
          }

          // hide the error message
          $("#edit-program-name-modal .text-error").hide();

          this.refreshDisplay();

          // fix the programs list size to be able to scroll through it
          this.resize($(".programInput"));

          //$(".programInput").height("100%");
        }
        return this;
      }

    });
    return ProgramReaderView;
  });
