define([
  "app",
    "views/service/details",
    "text!templates/services/details/energyMonitoring.html"

  ], function (App, ServiceDetailsView, EnergyMonitoringTemplate) {

	var EnergyMonitoringDetailsView = {};
	// detailed view for TTS Service
	EnergyMonitoringDetailsView = Backbone.View.extend({
		tplEnergyMonitoring: _.template(EnergyMonitoringTemplate),

		// map the events and their callback
		events: {
			"click #edit-energy-group-modal button.valid-button": "onClickEditEnergyGroup",
			"click button.back-button": "onBackButton",

			"click button.delete-energy-group-button": "onDeleteEnergyGroup",
			"click button.cancel-delete-energy-group-button": "onCancelDeleteEnergyGroup",
			"click button.delete-popover-button": "onClickDeleteEnergyGroup",

			"click button.start": "onStart",
			"click button.stop": "onStop",

			"click button.btn-target-dependencies": "onShowDependencies",
			"click button.btn-target-timelines": "onShowTimelines",

			"click input#allDevice": "onCheckAllDevice",
			"click input.checkbox-select-device": "onCheckDevice",

			"click button.add-annotation": "onAddAnnotation",
			"click button.delete-annotation": "onDeleteAnnotation",
			"keyup #input-new-annotation": "onKeyupInputAnnotation",
			"keyup #energyGroupNameInput": "onKeyupGroupName",
		},

		initialize: function () {
			var self = this;

			EnergyMonitoringDetailsView.__super__.initialize.apply(this, arguments);

			self.listenTo(self.model, 'energyDuringPeriodChanged', function (e) {
				self.updateValues(self.model.get('id'));
			});
			self.listenTo(self.model, 'statusChanged', function (e) {
				self.updateState(self.model.get('id'));
				self.updateDates();
			});
			self.listenTo(self.model, 'budgetTotalChanged', function (e) {
				self.updateValues(self.model.get('id'));
			});
			self.listenTo(self.model, 'budgetUnitChanged', function (e) {
				self.updateValues(self.model.get('id'));
			});
			self.listenTo(self.model, 'groupNameChanged', function (e) {
				$("#energy-group-name").text(self.model.get("name"));
			});
			self.listenTo(self.model, 'sensorsGroupChanged', function (e) {
				self.updateSensorsList();
			});
			self.listenTo(self.model, 'budgetReset', function (e) {
				self.updateValues(self.model.get('id'));
				self.updateHistory();
			});
			self.listenTo(self.model, 'annotationsChanged', function (e) {
				self.updateAnnotations();
			});
		},

		/**
		 * Return to the previous view
		 */
		onBackButton: function () {
			window.history.back();
		},

		/**
		 * Callback to edit energy group
		 */
		onClickEditEnergyGroup: function () {
			var self = this;

			var name = $("#edit-energy-group-modal #energyGroupNameInput").val();
			var sensors = self.getDevicesSelected();
			var budgetTotal = $("#edit-energy-group-modal #budgetValueInput").val();
			var budgetUnit = $("#edit-energy-group-modal #unitSelector").val();
			console.log("Edit: " + name + " " + budgetTotal + " " + budgetUnit);

			if (name !== self.model.get("name")) {
				self.model.setName(name);
			}

			if (budgetTotal !== self.model.get("budgetTotal")) {
				self.model.setBudgetTotal(budgetTotal);
			}

			if (parseInt(budgetUnit) !== parseInt(self.model.get("budgetUnit"))) {
				self.model.setBudgetUnit(budgetUnit);
			}

			var sensorsChanged = function (oldSensors, newSensors) {
				if (oldSensors.length !== newSensors.length) {
					return true;
				} else {
					_.each(oldSensors, function (s) {
						if (!_.contains(newSensors, s)) {
							return true;
						}
					});
				}
				return false;
			}(self.model.get("sensors"), sensors);
			if (sensorsChanged) {
				self.model.setEnergySensorsGroup(sensors);
			}

			$("#edit-energy-group-modal").modal("hide");
		},

		/**
		 * Callback when the user has clicked on the button to cancel the deleting
		 */
		onCancelDeleteEnergyGroup: function () {
			// destroy the popover
			this.$el.find("#delete-program-popover").popover('destroy');
		},

		/**
		 * Callback to delete energy Group
		 */
		onDeleteEnergyGroup: function (e) {
			var self = this;
			e.preventDefault();
			services.getEnergyMonitoringAdapter().removeEnergyMonitoringGroup(self.model.get('id'));

			appRouter.navigate("#services/types/EnergyMonitoringAdapter", {
				trigger: true
			});
		},

		/**
		 * Callback when the user has clicked on the button delete.
		 */
		onClickDeleteEnergyGroup: function (e) {
			var self = this;
			// create the popover
			this.$el.find("#delete-program-popover").popover({
				html: true,
				title: $.i18n.t("services.energy-monitoring.warning-delete"),
				content: "<div class='popover-div'><button type='button' class='btn btn-default cancel-delete-energy-group-button'>" + $.i18n.t("form.cancel-button") + "</button><button type='button' class='btn btn-danger delete-energy-group-button'>" + $.i18n.t("form.delete-button") + "</button></div>",
				placement: "bottom"
			});
			// listen the hide event to destroy the popup, because it is created to every click on Delete
			this.$el.find("#delete-program-popover").on('hidden.bs.popover', function () {
				self.onCancelDeleteEnergyGroup();
			});
			// show the popup
			this.$el.find("#delete-program-popover").popover('show');
		},

		/**
		 * Callback to start monitoring
		 */
		onStart: function (e) {
			services.getCoreEnergyMonitoringGroupById(this.model.get('id')).startMonitoring();
		},

		/**
		 * Callback to start monitoring
		 */
		onStop: function (e) {
			services.getCoreEnergyMonitoringGroupById(this.model.get('id')).stopMonitoring();
		},

		/**
		 * Callback when check all devices
		 */
		onCheckAllDevice: function (e) {
			_.forEach($(".checkbox-select-device"), function (chkbxDevice) {
				$(chkbxDevice).prop('checked', $("#allDevice").is(':checked'));
			});
		},

		/**
		 * Callback when check one devices
		 */
		onCheckDevice: function () {
			if ($(".checkbox-select-device:checked").length === devices.getDevicesByType(6).length) {
				$("#allDevice").prop('checked', true);
			} else {
				$("#allDevice").prop('checked', false);
			}
		},

		/**
		 * Callback on click dependencies
		 */
		onShowDependencies: function () {
			appRouter.navigate("#dependancies/" + this.model.get("id"), {
				trigger: true
			});
		},

		/**
		 * Callback on click timelines
		 */
		onShowTimelines: function () {
			appRouter.navigate("#debugger/" + this.model.get("id"), {
				trigger: true
			});
		},

		/**
		 * Callback on click add annotation
		 */
		onAddAnnotation: function () {
			this.model.addAnnotation($("#input-new-annotation").val());
		},

		/**
		 * Callback on click delete annotation
		 */
		onDeleteAnnotation: function (e) {
			e.preventDefault();
			var id = $(e.currentTarget).parent("span").parent("div").attr("id");
			var timestamp = id.split("annotation-")[1];

			this.model.removeAnnotation(timestamp);
		},

		/**
		 * Callback when typing in input new annotation
		 */
		onKeyupInputAnnotation: function (e) {
			var self = this;
			// Enter
			if (e.keyCode === 13) {
				// If there is an annotation add it 
				if ($("#input-new-annotation").val() !== "") {
					self.onAddAnnotation();
				}
			} else {
				if ($("#input-new-annotation").val() === "") {
					$(".btn.add-annotation").prop("disabled", true);
				} else {
					$(".btn.add-annotation").prop("disabled", false);
				}
			}
		},

		/**
		 * Callback when typing group name
		 */
		onKeyupGroupName: function (e) {
			if ($("#energyGroupNameInput").val() !== "") {
				$(".btn.valid-button").prop("disabled", false);
				$(".btn.valid-button").removeClass("disabled");
				$(".btn.valid-button").removeClass("valid-disabled");
			} else {
				$(".btn.valid-button").prop("disabled", true);
				$(".btn.valid-button").addClass("disabled");
				$(".btn.valid-button").addClass("valid-disabled");
			}
		},

		/**
		 * Render the detailed view of the service
		 */
		render: function () {
			var self = this;

			if (!appRouter.isModalShown) {

				this.$el.html(this.tplEnergyMonitoring({
					model: this.model
				}));

				// Build modal with previous values
				this.buildUnitSelector();
				this.buildDevicesChoice();

				this.updateValues(this.model.get('id'));
				this.updateState(this.model.get('id'));
				this.updateDates();
				this.buildSensorsList();
				this.updateHistory();
				this.updateSensorsList();
				this.updateAnnotations();

				this.resize($(".scrollable"));

				// translate the view
				this.$el.i18n();
				return this;
			}
		},

		/**
		 * Method to build the input checkbox for all energy devices
		 */
		buildDevicesChoice: function () {
			var self = this;
			var divChoice = $('#energyDevicesContainer');
			divChoice.append("<div class='col-md-12'><input type='checkbox' id='allDevice'><label for='allDevice'> " + $.i18n.t("services.energy-monitoring.modal-add.devices.all") + "</label></div>");

			var energyDevices = devices.getDevicesByType(6);
			_.each(energyDevices, function (device) {
				if (_.contains(self.model.get("sensors"), (device.get('id')))) {
					divChoice.append("<div class='col-md-12'><input type='checkbox' class='checkbox-select-device' id='" + device.get("id") + "' checked ><label for='" + device.get('id') + "'> " + device.get('name') + "</label></div>");
				} else {
					divChoice.append("<div class='col-md-12'><input type='checkbox' class='checkbox-select-device' id='" + device.get("id") + "'><label for='" + device.get('id') + "'> " + device.get('name') + "</label></div>");
				}
			});

			// Call Method to have correct check for allDevice
			self.onCheckDevice();
		},

		/**
		 * Method to build the unit selector with all units available
		 */
		buildUnitSelector: function () {
			var self = this;
			var selector = $('#unitSelector');

			$.each(services.getEnergyMonitoringAdapter().getUnits(), function (i, unit) {
				if (unit.value === parseInt(self.model.get('budgetUnit'))) {
					selector.append($('<option>', {
						value: unit.value,
						text: unit.text,
						selected: true
					}));
				} else {
					selector.append($('<option>', {
						value: unit.value,
						text: unit.text
					}));
				}
			});
		},

		/**
		 * Method to build the list of energy sensors
		 */
		buildSensorsList: function () {
			var self = this;
			var divSensorsList = $("#sensors-list");

			var energyDevices = devices.getDevicesByType(6);
			_.each(energyDevices, function (device) {
				divSensorsList.append("<div class='col-md-12'><input type='checkbox' id='sensor-" + device.get("id") + "' disabled><label for='" + device.get('id') + "'> " + device.get('name') + "</label></div>");
			});

		},

		/**
		 * Method to get the ids of the devices selected
		 */
		getDevicesSelected: function () {
			var ids = [];
			// Check All checked
			if ($("#allDevice").is(":checked")) {
				_.forEach($("input[type=checkbox]"), function (input) {
					if (input.id !== "allDevice") {
						ids.push(input.id);
					}
				});
			} else {
				_.forEach($("input[type=checkbox]:checked"), function (input) {
					if (input.id !== "allDevice") {
						ids.push(input.id.split("sensor-")[0]);
					}
				});
			}

			return ids;
		},

		/*
		 * Utility method to get a unit from a value
		 * @param unitValue : Value that we want to get the corresponding unit
		 */
		getUnit: function (unitValue) {
			var arrayUnit = services.getEnergyMonitoringAdapter().getUnits();
			var unit = arrayUnit[_.findIndex(arrayUnit, {
				value: parseInt(unitValue)
			})];
			return unit;
		},

		/**
		 * Method to update the html element for the status
		 * These elements are : show/hide start/stop button, the status-div and if the progress bar is active
		 */
		updateState: function (idGroup) {
			var self = this;
			var divGroup = $("#div-summary-information").children(".panel-body");

			var btnStart = $(".btn-toolbar").children(".btn-group").children(".btn.start");
			var btnStop = $(".btn-toolbar").children(".btn-group").children(".btn.stop");

			var progressBar = divGroup.children(".row").children("div").children("div").children(".progress-bar");
			var divStatus = divGroup.children(".row").children("div").children(".div-status");

			if (self.model.get('isMonitoring') === "true" || self.model.get('isMonitoring') === true) {
				btnStart.hide();
				btnStop.show();
				progressBar.addClass("active");
				divStatus.addClass("led-processing");
				divStatus.removeClass("led-deployed");
			} else {
				btnStart.show();
				btnStop.hide();
				progressBar.removeClass("active");
				divStatus.addClass("led-deployed");
				divStatus.removeClass("led-processing");
			}
		},

		/**
		 * Method to update the html element for the value
		 * These elements are : the text of total consumption, allocated budget, the percent value in the progress bar and its width
		 */
		updateValues: function (idGroup) {
			var self = this;
			var divGroup = $("#div-summary-information").children(".panel-body");

			var unit = self.getUnit(self.model.get('budgetUnit'));

			var spanTotalConsumption = divGroup.children(".row").children("div").children(".span-total-consumption");
			spanTotalConsumption.text(parseFloat(self.model.get('energyDuringPeriod')).toFixed(4));

			var spanBudgetTotal = divGroup.children(".row").children("div").children(".span-budget-allocated");
			spanBudgetTotal.text(parseFloat(self.model.get('budgetTotal')).toFixed(2));

			var spanBudgetUnit = divGroup.children(".row").children("div").children(".span-budget-unit");
			spanBudgetUnit.text(unit.text);

			// Progress Bar
			var divProgressBar = divGroup.children(".row").children("div").children("div.progress")
			var progressBar = divProgressBar.children(".progress-bar");
			var spanBudgetUsedPercent = progressBar.children(".budget-used-percent");
			var budgetUsedPercent = self.model.getPercentUsed();

			// If budget exceed 100%, create new red bar
			if (budgetUsedPercent > 100) {
				// Reduce valid bar before add new one
				progressBar.css("width", (200 - budgetUsedPercent) + "%");
				// If second bar already existed, don't recreate it
				if (divProgressBar.children(".progress-bar.progress-bar-over").length === 0) {
					// Create bar and append it before the valid
					progressBar.before("<div class='progress-bar progress-bar-over progress-bar-danger progress-bar-striped active' style='max-width:100%;'><span class='over-budget-used-percent'></span></div>");
					// Change min width for the valid progress bar to make it disappear at 198-200%
					progressBar.css("min-width", 0);
				}
				var overProgressBar = divProgressBar.children(".progress-bar.progress-bar-over");
				var spanOverBudgetUsedPercent = $(overProgressBar).children(".over-budget-used-percent");
				overProgressBar.css("width", budgetUsedPercent - 100 + "%");
				spanOverBudgetUsedPercent.text(budgetUsedPercent + "%");

				// We change of span to show percent because 100-120 (approx) don't have place to write it in over bar
				if (budgetUsedPercent > 125) {
					spanBudgetUsedPercent.text("");
					spanOverBudgetUsedPercent.text(budgetUsedPercent + "%");
				} else {
					spanBudgetUsedPercent.text(budgetUsedPercent + "%");
					spanOverBudgetUsedPercent.text("");
				}

			} else {
				if (divProgressBar.children(".progress-bar.progress-bar-over").length > 0) {
					// In this case, progress bar was over 100 before, so we need to remove the over-progress-bar
					divProgressBar.children(".progress-bar.progress-bar-over").remove();
					// And also reset min width
					progressBar.css("min-width", "2em");
				}
				spanBudgetUsedPercent.text(budgetUsedPercent + "%");
				progressBar.css("width", budgetUsedPercent + "%");
			}
		},

		/**
		 * Method to update the list of energy sensors
		 */
		updateSensorsList: function () {
			var self = this;

			var energyDevices = devices.getDevicesByType(6);
			_.each(energyDevices, function (device) {
				$("#sensor-" + device.get('id')).prop('checked', _.contains(self.model.get("sensors"), (device.get('id'))));
			});
		},

		/**
		 * Method to update the list of history values
		 */
		updateHistory: function () {
			var self = this;
			var history = [];
			history = $.parseJSON(self.model.get("history"));

			// Clear existing history
			$("#history-list").empty();
			_.each(history, function (entry) {
				var unit = self.getUnit(entry.budgetUnit);
				var newEntry = "<span class='col-md-12'>";
				newEntry += new Date(entry.startDate).toLocaleString();
				newEntry += " - ";
				newEntry += new Date(entry.stopDate).toLocaleString();
				newEntry += " : ";
				newEntry += parseFloat(entry.energyDuringPeriod).toFixed(4);
				newEntry += unit.text;
				newEntry += " / ";
				newEntry += entry.budgetTotal;
				newEntry += unit.text;
				newEntry += "</span>";
				$("#history-list").append(newEntry);
			});
		},

		/**
		 * Method to update the dates
		 */
		updateDates: function () {
			var self = this;

			var spanDateFrom = $("#div-dates").children(".span-date-from");
			var spanDateUntil = $("#div-dates").children(".span-date-until");

			var intDateFrom = parseInt(self.model.get('startDate'));
			// At group creation, dates = -1
			if (intDateFrom < 1) {
				spanDateFrom.text("--/-- --:--:--");
				spanDateUntil.text("--/-- --:--:--");
			} else {
				var dateFrom = new Date(intDateFrom);
				spanDateFrom.text(self.buildDate(dateFrom));

				if (self.model.get('isMonitoring') === "true" || self.model.get('isMonitoring') === true) {
					spanDateUntil.text($.i18n.t("services.energy-monitoring.date.now"));
				} else {
					var dateUntil = new Date(parseInt(self.model.get('stopDate')));
					spanDateUntil.text(self.buildDate(dateUntil));
				}
			}
		},
		
		/**
		 * Method to build date : dd/mm (fr) mm/dd (others) hh:mm:ss
		 * @param date : Date to build
		 **/
		buildDate: function (date) {
			var dateString = "";
			if ($.i18n.lng() === "fr") {
				dateString += date.getDate();
				dateString += "/";
				dateString += date.getMonth();
			} else {
				dateString += date.getMonth();
				dateString += "/";
				dateString += date.getDate();
			}
			dateString += " ";
			dateString += date.toTimeString().split(" ")[0];

			return dateString;
		},

		/*
		 * Method to update the annotations
		 */
		updateAnnotations: function () {
			var self = this;

			var divAnnotations = $("#div-annotations-history");
			// Clear existing annotations
			divAnnotations.empty();
			// Reset input & button as initial
			$("#input-new-annotation").prop("value", "");
			$(".btn.add-annotation").prop("disabled", true);

			var annotationsCollection = $.parseJSON(self.model.get('annotations'));
			var annotationSorted = _.sortBy(annotationsCollection, function (annotationObj) {
				// "-" to get descending annotations
				return -parseInt(_.keys(annotationObj)[0]);
			});

			_.forEach(annotationSorted, function (annotation) {
				divAnnotations.append(self.buildOneAnnotation(annotation));
			});
		},

		/*
		 * Method to build one annotation
		 * @param annotationObj : Annotation to build
		 */
		buildOneAnnotation: function (annotationObj) {
			// Get the date which is the key of the object
			var dateInt = parseInt(_.keys(annotationObj)[0]);
			var date = new Date(dateInt);
			// Get the annotation which is the value of the object
			var annotation = _.values(annotationObj)[0];

			// Build html annotation element
			var htmlAnnotation = "<div id='annotation-" + dateInt + "' class='input-group'><input type='text' class='form-control'";
			htmlAnnotation += "value='" + date.toLocaleString() + " : " + annotation + "' disabled>";
			htmlAnnotation += " <span class='input-group-btn'><button class='btn btn-default delete-annotation' type='button'>-</button></span></div>";

			return htmlAnnotation;
		}


	});
	return EnergyMonitoringDetailsView
});