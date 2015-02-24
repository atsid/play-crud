import com.atsid.play.controllers.{OneToManyCrudController, CrudController}
import com.avaje.ebean.Query
import controllers.{TestOneToManyCrudController, TestCrudController}
import models.{TestModel, TestNestedModel}

/**
 * Created by davidtittsworth on 2/23/15.
 */
class OneToManyCrudControllerTest extends BaseCrudControllerTest[TestNestedModel](classOf[TestNestedModel]) {

  override def getControllerInstance(): CrudController[TestNestedModel] = new TestOneToManyCrudController()
  override def getParentQuery: Query[TestNestedModel] = {
    super.getParentQuery.where().eq("testModel.id", 1).query;
  }

  override def getNestedField() = "testModel.id";

  override def list(controller: CrudController[_], offset:Integer = 0, count:Integer = null, orderBy:String = null, fields:String = null, fetches:String  = null, q:String = null, userId: java.lang.Long = 1) : play.mvc.Result = {
    controller.asInstanceOf[OneToManyCrudController[TestModel, TestNestedModel]].list(1l, offset,count,orderBy,fields,fetches,q).get(1000);
  }

  // Helpers
  override def read(controller: CrudController[_], id: Long = -1, fields:String = null, fetches:String  = null) : play.mvc.Result = {
    controller.asInstanceOf[OneToManyCrudController[TestModel, TestNestedModel]].read(1l, id, fields, fetches).get(1000);
  }

  override def delete(controller: CrudController[_], id: java.lang.Long, userId:java.lang.Long = 1) : play.mvc.Result = {
    controller.asInstanceOf[OneToManyCrudController[TestModel, TestNestedModel]].delete(1l, id).get(1000);
  }

  override def deleteBulk(controller: CrudController[_], userId:java.lang.Long = 1) : play.mvc.Result = {
    controller.asInstanceOf[OneToManyCrudController[TestModel, TestNestedModel]].deleteBulk(1l).get(1000);
  }

  override def create(controller: CrudController[_]) : play.mvc.Result = {
    controller.asInstanceOf[OneToManyCrudController[TestModel, TestNestedModel]].create(1l).get(1000);
  }

  override def update(controller: CrudController[_], id: Long) : play.mvc.Result = {
    controller.asInstanceOf[OneToManyCrudController[TestModel, TestNestedModel]].update(1l, id).get(1000);
  }
}
