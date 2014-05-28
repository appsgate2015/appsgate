define([
  "app",
  "modules/grammar",
  "modules/ProgramKeyboardBuilder",
  "modules/ProgramInputBuilder"
  ], function(App, Grammar, ProgramKeyboardBuilder, ProgramInputBuilder) {
    var ProgramMediator = {};
    // router
    ProgramMediator = Backbone.Model.extend({
      initialize: function() {
        this.ProgramKeyboardBuilder = new ProgramKeyboardBuilder();
        this.ProgramInputBuilder = new ProgramInputBuilder();

        this.resetProgramJSON();
        this.currentNode = 1;
        this.maxNodeId = 1;
        this.lastAddedNode = null;
        this.Grammar = new Grammar();
      },
      /**
      * Method to start a brand new program
      */
      resetProgramJSON: function() {
        this.programJSON = {
          iid: 0,
          type: "setOfRules",
          rules: [{
            iid: 1,
            type: "empty"
          }]
        };
      },
      /**
      * method that loads a program and set the max id
      */
      loadProgramJSON: function(programJSON) {
        this.programJSON = programJSON;
        this.maxNodeId = this.findMaxId(programJSON);
        this.currentNode = -1;
      },
      /**
      * Method that recursively retrieve the max node id of the program
      */
      findMaxId: function(curNode) {
        for (var o in curNode) {
          if (typeof curNode[o] === 'object') {
            this.findMaxId(curNode[o]);
          }
          if (curNode[o].iid > this.maxNodeId) {
            this.maxNodeId = curNode[o].iid;
          }
        }
        return this.maxNodeId;
      },
      /**
      * Method to set the id of the node pointed by the cursor in the current program
      */
      setCurrentPos: function(id) {
        if (id) {
          console.log("Setting current_pos to: " + id);
          this.currentNode = id;
        } else {
          this.currentNode = -1;
          console.error("A non valid pos has been passed to setCurrent pos: " + id);
        }
      },
      /**
      * method that set the cursor and build keyboard
      */
      setCursorAndBuildKeyboard: function(id) {
        console.debug("call to setCursorAndBuildKeyboard method");
        this.setCurrentPos(id);
        //this.checkProgramAndBuildKeyboard(this.programJSON);
      },
      /**
      * Method that catch the event button pressed and do what is needed
      */
      buttonPressed: function(button) {
        n = {};
        if ($(button).hasClass("specific-node")) {
          n = JSON.parse($(button).attr('json'));
        } else if ($(button).hasClass("device-node")) {
          n = this.getDeviceJSON(button.id);
        } else if ($(button).hasClass("service-node")) {
          n = this.getServiceJSON(button.id);
        } else if ($(button).hasClass("program-node")) {
          n = {
            "value": button.id,
            "name": $(button).attr('prg_name'),
            "iid": "X",
            "type": "programCall"
          };
        } else if ($(button).hasClass("number-node")) {
          n = {
            "type": "number",
            "iid": "X",
            "value": "0"
          };
        } else if ($(button).hasClass("if-node")) {
          n = this.getIfJSON();
        } else if ($(button).hasClass("when-node")) {
          n = {
            "type": "when",
            "iid": "X",
            "events": this.getEmptyJSON("mandatory"),
            "seqRulesThen": {
              "iid": "X",
              "type": "seqRules",
              "rules": [this.getEmptyJSON("mandatory")]
            }
          };
        } else if ($(button).hasClass("while-node")) {
          n = {
            "type": "while",
            "iid": "X",
            "state": this.getEmptyJSON("mandatory"),
            "rules": {
              "type": "keepState",
              "iid": "X",
              "state": this.getEmptyJSON("mandatory")

            },
            "rulesThen": {
              "iid": "X",
              "type": "seqRules",
              "rules": [this.getEmptyJSON("empty")]
            }
          };
        } else {
		  console.debug("This event is not catched: "+ button);
		}

        this.lastAddedNode = this.setIidOfJson(n);
        this.appendNode(this.lastAddedNode, this.currentNode);

        // reset the selection because a node was added
        this.setCurrentPos(-1);
        dispatcher.trigger("refreshDisplay");
      },
      /**
      * Method to append a node to the program
      */
      appendNode: function(node, pos) {
        this.programJSON = this.recursivelyAppend(node, pos, this.programJSON);
      },
      /**
      * Method that recursively append a node to a program
      */
      recursivelyAppend: function(nodeToAppend, pos, curNode) {
        if (parseInt(curNode.iid) === parseInt(pos)) {
          curNode = nodeToAppend;
        } else {
          for (var o in curNode) {
            if (typeof curNode[o] === "object") {
              var prevIid = curNode[o].iid;
              // If adding an element to a rules array, we add an empty element to allow further insertions
              curNode[o] = this.recursivelyAppend(nodeToAppend, pos, curNode[o]);
              if (parseInt(prevIid) === parseInt(pos) && $.isArray(curNode)) {
                if(curNode.length-1 == o){
                  curNode.push(this.setIidOfJson(this.getEmptyJSON("empty")));
                }
              }
            }
          }
        }
        return curNode;
      },
      /**
      * Method that remove the selected node
      */
      removeSelectedNode: function() {
        console.debug("removing node: " + this.currentNode);
        this.programJSON = this.recursivelyRemove(this.currentNode, this.programJSON);
        this.buildInputFromJSON();
      },
      /**
      * method that recursively remove a node (by putting empty instead of what was present before)
      */
      recursivelyRemove: function(pos, curNode, parentNode) {
        if (parseInt(curNode.iid) === parseInt(pos)) {
          curNode = {
            iid: curNode.iid,
            type: "empty"
          };
        } else {
          for (var o in curNode) {
            if (typeof curNode[o] === "object") {
              if (Array.isArray(curNode[o])) {
                curNode[o] = this.recRemoveArray(pos, curNode[o], curNode);
              } else {
                curNode[o] = this.recursivelyRemove(pos, curNode[o], curNode);
              }
            }
          }
        }
        return curNode;
      },
      /**
      * Method that is called to remove a node in an array.
      * it cleans up the array by removing extra empty nodes
      */
      recRemoveArray: function(pos, curNode, parentNode) {
        var prevIsEmpty = false;
        var retNode = [];
        for (var o in curNode) {
          if (typeof curNode[o] === "object" && Array.isArray(curNode[o])) {
            console.error("An array has been found inside an array");
            return curNode;
          }
          var recNode = this.recursivelyRemove(pos, curNode[o], curNode);
          if (recNode.type !== "empty") {
            retNode.push(recNode);
            prevIsEmpty = false;
          } else {
            if (!prevIsEmpty) {
              retNode.push(recNode);
            } else {
              // Check whether the deleted node is not the node with the current pos
              if (recNode.iid == pos) {
                retNode.pop();
                retNode.push(recNode);
              }
            }
            prevIsEmpty = true;
          }
        }
        return retNode;
      },
      /*
      * set a node attribute
      */
      setNodeArg: function(iid, index, value) {
        this.recursivelySetNodeArg(iid, index, value, this.programJSON);
      },
      /**
      * Method that set the args in a node
      */
      recursivelySetNodeArg: function(iid, index, value, curNode) {
        if (parseInt(curNode.iid) === parseInt(iid)) {
          curNode.args[index] = value;
        } else {
          for (var o in curNode) {
            if (typeof curNode[o] === "object") {
              this.recursivelySetNodeArg(iid, index, value, curNode[o]);
            }
          }
        }
      },
      /*
      * set a node attribute
      */
      setNodeAttribute: function(iid, attribute, value) {
        this.recursivelySetNodeAttribute(iid, attribute, value, this.programJSON);
      },
      /**
      * Method that set an attribute to a node
      */
      recursivelySetNodeAttribute: function(iid, attribute, value, curNode) {
        if (parseInt(curNode.iid) === parseInt(iid)) {
          curNode[attribute] = value;
        } else {
          for (var o in curNode) {
            if (typeof curNode[o] === "object") {
              this.recursivelySetNodeAttribute(iid, attribute, value, curNode[o]);
            }
          }
        }
      },
      /**
      * Method to replace the X in the iid attribute of nodes
      */
      setIidOfJson: function(obj) {
        if (obj.iid === "X") {
          obj.iid = ++this.maxNodeId;
        }
        for (var k in obj) {
          if (typeof obj[k] === "object") {
            this.setIidOfJson(obj[k]);
          }
        }
        return obj;
      },
      /**
      * return a JSON object corresponding to a device given its id
      */
      getDeviceJSON: function(deviceId) {
        var d = devices.get(deviceId);
        var deviceName = d.get("name");
        return {"type": "device", "value": deviceId, "name": deviceName, "iid": "X", "deviceType": d.get("type")};

      },
      /**
      * return a JSON object corresponding to a service given its id
      */
      getServiceJSON: function(serviceId) {
        var s = services.get(serviceId);
        var serviceName = s.get("name");
        return {"type": "service", "value": serviceId, "name": serviceName, "iid": "X", "serviceType": s.get("type")};
      },
      /**
      * Return the JSON structure of an if
      */
      getIfJSON: function() {
        return {
          "type": "if",
          "iid": "X",
          "expBool": this.getEmptyJSON("mandatory"),
          "seqRulesTrue": {
            "type": "seqRules",
            "iid": "X",
            "rules": [this.getEmptyJSON("mandatory")]
          },
          "seqRulesFalse": {
            "type": "seqRules",
            "iid": "X",
            "rules": [this.getEmptyJSON("empty")]
          }
        };
      },
      /**
      * return the JSON object corresponding to an empty node
      */
      getEmptyJSON: function(type) {
        return {
          "type": type,
          "iid": "X"
        };
      },

      /**
      * Method that build the input and put it in the programInput div
      */
      buildInputFromJSON: function() {
        $(".programInput").html(this.getInputFromJSON());
      },

      /**
      * Method to get the JSON representation of the program.
      */
      getInputFromJSON: function() {
        console.debug("Getting Input FROM JSON");
        if (this.checkProgramAndBuildKeyboard()) {
          this.isValid = true;
        } else {
          this.isValid = false;
        }
        var input = $.parseHTML(this.ProgramInputBuilder.buildInputFromNode(this.programJSON, this.currentNode));

        $(input).find(".btn").css("padding", "3px 6px");

        $(input).i18n();

        var keyBands = $(".expected-elements").children();
        var self = this.ProgramKeyboardBuilder;
        keyBands.each(function(index) {
          self.sortKeyband(this);
        });

        return input;
      },

      /**
      *
      */
      findNextInput: function(selected) {
        //
        while(typeof selected != "undefined") {
          console.log("selected(inside) : "+selected.nextAll(".input-spot").attr("id"));
          if(selected.nextAll(".input-spot").length != 0) {
            return selected;
          }
          selected = selected.parent();
        }
        return selected;

      },

      /**
      *
      */
      checkProgramAndBuildKeyboard: function(programJSON) {
        console.debug("CheckProgramAndBuildKeyboard")
        if (typeof programJSON !== "undefined") {
          this.programJSON = programJSON;
        }
        var n = this.Grammar.parse(this.programJSON, this.currentNode);
        if (n == null && !this.isProgramEmpty()) {
          //console.log("Program is correct");
          return true;
        } else if (n !== null && n.expected[0] === "ID") {
          console.warn('Something unexpected happened');
          this.resetProgramJSON();
          this.checkProgramAndBuildKeyboard();
        } else if (n !== null) {
          if(typeof n.id !== "undefined") {
            this.setCurrentPos(n.id);
          }
          this.ProgramKeyboardBuilder.buildKeyboard(n);
        }
        var keyBands = $(".expected-elements").children();
        var self = this.ProgramKeyboardBuilder;
        keyBands.each(function(index) {
          self.sortKeyband(this);
        });
        return false;
      },

      isProgramEmpty:function(){
        return (this.programJSON.rules.length === 1 && this.programJSON.rules[0].type === "empty");
      }
    });
    return ProgramMediator;
  });
