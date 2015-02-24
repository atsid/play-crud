package controllers;

import com.atsid.play.controllers.OneToManyCrudController;
import models.TestModel;
import models.TestNestedModel;

/**
 * Created by davidtittsworth on 2/23/15.
 */
public class TestOneToManyCrudController extends OneToManyCrudController<TestModel, TestNestedModel> {
    public TestOneToManyCrudController() {
        super(TestModel.class, TestNestedModel.class, "testModel");
    }
}
