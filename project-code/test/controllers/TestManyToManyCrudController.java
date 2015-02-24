package controllers;

import com.atsid.play.controllers.ManyToManyCrudController;
import com.atsid.play.controllers.OneToManyCrudController;
import models.TestJunctionModel;
import models.TestModel;
import models.TestNestedModel;
import models.TestOtherModel;

/**
 * Created by davidtittsworth on 2/23/15.
 */
public class TestManyToManyCrudController extends ManyToManyCrudController<TestModel, TestJunctionModel, TestOtherModel> {
    public TestManyToManyCrudController() {
        super(TestModel.class, TestJunctionModel.class, TestOtherModel.class, "testModel", "testOtherModel");
    }
}
