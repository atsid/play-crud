package utils;

import java.lang.reflect.Field
import java.nio.charset.Charset
import java.sql.SQLException

import com.google.common.collect.Lists
import org.mockito.Mockito._
import org.mockito.Mockito._
import java.sql.Connection
import play.i18n.Lang
import play.libs.Json
import play.mvc.Http
import play.test.{FakeApplication, Helpers}
import play.test.Helpers._
import com.google.common.collect.Lists
import org.junit.AfterClass
import org.junit.BeforeClass
import play.test.FakeApplication
import play.test.Helpers
import java.io._
import java.util.HashMap
import java.util.List
import java.util.Map
import play.test.Helpers.fakeApplication

/**
 * Created by davidtittsworth on 9/11/14.
 */
object TestUtils {
  protected var app: FakeApplication = null
  private final val includedPlugins: List[String] = Lists.newArrayList();
  private final val excludedPlugins: List[String] = Lists.newArrayList("com.github.cleverage.elasticsearch.plugin.IndexPlugin", "play.api.libs.concurrent.AkkaPlugin")

  def startApp {
    val conf: Map[String, AnyRef] = new HashMap[String, AnyRef]
    conf.put("db.default.driver", "org.h2.Driver")
    conf.put("db.default.url", "jdbc:h2:mem:play-test;DB_CLOSE_DELAY=-1;MODE=MYSQL;DB_CLOSE_ON_EXIT=FALSE")
    conf.put("elasticsearch.indexDataInitially", Boolean.box(false))
    conf.put("elasticsearch.indexDataPeriodically", Boolean.box(false))
    app = fakeApplication(conf, includedPlugins, excludedPlugins)
    Helpers.start(app)
  }

  def getMockContext(jsonBody: String = ""): Http.Context = {
    val mockRequest: Http.Request = mock(classOf[Http.Request])

    when(mockRequest.remoteAddress).thenReturn("127.0.0.1")
    when(mockRequest.getHeader("User-Agent")).thenReturn("mocked user-agent")
    val mockContext: Http.Context = mock(classOf[Http.Context])
    when(mockContext.request).thenReturn(mockRequest)
    when(mockContext.lang).thenReturn(Lang.forCode("en"))
    val map: HashMap[String, String] = new HashMap[String, String]
    val expiration: Long = System.currentTimeMillis + (1000 * 60 * 30)
    map.put("pa.u.exp", expiration.toString)
    map.put("pa.p.id", "enfield")
    map.put("pa.u.id", "john.smith@hotmail.com")
    map.put("user", "1")
    val mockSession: Http.Session = new Http.Session(map)
    when(mockContext.session).thenReturn(mockSession)
    return mockContext
  }

  def setRequestBody(mockContext: Http.Context, jsonBody: String): Unit = {
    if (jsonBody != null && !jsonBody.isEmpty) {
      val node = Json.parse(jsonBody);
      val mockBody = mock(classOf[Http.RequestBody]);
      when(mockBody.asJson()).thenReturn(node);
      when(mockContext.request.body()).thenReturn(mockBody);
    }
  }

  def runSqlScript(script: String) {
    if (script != null && !script.isEmpty) {
      val is: ByteArrayInputStream = new ByteArrayInputStream(script.getBytes(Charset.defaultCharset))
      TestUtils.runSqlScript(is)
      is.close
    }
  }

  def runSqlScript(is: InputStream) {
    if (is != null) {
      val con: Connection = play.db.DB.getConnection
      val runner: ScriptRunner = new ScriptRunner(con, true, false)
      runner.setDelimiter(";", false)
      runner.setLogWriter(null)
      val reader: BufferedReader = new BufferedReader(new InputStreamReader(is))
      runner.runScript(reader)
      reader.close
      con.close
    }
  }

  def getFieldValue(obj: Object, fieldName: String): AnyRef = {
    getFieldValue(obj, obj.getClass.getField(fieldName))
  }
  def getFieldValue(obj: Object, field: Field): AnyRef = {
    field.get(obj)
  }
  def setFieldValue(obj: Object, fieldName: String, value: AnyRef) : Unit = {
    setFieldValue(obj, obj.getClass.getField(fieldName), value)
  }
  def setFieldValue(obj: Object, field: Field, value: AnyRef) : Unit  = {
    field.set(obj, value)
  }

  def getApp : FakeApplication = {
    app;
  }

  def stopApp {
    Helpers.stop(app)
  }
}
