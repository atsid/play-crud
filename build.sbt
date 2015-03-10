name := "play-crud"

organization := "com.atsid"

version := "0.9-SNAPSHOT"

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

play.Project.playJavaSettings

useGpg := true

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
<url>https://github.com/atsid/play-crud</url>
  <licenses>
    <license>
      <name>Apache 2.0</name>
      <url>https://github.com/atsid/play-crud/blob/master/LICENSE.md</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:atsid/play-crud.git</url>
    <connection>scm:git:git@github.com:atsid/play-crud.git</connection>
  </scm>
  <developers>
    <developer>
      <id>atsid</id>
      <name>Applied Technical Systems</name>
      <url>https://github.com/atsid</url>
    </developer>
  </developers>
)