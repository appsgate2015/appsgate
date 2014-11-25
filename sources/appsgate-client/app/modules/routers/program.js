define([
    "app",
    "views/program/menu",
    "views/program/reader",
    "views/program/editor"
], function(App, ProgramMenuView, ProgramReaderView, ProgramEditorView) {

    var ProgramRouter = {};
    // router
    ProgramRouter = Backbone.Router.extend({
        routes: {
            "programs": "list",
            "programs/:id": "reader",
            "programs/editor/:id": "editor",
        },
        list: function() {

            // display the side menu
            appRouter.showMenuView(new ProgramMenuView());

            $(".nav-item").removeClass("active");
            $("#programs-nav").addClass("active");

            // update the url if there is at least one program
            if (programs.length > 0) {
              this.reader(programs.at(0).get("id"));
            }

            dispatcher.trigger("router:loaded");
        },
        reader: function(id) {

            // in the case the program isn't found in the side-menu, re-render it
            if($(".aside-menu #side-" + id).length == 0){
              this.list();
            }

            // display the requested program
            appRouter.showDetailsView(new ProgramReaderView({model: programs.get(id)}));

            // update the url
            appRouter.navigate("#programs/" + id);

            appRouter.currentMenuView.updateSideMenu();
        },
        editor: function(id) {
            // remove and unbind the current view for the menu
            if (appRouter.currentMenuView) {
                appRouter.currentMenuView.close();
            }
            if (appRouter.currentView) {
                appRouter.currentView.close();
            }

            $("#main").html(appRouter.navbartemplate());

            $(".nav-item").removeClass("active");
            $("#programs-nav").addClass("active");

            appRouter.navigate("#programs/editor/" + id);

            appRouter.currentView = new ProgramEditorView({el:$("#main"),model: programs.get(id)});
            appRouter.currentView.render();

            $("#main").append(appRouter.circlemenutemplate());

            // initialize the circle menu
            $(".controlmenu").circleMenu({
                trigger: "click",
                item_diameter: 50,
                circle_radius: 75,
                direction: 'top-right'
            });

            $(document).i18n();
        }

    });
    return ProgramRouter;
});
