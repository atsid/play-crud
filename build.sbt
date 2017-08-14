name := "play-crud"

organization := "com.atsid"

version := "0.9.3"

libraryDependencies ++= Seq(
    javaJdbc,
    javaEbean,
    "org.reflections" % "reflections" % "0.9.8",
    "org.mockito" %  "mockito-all" % "1.9.5"
)

play.Project.playJavaSettings

// This will screw up `sbt dependencies`
crossPaths := false

useGpg := true

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

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
      <id>david.tittsworth</id>
      <name>David Tittsworth</name>
      <email>david.tittsworth@atsid.com</email>
      <url>https://github.com/stopyoukid</url>
      <organization>ATS</organization>
      <organizationUrl>https://github.com/atsid</organizationUrl>
    </developer>
  </developers>
)
