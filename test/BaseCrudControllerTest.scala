import java.text.SimpleDateFormat

import com.atsid.play.common.SchemaBuilder
import com.atsid.play.controllers.{Util, CrudController}
import com.atsid.play.models.schema.{FieldType, FieldDescriptor}
import com.avaje.ebean.{Ebean, Query}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import play.db.ebean.Model
import play.libs.Json
import play.mvc.{Http, Result}
import java.lang.reflect.Field
import java.util._
import play.test.Helpers._
import utils.{ScriptRunnerBeforeAfter, TestUtils}
import scala.collection.JavaConverters._
import scala.collection.immutable.HashSet;

abstract class BaseCrudControllerTest[M <: Model] extends Specification  {

  sequential

//  protected var find: Model.Finder[Long, M] = null
  private var modelArrayClass: Class[_] = null
  private var modelClass: Class[_] = null
  private var modelFields: Array[Field] = null
  private var schema: Seq[FieldDescriptor] = null

  def this(modelClass: Class[M]) {
    this();
    this.modelClass = modelClass
//    this.find = new Model.Finder[Long, M](classOf[Long], modelClass)
    this.modelArrayClass = java.lang.reflect.Array.newInstance(modelClass, 0).getClass
    this.modelFields = modelClass.getFields
    this.schema = SchemaBuilder.buildSchema(modelClass).asScala.toSeq
  }

  var dateProp = "myDate";
  var dateTimeProp = "myDateTime";

  var testSql =

    """
      INSERT INTO test_model(id, my_string) VALUES
        (1, '1'),
        (2, '2'),
        (3, '3');

      INSERT INTO test_nested_model(id, my_string, test_model_id) VALUES
        (1, '1', 1),
        (2, '2', 2),
        (3, '3', 3),
        (4, '4', 1),
        (5, '5', 1),
        (6, '6', 1);

      insert into test_yet_another_model(id) VALUES
        (1),
        (2),
        (3),
        (4),
        (5),
        (6);

      INSERT INTO test_other_model(id, my_string, test_yet_another_model_id) VALUES
        (1, '1', 1),
        (2, '2', 2),
        (3, '3', 3),
        (4, '4', 4),
        (5, '5', 5),
        (6, '6', 6);

      INSERT INTO test_junction_model(id, test_other_model_id, test_model_id) VALUES
        (1, 1, 1),
        (2, 2, 2),
        (3, 3, 3),
        (4, 4, 1),
        (5, 5, 1),
        (6, 6, 1);
    """.stripMargin

