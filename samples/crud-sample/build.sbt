name := "crud-sample"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "play-crud" % "play-crud_2.10" % "1.0-SNAPSHOT"
)

resolvers += "Local Play Repository" at "file://Development/SDKs/play/repository/local"

play.Project.playJavaSettings
