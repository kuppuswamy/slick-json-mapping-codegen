import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.3",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "slick-json-mapping-codegen",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-codegen" % "3.2.1",
      "org.slf4j" % "slf4j-nop" % "1.6.4"
    ),
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.6",
    libraryDependencies += "mysql" % "mysql-connector-java" % "6.0.6",
    libraryDependencies += scalaTest % Test
  )
