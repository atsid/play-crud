package utils;

object ScriptDefaults {
  val CLEAN_SCRIPT =
    """
      DELETE FROM TEST_JUNCTION_MODEL WHERE TEST_JUNCTION_MODEL.id>=1;
      DELETE FROM TEST_OTHER_MODEL WHERE TEST_OTHER_MODEL.id>=1;
      DELETE FROM TEST_NESTED_MODEL WHERE TEST_NESTED_MODEL.id>=1;
      DELETE FROM TEST_MODEL WHERE TEST_MODEL.id>=1;
      DELETE FROM TEST_YET_ANOTHER_MODEL WHERE TEST_YET_ANOTHER_MODEL.id>=1;
    """;
}
class ScriptRunnerBeforeAfter(startupScript: String = "", doClean: Boolean = true, cleanScript: String = ScriptDefaults.CLEAN_SCRIPT) extends org.specs2.mutable.BeforeAfter {
  def before = {
    runScripts(startupScript, doClean)
  }

  def after = {
    runScripts("", doClean)
  }

  def runScripts(script: String, clean: Boolean) {
    var sql : StringBuilder = new StringBuilder;
    if (clean) {
      sql ++= cleanScript;
    }
    if (!script.isEmpty) {
      sql ++= script;
    }

    TestUtils.runSqlScript(sql.toString);
  }
}
