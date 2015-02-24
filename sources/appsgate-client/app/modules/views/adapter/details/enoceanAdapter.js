define([
  "app"

  ], function(App) {

    var UbikitAdapter = {};
    // detailed view for TTS Service
    TTSView = Backbone.View.extend({



        // map the events and their callback
        events: {

        },

      initialize: function() {
        var self = this;

           TTSView.__super__.initialize.apply(this, arguments);

        $.extend(self.__proto__.events, TTSView.__super__.events);
      },

        onItemsChangedModel: function() {
            console.log("onItemsChanged");
            this.renderTTS(this.model.getTTSItems());

        },
        onSpeedChangedModel: function() {
            console.log("onSpeedChanged");
            this.renderSpeed(this.model.get("speed"));
        },
        onVoiceChangedModel: function() {
            console.log("onVoiceChanged");
            this.renderVoices(this.model.get("voice"));
        },

      autoupdate: function() {
          TTSView.__super__.autoupdate.apply(this);

        // translate the view
        this.$el.i18n();
      },

        onDeleteTTSUI: function() {
            var tts = $(".select-tts").val();
            this.model.deleteTTSItem(tts);
        },


        onChangeVoiceUI: function(event) {
            console.log("onChangeVoice");
            var desc = event.currentTarget.value;
//            $(".select-gender").append(currentTarget.value[1]);

            this.model.setVoice(desc);
        },

        onChangeSpeedUI   : function(event) {
            var speed = event.currentTarget.value;
            this.model.setSpeed(speed);
        },

        onChangeLangUI: function(event) {
            var lang = event.currentTarget.value;
            var voices = this.model.getVoices();
            this.model.setVoice(voices[lang][0][2]);
            $(".select-voice option").remove();
        },

        onAddTTSUI: function(event) {
            var text = $(".new-tts").val();
            this.model.prepareTTS(text,
                this.model.getVoice(),
                this.model.getSpeed());
            $(".new-tts").val("");
        },


        /**
         * This function does a partial rendering of the view
         * only the voice selection part is changed
         */
        renderVoices: function(voice) {
            var voices = this.model.getVoices();
            var lang = this.model.getLangFromVoice(voice);

            this.renderLang(voice);

            $(".select-voice option").remove();
            for(var i = 0; i< voices[lang].length; i++) {

                $(".select-voice").append(this.tplTTSVoice({
                    selectedVoice: voice,
                    voice: voices[lang][i][2],
                    gender: voices[lang][i][1],
                    country: voices[lang][i][0]
                }));
            }
            $(".select-lang").i18n();
        },
        renderLang: function(voice) {
            $(".select-lang option").remove();
            var voices = this.model.getVoices();
            var s = this.model.getLangFromVoice(voice);
            for(i in voices) {
                $(".select-lang").append(this.tplTTSLang({
                    selectedLang: s,
                    lang: i
                }));
            }
            $(".select-lang").i18n();

        },

        /**
         * This function does a partial rendering of the view
         * only the voice selection part is changed
         * @param speed
         */
        renderSpeed: function(speed) {
            $(".select-speed").val(speed);
            $(".select-speed").i18n();
        },

        /**
         * This function does a partial rendering of the view
         * only the TTS Items selection part is changed
         * @param speed
         */
        renderTTS: function(ttsItems) {
            $(".select-tts option").remove();
            for(var i=0; i<ttsItems.length; i++) {
                $(".select-tts").append(this.tplTTSItem({
                    book_id: ttsItems[i].book_id,
                    text: ttsItems[i].text,
                    voice: ttsItems[i].voice,
                    speed: ttsItems[i].speed
                }));
            }
            $(".select-tts").i18n();
        },


      /**
      * Render the detailed view of the service
      */
      render: function() {
        var self = this;

        if (!appRouter.isModalShown) {
            console.log("current Voice : ",this.model.getVoice());

            this.$el.html(this.tplTTS({
                service: this.model,
                sensorImg: ["app/img/tts.png"],
                sensorType: $.i18n.t("services.tts.name.singular"),
            }));

            this.renderVoices(this.model.getVoice());
            this.renderSpeed(this.model.getSpeed());
            this.renderTTS(this.model.getTTSItems());

            this.resize($(".scrollable"));
          // translate the view
          this.$el.i18n();
          return this;
        }
      }
    });
    return TTSView
  });
