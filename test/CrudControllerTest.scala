import com.atsid.play.controllers.CrudController
import controllers.TestCrudController
import models.TestModel

class CrudControllerTest extends BaseCrudControllerTest[TestModel](classOf[TestModel]) {
  override def getUniqueFieldName(): String =  "randomFieldCrud";
  override def getControllerInstance(): CrudController[TestModel] = new TestCrudController()
  override def getNestedField() = "nestedModel.id";
}