  var withDatesSql =
    """
      INSERT INTO test_model(id, my_date, my_date_time) VALUES
        (1, '2014-03-22T17:00:00.111', '2014-03-22T17:00:00.111'),
        (2, '2014-03-23T17:00:00.111', '2014-03-23T17:00:00.111'),
        (3, '2014-03-24T17:00:00.111', '2014-03-24T17:00:00.111'),
        (4, '2014-03-22T18:00:00.111', '2014-03-22T12:00:00.111'),
        (5, '2014-03-23T15:00:00.111', '2014-03-23T15:00:00.111'),
        (6, '2014-03-24T16:00:00.111', '2014-03-24T20:00:00.111');

      INSERT INTO test_nested_model(id, my_date, my_date_time, test_model_id) VALUES
        (1, '2014-03-22T17:00:00.111', '2014-03-22T17:00:00.111', 1),
        (2, '2014-03-23T17:00:00.111', '2014-03-23T17:00:00.111', 2),
        (3, '2014-03-24T17:00:00.111', '2014-03-24T17:00:00.111', 3),
        (4, '2014-03-22T18:00:00.111', '2014-03-22T12:00:00.111', 4),
        (5, '2014-03-23T15:00:00.111', '2014-03-23T15:00:00.111', 5),
        (6, '2014-03-24T16:00:00.111', '2014-03-24T20:00:00.111', 6),
        (7, '2014-03-22T17:00:00.111', '2014-03-22T17:00:00.111', 1),
        (8, '2014-03-23T17:00:00.111', '2014-03-23T17:00:00.111', 1),
        (9, '2014-03-24T17:00:00.111', '2014-03-24T17:00:00.111', 1),
        (10, '2014-03-22T18:00:00.111', '2014-03-22T12:00:00.111', 1),
        (11, '2014-03-23T15:00:00.111', '2014-03-23T15:00:00.111', 1),
        (12, '2014-03-24T16:00:00.111', '2014-03-24T20:00:00.111', 1);

      INSERT INTO test_other_model(id, my_date, my_date_time) VALUES
        (1, '2014-03-22T17:00:00.111', '2014-03-22T17:00:00.111'),
        (2, '2014-03-23T17:00:00.111', '2014-03-23T17:00:00.111'),
        (3, '2014-03-24T17:00:00.111', '2014-03-24T17:00:00.111'),
        (4, '2014-03-22T18:00:00.111', '2014-03-22T12:00:00.111'),
        (5, '2014-03-23T15:00:00.111', '2014-03-23T15:00:00.111'),
        (6, '2014-03-24T16:00:00.111', '2014-03-24T20:00:00.111'),
        (7, '2014-03-22T17:00:00.111', '2014-03-22T17:00:00.111'),
        (8, '2014-03-23T17:00:00.111', '2014-03-23T17:00:00.111'),
        (9, '2014-03-24T17:00:00.111', '2014-03-24T17:00:00.111'),
        (10, '2014-03-22T18:00:00.111', '2014-03-22T12:00:00.111'),
        (11, '2014-03-23T15:00:00.111', '2014-03-23T15:00:00.111'),
        (12, '2014-03-24T16:00:00.111', '2014-03-24T20:00:00.111');

      INSERT INTO test_junction_model(id, test_other_model_id, test_model_id) VALUES
        (1, 1, 1),
        (2, 2, 2),
        (3, 3, 3),
        (4, 4, 4),
        (5, 5, 5),
        (6, 6, 6),
        (7, 7, 1),
        (8, 8, 1),
        (9, 9, 1),
        (10, 10, 1),
        (11, 11, 1),
        (12, 12, 1);

    """.stripMargin


