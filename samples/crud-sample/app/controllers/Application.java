package controllers;

import play.*;
import play.mvc.*;
import com.atsid.play.controllers.CrudController;
import models.Task;

import views.html.*;

public class Application extends CrudController<Task> {

    Application () {
        super(Task.class);
    }

    public static Result index() {
        return ok(index.render("Your new applicaton is ready."));
    }

}
