name := "play-crud"

organization := "com.atsid"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
    javaJdbc,
    javaEbean,
    cache,
    "com.google.inject" % "guice" % "3.0",
    "com.google.inject.extensions" % "guice-multibindings" % "3.0",
    "org.reflections" % "reflections" % "0.9.8",
    "net.sf.supercsv" % "super-csv" % "2.1.0",
    "net.sf.supercsv" % "super-csv-dozer" % "2.1.0",
    "org.mockito" %  "mockito-all" % "1.9.5"
)

publishTo := Some("CloudBees Snapshots" at "https://repository-atsid.forge.cloudbees.com/snapshot")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

play.Project.playJavaSettings