  "CrudController" should {

    "testList" in new HttpContextBeforeAfter(testSql) {
      val result: Result = list(getControllerInstance(), 0, 100000, "id");

      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: JsonNode = Json.parse(contentAsString(result))
      val returned: Array[AnyRef] = Json.fromJson(node.get("data"), modelArrayClass).asInstanceOf[Array[AnyRef]]
      assertEquals(getParentQuery.orderBy("id asc").findList.toArray, returned)
    }

    "testListCount" in new HttpContextBeforeAfter(testSql) {
      val result: Result = list(getControllerInstance(), 0, 2);
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: JsonNode = Util.parseJson(contentAsString(result))
      val items: List[M] = Util.fromJson(node.get("data"), classOf[List[M]])
      items.size must be equalTo(2)
    }

    "testListWithAllStringFields" in new HttpContextBeforeAfter(testSql) {
      val fields: Seq[String] = modelFields.filter(n => n.getType.equals(classOf[String])).map(_.getName).toSeq;
      val result: Result = list(getControllerInstance(), 0, 2, fields = fields.mkString(","));
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: ArrayNode = Util.parseJson(contentAsString(result)).get("data").asInstanceOf[ArrayNode];

      val firstItem = node.get(0).asInstanceOf[ObjectNode]
      val fieldsToCheck : Seq[String] = (fields ++ Seq[String]("id"));
      val returnedProps = firstItem.fields().asScala.map(n => n.getKey).toSet.diff(fieldsToCheck.toSet);

      returnedProps.size must be equalTo(0)
    }

    "testListWithOneField" in new HttpContextBeforeAfter(testSql) {
      val fields: Seq[String] = Seq(modelFields.filter(n => n.getType.equals(classOf[String])).head.getName);
      val result: Result = list(getControllerInstance(), 0, 2, fields = fields.mkString(","));
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: ArrayNode = Util.parseJson(contentAsString(result)).get("data").asInstanceOf[ArrayNode];

      val firstItem = node.get(0).asInstanceOf[ObjectNode]
      val fieldsToCheck : Seq[String] = (fields ++ Seq[String]("id"));
      val returnedProps = firstItem.fields().asScala.map(n => n.getKey).toSet.diff(fieldsToCheck.toSet[String]);

      returnedProps.size must be equalTo(0)
    }

    "testListWithUniqueFields" in new HttpContextBeforeAfter(testSql) {
      val result: Result = list(getControllerInstance(), 0, 2, fields = getUniqueFieldName);
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: ArrayNode = Util.parseJson(contentAsString(result)).get("data").asInstanceOf[ArrayNode];

      val firstItem = node.get(0).asInstanceOf[ObjectNode]
      val returnedProps = firstItem.fields().asScala.map(n => n.getKey).toSet.diff(HashSet[String]("id", getUniqueFieldName()));

      returnedProps.size must be equalTo(0)
    }

    "testListOffset" in new HttpContextBeforeAfter(testSql) {
      val result: Result = list(getControllerInstance(), 1, 1, "id");
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: JsonNode = Util.parseJson(contentAsString(result))
      val items: Array[M] = Util.fromJson(node.get("data"), modelArrayClass).asInstanceOf[Array[M]]
      items.length must be equalTo(1)

      var expected = TestUtils.getFieldValue(getParentQuery.findList.get(1), "id");
      var actual = TestUtils.getFieldValue(items(0), "id");

      expected must be equalTo actual
    }

    "testListOrderByDesc" in new HttpContextBeforeAfter(testSql) {
      val result: Result = list(getControllerInstance(), 0, 2, "id desc");
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: JsonNode = Util.parseJson(contentAsString(result))
      val items: Array[M] = Util.fromJson(node.get("data"), modelArrayClass).asInstanceOf[Array[M]]
      items.length must be equalTo(2)

      var expected = getParentQuery.findList.toArray;

      assertEquals(Array[AnyRef](expected(expected.length - 1), expected(expected.length - 2)), items.asInstanceOf[Array[AnyRef]])
    }

    "testListOrderByAsc" in new HttpContextBeforeAfter(testSql) {
      val result: Result = list(getControllerInstance(), 0, 2, "id asc");
      status(result) must be equalTo(200);
      contentType(result) must be equalTo("application/json");

      val node: JsonNode = Util.parseJson(contentAsString(result))
      val items: Array[M] = Util.fromJson(node.get("data"), modelArrayClass).asInstanceOf[Array[M]]
      items.length must be equalTo(2)

      var expected = getParentQuery.findList.toArray;

      assertEquals(Array[AnyRef](expected(0), expected(1)), items.asInstanceOf[Array[AnyRef]])
    }

    "testCreate" in new HttpContextBeforeAfter(testSql) {
      val item = createModel();
      setRequestBody(item);

      val result: Result = create(getControllerInstance());

      val resultStatus = status(result);
      resultStatus must be equalTo 201

      contentType(result) must be equalTo("application/json");

      val node: JsonNode = Util.parseJson(contentAsString(result))
      val resultItem: M = Util.fromJson(node.get("data"), modelClass).asInstanceOf[M]

      assertEquals(Array[AnyRef](resultItem), Array[AnyRef](mockBody), false)
      getParentQuery.where().eq("id", TestUtils.getFieldValue(resultItem, "id").asInstanceOf[Long]).findUnique mustNotEqual(null)
    }

    "testBulkCreate" in new HttpContextBeforeAfter(testSql) {
        val item = createModel();
        val item2 = createModel();
        val list = new java.util.ArrayList[M]();
        list.add(item);
        list.add(item2);
        setRequestBody(list);

        val result: Result = create(getControllerInstance());

        val resultStatus = status(result);
        resultStatus must be equalTo 201

        contentType(result) must be equalTo("application/json");

        val node: JsonNode = Util.parseJson(contentAsString(result))

        val resultItems =
          node.get("data").asInstanceOf[ArrayNode].iterator().asScala.map(n => Util.fromJson(n, modelClass).asInstanceOf[M]).toSeq;

        val resultItem1Id = getParentQuery.where().eq("id", TestUtils.getFieldValue(resultItems(0), "id").asInstanceOf[Long]).findUnique
        val resultItem2Id = getParentQuery.where().eq("id", TestUtils.getFieldValue(resultItems(1), "id").asInstanceOf[Long]).findUnique

        resultItem1Id  mustNotEqual null
        resultItem2Id  mustNotEqual null
    }

    "testRead" in new HttpContextBeforeAfter(testSql) {
        val existingId: Long = TestUtils.getFieldValue(getParentQuery.findList.get(0), "id").asInstanceOf[Long]
        val result = read(getControllerInstance(), existingId);

        val resultStatus = status(result);
        resultStatus must be equalTo 200

        contentType(result) must be equalTo("application/json");
        val node: JsonNode = Util.parseJson(contentAsString(result))
        val item: M = Util.fromJson(node.get("data"), modelClass).asInstanceOf[M]

        TestUtils.getFieldValue(item, "id").asInstanceOf[Long] must be equalTo(existingId)
    }

    "testBadRead" in new HttpContextBeforeAfter(testSql) {
      val result = read(getControllerInstance(), 981984796431l);

      val resultStatus = status(result);
      resultStatus must be equalTo 404
    }

    "testUpdate" in new HttpContextBeforeAfter(testSql) {
        var existingId =
          getParentQuery.findList().asScala.map(n => TestUtils.getFieldValue(n, "id").asInstanceOf[Long]).head
        val item = getParentQuery.where().eq("id", existingId).findUnique
        val random: String = "TEST" + new Random().nextInt(200)
        var foundFields: Boolean = false;
        for (f <- modelFields) {
          if (f.getType == classOf[java.lang.String]) {
            TestUtils.setFieldValue(item, f, random)
            foundFields = true;
          }
        }

        foundFields must be equalTo(true);

        setRequestBody(item);
        val result: Result = update(getControllerInstance(), existingId);

        val resultStatus = status(result);
        resultStatus must be equalTo 200
        val checkItem = getParentQuery.where().eq("id", existingId).findUnique;
        assertEquals(Array[AnyRef](item), Array[AnyRef](checkItem))
    }

    "testDelete" in new HttpContextBeforeAfter(testSql) {
      val item = createModel();
      setRequestBody(item);

      var result: Result = create(getControllerInstance());
      val node: JsonNode = Util.parseJson(contentAsString(result))
      val resultItem: M = Util.fromJson(node.get("data"), modelClass).asInstanceOf[M]
      val id: Long = TestUtils.getFieldValue(resultItem, "id").asInstanceOf[Long]

      result = delete(getControllerInstance(), id);
      status(result) must be equalTo(204)

      getParentQuery.where().eq("id", id).findUnique must be equalTo(null.asInstanceOf[M])
    }

    "testDeleteExisting" in new HttpContextBeforeAfter(testSql) {
      var existingId =
        getParentQuery.findList().asScala.map(n => TestUtils.getFieldValue(n, "id").asInstanceOf[Long]).head

      val result: Result = delete(getControllerInstance(), existingId);
      status(result) must be equalTo(204)

      getParentQuery.where().eq("id", existingId).findUnique must be equalTo(null.asInstanceOf[M])
    }

    "testBulkDelete" in new HttpContextBeforeAfter(testSql) {

      // Take the first two items
      var ids =
        getParentQuery.findList().asScala.map(n => TestUtils.getFieldValue(n, "id").asInstanceOf[Long]).take(2);

      setRequestBody(ids.toList.asJavaCollection);

      val result : Result = deleteBulk(getControllerInstance());
      status(result) must be equalTo(204)

      getParentQuery.where().eq("id", ids(0)).findUnique must be equalTo(null.asInstanceOf[M])
      getParentQuery.where().eq("id", ids(1)).findUnique must be equalTo(null.asInstanceOf[M])
    }

    "testDateEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-22", "", expectedCompareValues = Seq(0));
    }

    "testDateLessThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", "<", expectedCompareValues = Seq(-1));
    }

    "testDateGreaterThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", ">", expectedCompareValues = Seq(1));
    }

    "testDateGreaterThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", ">=", expectedCompareValues = Seq(0, 1));
    }

    "testDateLessThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", "<=", expectedCompareValues = Seq(0, -1));
    }

    "testDateEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-22", "", expectedCompareValues = Seq(0));
    }

    "testDateLessThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", "<", expectedCompareValues = Seq(-1));
    }

    "testDateGreaterThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", ">", expectedCompareValues = Seq(1));
    }

    "testDateGreaterThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", ">=", expectedCompareValues = Seq(0, 1));
    }

    "testDateLessThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateProp, "2014-03-23", "<=", expectedCompareValues = Seq(0, -1));
    }

    "testDateTimeEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-22T17:00:00.111", "", expectedCompareValues = Seq(0));
    }

    "testDateTimeLessThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", "<", expectedCompareValues = Seq(-1));
    }

    "testDateTimeGreaterThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", ">", expectedCompareValues = Seq(1));
    }

    "testDateTimeGreaterThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", ">=", expectedCompareValues = Seq(0, 1));
    }

    "testDateTimeLessThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", "<=", expectedCompareValues = Seq(0, -1));
    }

    "testDateTimeEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-22T17:00:00.111", "", expectedCompareValues = Seq(0));
    }

    "testDateTimeLessThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", "<", expectedCompareValues = Seq(-1));
    }

    "testDateTimeGreaterThanQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", ">", expectedCompareValues = Seq(1));
    }

    "testDateTimeGreaterThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", ">=", expectedCompareValues = Seq(0, 1));
    }

    "testDateTimeLessThanOrEqualToQuery" in new HttpContextBeforeAfter(withDatesSql) {
      doDateQueryTest(dateTimeProp, "2014-03-23T17:00:00.111", "<=", expectedCompareValues = Seq(0, -1));
    }

    "testNonNestedNumberEqualQuery" in new HttpContextBeforeAfter(testSql) {
      doQueryTest("id:1", (item: M) => TestUtils.getFieldValue(item, "id").asInstanceOf[Long] != 1l, true)
    }
    "testNonNestedNumberNotEqualQuery" in new HttpContextBeforeAfter(testSql) {
      doQueryTest("id:!1", (item: M) => TestUtils.getFieldValue(item, "id").asInstanceOf[Long] == 1l, true)
    }
    "testNonNestedNumberGreaterThanQuery" in new HttpContextBeforeAfter(testSql) {
      doQueryTest("id:>1", (item: M) => TestUtils.getFieldValue(item, "id").asInstanceOf[Long] <= 1l, true)
    }
    "testNonNestedNumberGreaterThanEqualQuery" in new HttpContextBeforeAfter(testSql) {
      doQueryTest("id:>=2", (item: M) => TestUtils.getFieldValue(item, "id").asInstanceOf[Long] < 2l, true)
    }
    "testNonNestedNumberLessThanQuery" in new HttpContextBeforeAfter(testSql) {
      doQueryTest("id:<2", (item: M) => TestUtils.getFieldValue(item, "id").asInstanceOf[Long] >= 2l, true)
    }
    "testNonNestedNumberLessThanEqualQuery" in new HttpContextBeforeAfter(testSql) {
      doQueryTest("id:<=2", (item: M) => TestUtils.getFieldValue(item, "id").asInstanceOf[Long] > 2l, true)
    }
