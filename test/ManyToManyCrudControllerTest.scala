import com.atsid.play.controllers.{ManyToManyCrudController, OneToManyCrudController, CrudController}
import com.avaje.ebean.{Expr, Junction, Ebean, Query}
import controllers.{TestManyToManyCrudController, TestOneToManyCrudController, TestCrudController}
import models.{TestJunctionModel, TestModel, TestOtherModel}
import utils.TestUtils
import scala.collection.JavaConverters._

/**
 * Created by davidtittsworth on 2/23/15.
 */
class ManyToManyCrudControllerTest extends BaseCrudControllerTest[TestOtherModel](classOf[TestOtherModel]) {

  override def getControllerInstance(): CrudController[TestOtherModel] = new TestManyToManyCrudController()
  override def getParentQuery: Query[TestOtherModel] = {
    val rightIds =
      Ebean.createQuery(classOf[TestJunctionModel]).where().eq("testModel.id", 1l).findList.asScala
      .map(n => TestUtils.getFieldValue(TestUtils.getFieldValue(n, "testOtherModel"), "id").asInstanceOf[Long])
      .asJava
    return Ebean.createQuery(classOf[TestOtherModel]).where().in("id", rightIds).query
  }
  override def getNestedField() = "testYetAnotherModel.id";

  override def list(controller: CrudController[_], offset:Integer = 0, count:Integer = null, orderBy:String = null, fields:String = null, fetches:String  = null, q:String = null, userId: java.lang.Long = 1) : play.mvc.Result = {
    controller.asInstanceOf[ManyToManyCrudController[TestModel, TestJunctionModel, TestOtherModel]].list(1l, offset,count,orderBy,fields,fetches,q).get(1000);
  }

  // Helpers
  override def read(controller: CrudController[_], id: Long = -1, fields:String = null, fetches:String  = null) : play.mvc.Result = {
    controller.asInstanceOf[ManyToManyCrudController[TestModel, TestJunctionModel, TestOtherModel]].read(1l, id, fields, fetches).get(1000);
  }

  override def delete(controller: CrudController[_], id: java.lang.Long, userId:java.lang.Long = 1) : play.mvc.Result = {
    controller.asInstanceOf[ManyToManyCrudController[TestModel, TestJunctionModel, TestOtherModel]].delete(1l, id).get(1000);
  }

  override def deleteBulk(controller: CrudController[_], userId:java.lang.Long = 1) : play.mvc.Result = {
    controller.asInstanceOf[ManyToManyCrudController[TestModel, TestJunctionModel, TestOtherModel]].deleteBulk(1l).get(1000);
  }

  override def create(controller: CrudController[_]) : play.mvc.Result = {
    controller.asInstanceOf[ManyToManyCrudController[TestModel, TestJunctionModel, TestOtherModel]].create(1l).get(1000);
  }

  override def update(controller: CrudController[_], id: Long) : play.mvc.Result = {
    controller.asInstanceOf[ManyToManyCrudController[TestModel, TestJunctionModel, TestOtherModel]].update(1l, id).get(1000);
  }
}
