package controllers;

import com.atsid.play.controllers.CrudController;
import models.TestModel;

/**
 * Created by davidtittsworth on 2/20/15.
 */
public class TestCrudController extends CrudController<TestModel> {
    public TestCrudController() {
        super(TestModel.class);
    }
}