//
//    "Not allow expressions in the query string" in new HttpContextBeforeAfter(testSql) {
//      setRequestBody(Seq[Long](13371337l).toArray);
//
//      // Make the controller call
//      val result = list(getControllerInstance(), q = "id:13371336+1");
//
//      val resultStatus = status(result);
//      resultStatus must be greaterThanOrEqualTo 400
//      contentAsString(result) must contain ("Invalid query")
//    }

    "Fail if passed a bad field in the query string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), q = "BADFIELD:1")  must throwA(message = "BADFIELD");
    }

    "Fail if passed a bad nested field in the query string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), q = "nestedModel.BADFIELD:1") must throwA(message = "BADFIELD");
    }

    "Fail if passed a bad field in the orderby string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), orderBy = "BADFIELD") must throwA(message = "BADFIELD");
    }

    "Not Fail if passed a nested field in the orderby string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      val result = list(getControllerInstance(), orderBy = getNestedField);

      val resultStatus = status(result);
      resultStatus must be greaterThanOrEqualTo 200
    }

    "Not Fail if passed multiple fields in the orderby string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      val result = list(getControllerInstance(), orderBy = getNestedField + ",id");

      val resultStatus = status(result);
      resultStatus must be greaterThanOrEqualTo 200
    }

    "Not Fail if passed multiple fields in the orderby string with directions" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      val result = list(getControllerInstance(), orderBy = "id asc," + getNestedField + " desc");

      val resultStatus = status(result);
      resultStatus must be greaterThanOrEqualTo 200
    }

    "Fail if passed a bad field in the fields string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), fields = "BADFIELD") must throwA(message = "BADFIELD");;
    }

    "Fail if passed a bad field in the fields string, with multiple fields" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), fields = "id, BADFIELD") must throwA(message = "BADFIELD");
    }

    "Fail if passed a bad fetch in the fetches string" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), fetches = "BADFIELD") must throwA(message = "BADFIELD");;
    }

    "Fail if passed a bad fetch in the fetches string, with multiple fetches" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), fetches = "BADFIELD, nestedModel") must throwA(message = "BADFIELD");
    }

    "Fail if passed a bad field in the field string, with fetches" in new HttpContextBeforeAfter(testSql) {
      // Make the controller call
      list(getControllerInstance(), fields = "BADFIELD.id", fetches = "BADFIELD, nestedModel") must throwA(message = "BADFIELD");
    }

    "Fail if passed a bad nested field in the field string, with fetches" in new HttpContextBeforeAfter(testSql) {
      list(getControllerInstance(), fields = "nestedModel.BADFIELD", fetches = "nestedModel") must throwA(message = "BADFIELD");
    }

    "Fail if passed a bad nested fetch in the fetches string, with fields" in new HttpContextBeforeAfter(testSql) {
      list(getControllerInstance(), fields = "nestedModel.BADFIELD,nestedModel.id", fetches = "BADFIELD,nestedModel") must throwA(message = "BADFIELD");
    }
  }

  protected def doDateQueryTest(prop: String, dateToCheck: String, method: String, expectedCompareValues: Seq[Integer]): Unit = {
    doFormattedDateQueryTest(prop, "yyyy-MM-dd", dateToCheck, method, expectedCompareValues);
  }

  protected def doDateTimeQueryTest(prop: String, dateToCheck: String, method: String, expectedCompareValues: Seq[Integer]): Unit = {
//    '2014-03-22T17:00:00.111'
    doFormattedDateQueryTest(prop, "yyyy-MM-ddTHH:mm:ss.SSS", dateToCheck, method, expectedCompareValues);
  }

  protected def doFormattedDateQueryTest(prop: String, strDateFormat:String, dateToCheck: String, method: String, expectedCompareValues: Seq[Integer]): Unit = {
    val dateFormat = new SimpleDateFormat(strDateFormat);
    doQueryTest(prop + ":" + method + dateToCheck, (item: M) => {
      val actualDate = dateFormat.parse(dateFormat.format(TestUtils.getFieldValue(item, prop).asInstanceOf[java.util.Date]));
      !expectedCompareValues.contains(actualDate.compareTo(dateFormat.parse(dateToCheck)));
    })
  }


  /**
   * Performs a query test
   * @param query
   * @param isInvalid returns true if the given item is an invalid item
   */
  protected def doQueryTest(query: String, isInvalid: (M) => Boolean, canBeEmpty: Boolean = false) {
      val result: Result = list(getControllerInstance(), offset = 0, count = 2, q = query);
      status(result) must be equalTo(200)
      contentType(result) must be equalTo("application/json")
      val node: JsonNode = Json.parse(contentAsString(result))
      val returned: Array[M] = Json.fromJson(node.get("data"), modelArrayClass).asInstanceOf[Array[M]]
      var foundInvalid: Boolean = returned.find(n => isInvalid(n)).getOrElse(null) != null;

      if (!canBeEmpty) {
          returned.length must be greaterThan(0);
      }

      foundInvalid must be equalTo(false);
  }

  /**
   * Gets a list of the child items
   * @return
   */
  protected def getParentQuery: Query[M] = {
    var q: Query[M] = Ebean.createQuery(modelClass).asInstanceOf[Query[M]];
    for (desc <- schema) {
      if (desc.`type` == FieldType.ENTITY) {
        q = q.fetch(desc.field)
      }
    }
    return q
  }

  /**
   * Creates a new model with canned data
   * @return
   */
  protected def createModel(id: Long = -1): M = {
    val items: List[M] = getParentQuery.findList
    var item = items.get(0)
    TestUtils.setFieldValue(item, "id", if(id == -1) null else id.asInstanceOf[AnyRef])
    return item
  }
