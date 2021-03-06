/**
 * Created by thibaud on 19/05/2014.
 */
define([
    "app",
    "text!templates/program/nodes/defaultActionNode.html",
    "text!templates/program/nodes/deviceNode.html",
    "text!templates/program/nodes/serviceNode.html",
    "text!templates/program/nodes/ifNode.html",
    "text!templates/program/nodes/whenNode.html",
    "text!templates/program/nodes/defaultEventNode.html",
    "text!templates/program/nodes/programEventNode.html",
    "text!templates/program/nodes/stateNode.html",
    "text!templates/program/nodes/keepStateNode.html",
    "text!templates/program/nodes/whileNode.html",
    "text!templates/program/nodes/booleanExpressionNode.html",
    "text!templates/program/nodes/comparatorNode.html",
    "text!templates/program/nodes/numberNode.html",
    "text!templates/program/nodes/waitNode.html",
    "text!templates/program/nodes/programNode.html",
    "text!templates/program/nodes/programActionNode.html",
    "text!templates/program/nodes/defaultPropertyNode.html",
    "text!templates/program/nodes/selectNode.html",
    "text!templates/program/nodes/scaleTemplate.html"
], function(App, dfltActionTpl, deviceTpl, serviceTpl, ifTpl, whenTpl, dfltEventTpl, programEventTpl, stateTpl, keepStateTpl, whileTpl, booleanExpressionTpl, comparatorTpl, numberTpl, waitTpl, programTpl, pgmActionTpl, dfltPropertyTpl, selectNodeTpl, scaleTpl) {
    var ProgramInputBuilder = {};
    // router
    ProgramInputBuilder = Backbone.Model.extend({
        tplDefaultActionNode: _.template(dfltActionTpl),
        tplDeviceNode: _.template(deviceTpl),
        tplServiceNode: _.template(serviceTpl),
        tplIfNode: _.template(ifTpl),
        tplWhenNode: _.template(whenTpl),
        tplEventNode: _.template(dfltEventTpl),
        tplEventProgramNode: _.template(programEventTpl),
        tplStateNode: _.template(stateTpl),
        tplKeepStateNode: _.template(keepStateTpl),
        tplWhileNode: _.template(whileTpl),
        tplBooleanExpressionNode: _.template(booleanExpressionTpl),
        tplComparatorNode: _.template(comparatorTpl),
        tplNumberNode: _.template(numberTpl),
        tplWaitNode: _.template(waitTpl),
        tplProgramNode: _.template(programTpl),
        tplProgramActionNode: _.template(pgmActionTpl),
        tplDefaultPropertyNode: _.template(dfltPropertyTpl),
        tplSelectNode: _.template(selectNodeTpl),
        tplScale: _.template(scaleTpl),


        initialize: function() {
            this.isValid = true;
            this.isComplete = true;
        },
        buildInputFromObjectOrTarget: function(node, currentNode) {
            if (typeof node.object !== 'undefined') {
                return this.buildInputFromNode(node.object, currentNode)
            } else if (typeof node.target !== 'undefined') {
                return this.buildInputFromNode(node.target, currentNode)
            }
            console.warn("Node Invalid: " + node);
            return "Unkwown";
        },
        buildInputFromNodeOrMandatory: function(jsonNode, currentNode, test) {
            if (test) {
                return this.buildInputFromNode(jsonNode, currentNode);
            } else {
                input = "<div class='btn btn-default btn-prog input-spot mandatory-spot' id='" + jsonNode.iid + "'>";
                input += "<span data-i18n='language.mandatory-keyword'/></div>";
                return input;
            }
        },
        /**
         * buildInputFromNode is the only 'public' method, it takes as input
         * the json representation of a SPOK program and build the corresponding representation in HTML
         *
         * @param jsonNode
         * @param currentNode
         * @returns {string}
         */
        buildInputFromNode: function(jsonNode, currentNode) {
            var self = this;

            param = {
                node: jsonNode,
                currentNode : currentNode,
                engine: this
            };
            var deletable = false;
            var input = "";
            if (jsonNode.type === undefined) {
                console.error("There is a problem in the passing of argument for node: " + currentNode);
                return "ERROR";
            }
            switch (jsonNode.type) {
                case "stopMyself":
                    this.stopMyself = true;
                case "action":
				case "action0":
				case "action1":
				case "action2":
				case "action3":
                    deletable = true;
                    input += this.buildActionNode(param);
                    this.stopMyself = false;
                    break;
                case "if":
                    deletable = true;
                    input += this.tplIfNode(param);
                    break;
                case "booleanExpression":
                    deletable = true;
                    input += this.tplBooleanExpressionNode(param);
                    break;
                case "comparator":
                    input += this.buildComparatorNode(param, currentNode);
                    break;
                case "when":
                case "whenImp":
                    deletable = true;
                    input += this.tplWhenNode(param);
                    break;
                case "device":
                    param.node.validDevice = this.isDeviceValid(param.node.value);
                    this.isComplete = this.isComplete && param.node.validDevice;
                    param.node.name = this.getDeviceName(param.node.value, param.node.name);
                    if(!param.node["hideSelector"] || param.node.hideSelector!=true){
                        input += this.tplDeviceNode(param);
                    }
                    break;
                case "select":
                    input += this.tplSelectNode(param);
                    break;
                case "service":
                    param.node.name = this.getServiceName(param.node.value);
                    input += this.tplServiceNode(param);
                    break;
                case "event":
                    deletable = true;
                    if(!param.node["hideSelector"] || param.node.hideSelector!=true){
                        input += this.buildEventNode(param);
                    }
                    break;
                case "eventProgram":
                    deletable = true;
                    input += this.buildEventProgramNode(param);
                    break;
                case "state":
                case "stateProgram":
                case "keepStateProgram":
                case "maintainableState":
                    deletable = true;
                    input = this.buildStateNode(param);
                    break;
                case "property":
                    deletable = true;
                    input += this.buildPropertyNode(param);
                    break;
                case "while":
                    deletable = true;
                    input += this.tplWhileNode(param);
                    break;
                case "keepState":
                    input += this.tplKeepStateNode(param);
                    break;
                case "empty":
                    input += "<div class='btn btn-default btn-prog input-spot empty-btn' id='" + jsonNode.iid + "'><span data-i18n='language.nothing-keyword'/></div>";
                    break;
                case "mandatory":
                    input += "<div class='btn btn-default input-spot mandatory-spot' id='" + jsonNode.iid + "'><span data-i18n='language.mandatory-keyword'/></div>";
                    break;
                case "seqRules":
                      input+="<div class='rules-node seq-block-node'>";
                      input+= jsonNode.iid == 1 || jsonNode.iid == 3?"<h2 class='seq-block-header'><span data-i18n='language.only-once'/></h2>":"";
                      jsonNode.rules.forEach(function(rule) {
                          if (rule !== jsonNode.rules[0]) {
                              input += "<div class='separator'><span class='link-span' data-i18n='language.op-then-rule'/></div>";
                          }
                          input += self.buildInputFromNode(rule, currentNode);
                      });
                      input+="</div>";
                    break;
                case "setOfRules":
                      input+= jsonNode.iid != 0?"<div class='rules-node set-block-node'>":"";
                      input+= jsonNode.iid == 1 || jsonNode.iid == 3?"<h2 class='set-block-header'><span data-i18n='language.repeated'/></h2>":"";
                      jsonNode.rules.forEach(function(rule) {
                        if (rule !== jsonNode.rules[0]) {
                          input += "<div class='separator'><span class='link-span' data-i18n='language.op-and-rule'/></div>";
                        }
                        input += self.buildInputFromNode(rule, currentNode);
                      });
                      input+= jsonNode.iid != 0?"</div>":"";
                    break;
                case "boolean":
                    input += "<button class='btn btn-prog btn-primary' id='" + jsonNode.iid + "'><span>" + jsonNode.value + "</span></button>";
                    break;
                case "number":
                    input += this.tplNumberNode(param);
                    break;
                case "wait":
                    deletable = true;
                    input += this.tplWaitNode(param);
                    break;
                case "param":
                    deletable=false;
                    if (jsonNode.icon) {
                        input += "<div class='btn btn-prog btn-prog-action ' id='" + jsonNode.iid + "'>" + "<img src='"+jsonNode.icon+"' width='36px'/>" + "</div>";
                        break;
                    }
                    if (jsonNode.i18n) {
                        input += "<div class='btn btn-prog btn-prog-action input-spot' id='" + jsonNode.iid + "'>" + "<span data-i18n='" + jsonNode.i18n + "'/>" + "</div>";
                        break;
                    }
                    input += "<div class='btn btn-prog btn-prog-action mandatory-spot input-spot' id='" + jsonNode.iid + "'>" + "<span data-i18n='language.mandatory-keyword'/>" + "</div>";
                    break;
                case "programCall":
                    param.state = this.getProgramState(jsonNode.value);
                    param.name = programs.getName(jsonNode.value);
                    input += this.tplProgramNode(param);
                    break;
                case "programs":
                    input += "<button class='btn btn-default input-spot mandatory-spot' id='" + jsonNode.iid + "'><span data-i18n='language.mandatory-keyword'/></button>";
                    break;
                case "scale":
                    input += this.tplScale(param);
                    break;
                default:
                    input += "<button class='btn btn-prog btn-primary' id='" + jsonNode.iid + "'><span>" + jsonNode.type + "</span></button>";
                    break;
            }

            // For only some kind of node we add a delete button
            if (deletable == true) {
                var supprClasses = "";
                if (currentNode == jsonNode.iid) {
                    supprClasses = "glyphicon glyphicon-trash";
                }
                input = "<div class='btn-current'>"
                    + input
                    + "<div class='btn-prog btn-trash " + supprClasses + "' id='" + jsonNode.iid + "' style='right:5px;position:absolute;top:0px;'></div></div>";

            }

            return input;
        },

        buildActionNode: function(param) {
            if (param.node.target.deviceType || param.node.target.type === "select") {
                return devices.getTemplateByType('action',param.node.target.deviceType, param);
            }
            if (param.node.target.serviceType) {
                return services.getTemplateByType('action',param.node.target.serviceType, param);
            }
            if (param.node.target.type === "programCall" || param.node.target.type === "programs") {
                return this.tplProgramActionNode(param);
            }
            return this.tplDefaultActionNode(param);
        },
        buildPropertyNode: function(param) {
            if (param.node.target.deviceType) {
                return devices.getTemplateByType('property',param.node.target.deviceType, param);
            }
            if (param.node.target.serviceType) {
                return services.getTemplateByType('property',param.node.target.serviceType, param);
            }
            return this.tplDefaultPropertyNode(param);
        },
        buildStateNode: function(param) {
            if (param.node.object.deviceType) {
                return devices.getTemplateByType('state',param.node.object.deviceType, param);
            }
            if (param.node.object.serviceType) {
                return services.getTemplateByType('state',param.node.object.serviceType, param);
            }
            return this.tplStateNode(param);
        },
        buildEventNode: function(param) {
            var result = "";
            if (param.node.source.deviceType) {
                return devices.getTemplateByType('event',param.node.source.deviceType, param);
            }
            if (param.node.source.serviceType) {
                return services.getTemplateByType('event',param.node.source.serviceType, param);

            }
            return this.tplEventNode(param);
        },
        buildEventProgramNode: function(param) {
            return this.tplEventProgramNode(param);
        },
        buildComparatorNode: function(param, currentNode) {
            var leftOp = this.buildInputFromNode(param.node.leftOperand, currentNode);
            if (param.node.leftOperand.target) {
                if (param.node.leftOperand.target.deviceType) {
                    var deviceOfNode = devices.where({type:param.node.leftOperand.target.deviceType})[0];
                    if (deviceOfNode != undefined) {
                        param.node.rightOperand.scale = deviceOfNode.getScale();
                        param.node.rightOperand.type = param.node.leftOperand.returnType;
                        if(param.node.leftOperand.returnType !== 'undefined' && param.node.leftOperand.returnType == "boolean")
                            param.node.rightOperand.value="true";
                        param.node.rightOperand.unit = (param.node.leftOperand.unit) ? param.node.leftOperand.unit: "";
                        //param.node.rightOperand.value = (param.node.leftOperand.defaultValue) ? param.node.leftOperand.defaultValue: "";
                    }
                }
                if (param.node.leftOperand.target.serviceType) {
                    var serviceOfNode = services.where({type:param.node.leftOperand.target.serviceType})[0];
                    if (serviceOfNode != undefined) {
                        param.node.rightOperand.scale = serviceOfNode.getScale();
                        param.node.rightOperand.type = param.node.leftOperand.returnType;
                        if(param.node.leftOperand.returnType !== 'undefined' && param.node.leftOperand.returnType == "boolean")
                            param.node.rightOperand.value="true";
                        param.node.rightOperand.unit = (param.node.leftOperand.unit) ? param.node.leftOperand.unit: "";
                        //param.node.rightOperand.value = (param.node.leftOperand.defaultValue) ? param.node.leftOperand.defaultValue: "";
                    }
                }
            }

            // enabling sup/ing comparator if returnType not a scale
            param.node.comparatorEnabled = (param.node.leftOperand.returnType !== "scale" && param.node.leftOperand.returnType !== "boolean");
            if(param.node.comparator === '==' && param.node.rightOperand.type === 'boolean' && param.node.rightOperand.value === 'true') {
                return leftOp;
            } else {
                var rightOp = this.buildInputFromNode(param.node.rightOperand, currentNode);
                return leftOp + this.tplComparatorNode(param) + rightOp;
            }
        },
        getDeviceName: function(id, defaultName) {
            var deviceName;
            if (devices.get(id) == undefined) {
                deviceName = defaultName;
            } else if (devices.get(id).get("name") !== "") {
                deviceName = devices.get(id).get("name");
            } else {
                deviceName = devices.get(id).get("id");
            }
            return deviceName;
        },

        getServiceName: function(id) {
            if (services.get(id) == undefined) {
                this.isComplete = false;
                return "UNKNOWN SERVICE";
            }
            return services.get(id).get("name");
        },
        isDeviceValid: function(id) {
            return devices.get(id) != undefined;
        },
        getProgramState: function(id) {
            if (this.stopMyself) {
                return "";
            }
            p = programs.get(id);
            if (p == undefined) {
                this.isValid = false;
                return "absent";
            }
            if (p.get("runningState") == "INVALID") {
                this.isComplete = false;
                return "invalid";
            }
            if (!p.isValid()) {
                return "incomplete";
            }
            return "";
        },
        
        getVariableFromDevice: function(deviceId, varname) {
            var objPatterns = devices.get(deviceId).get(varname);
			if (typeof objPatterns === 'string') {
				objPatterns = $.parseJSON(objPatterns);
			}
            listPattern = [];
			$.each(objPatterns, function (keyPattern) {
				listPattern.push( keyPattern );
			});
            return listPattern;
        }

    });

        return ProgramInputBuilder;
});
