import com.atsid.play.controllers.CrudController
import controllers.TestCrudController
import models.TestModel
import play.mvc.Result
import play.test.Helpers._

class CrudControllerTest extends BaseCrudControllerTest[TestModel](classOf[TestModel]) {
  override def getUniqueFieldName(): String =  "randomFieldCrud";
  override def getControllerInstance(): CrudController[TestModel] = new TestCrudController()
  override def getNestedField() = "nestedModel.id";

  "Crud Controller" should {

    /**
     * The problem was that the validation would show errors for dates, but only if
     * another property on the model was also failing.  This was because when isModelValid is called, it uses Util.toJson to validate.
     * This method works OK with our dates, but it would then would call CrudResults.validationError, which would then also call
     * Form.bindFromRequest, but would call it with Json.toJson, which doesn't serialize dates the same.  Json.toJson uses the Unix Timestamp format
     * which clashes with the Formats.DateTime("yyyy-mm-dd") on properties.
     */
    "not fail validation for dates" in new HttpContextBeforeAfter(testSql)  {
      val item = new TestModel();
      item.myTimeStamp = new java.util.Date();

      setRequestBody(item);

      val result: Result = create(getControllerInstance());

      // Shouldn't return a validation error for myTimestamp, since we passed it a legit value
      contentAsString(result) must not contain("myTimeStamp");

      // This is the only valid error
      contentAsString(result) must contain("myString");
    }
  }
}