//
  /**
   * Makes sure that all elements of array a equal all elements in array b
   * @param expected
   * @param actual
   */
  protected def assertEquals(expected: Array[AnyRef], actual: Array[AnyRef]) {
    assertEquals(expected, actual, true)
  }
//
  /**
   * Makes sure that all elements of array a equal all elements in array b
   * @param expected
   * @param actual
   */
  protected def assertEquals(expected: Array[AnyRef], actual: Array[AnyRef], compareIds: Boolean) {
    actual.length must be equalTo(expected.length)
    expected.zipWithIndex.foreach((item) => {
      for (f <- modelFields) {
        if (!classOf[Collection[_]].isAssignableFrom(f.getType) && f.getType.getName.startsWith("java") && !f.getName.toLowerCase.contains("timestamp") && !f.getName.toLowerCase.contains("passwordhash") && (compareIds || !(f.getName == "id"))) {
          val actualFieldValue: AnyRef = TestUtils.getFieldValue(actual(item._2), f)
          val expectedFieldValue: AnyRef = TestUtils.getFieldValue(expected(item._2), f)
          actualFieldValue must be equalTo(expectedFieldValue);
        }
      }
    });
  }

  def getControllerInstance() : CrudController[M];

  def getNestedField() : String;

  def getUniqueFieldName(): String;

  // Helpers
  def list(controller: CrudController[_], offset:Integer = 0, count:Integer = null, orderBy:String = null, fields:String = null, fetches:String  = null, q:String = null, userId: java.lang.Long = 1) : play.mvc.Result = {
    return controller.list(offset,count,orderBy,fields,fetches,q).get(1000);
  }

  // Helpers
  def read(controller: CrudController[_], id: Long = -1, fields:String = null, fetches:String  = null) : play.mvc.Result = {
    controller.read(id, fields, fetches).get(1000);
  }

  def delete(controller: CrudController[_], id: java.lang.Long, userId:java.lang.Long = 1) : play.mvc.Result = {
    controller.delete(id).get(1000);
  }

  def deleteBulk(controller: CrudController[_], userId:java.lang.Long = 1) : play.mvc.Result = {
    controller.deleteBulk().get(1000);
  }

  def create(controller: CrudController[_]) : play.mvc.Result = {
    controller.create().get(1000);
  }

  def update(controller: CrudController[_], id: Long) : play.mvc.Result = {
    controller.update(id).get(1000);
  }

  protected trait ResultValidator[R] {
    def isInvalid(u: R): Boolean
  }

  // This basically adds Spec Setup and Teardown
  override def map(fs: =>Fragments) = {
    Step(TestUtils.startApp) ^ fs ^ Step(TestUtils.stopApp)
  }

  private class HttpContextBeforeAfter(script: String) extends ScriptRunnerBeforeAfter(script) {
    var mockContext: Http.Context = null;
    var mockBody: Object = null;

    override def before = {
      super.before

      mockContext = TestUtils.getMockContext()

      Http.Context.current.set(mockContext);
    }

    def setRequestBody(body: Object) {
      mockBody = body;

      var jsonPayload = "";
      if (body != null) {
        jsonPayload = Json.stringify(Util.toJson(body));
      }

      TestUtils.setRequestBody(mockContext, jsonPayload);
    }

    override def after = {
      super.after
    }

  }
}
