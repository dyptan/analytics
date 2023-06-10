ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "crawler",
    idePackagePrefix := Some("com.dyptan.crawler")
  )

//libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.8.15"

libraryDependencies ++= List(
  "com.softwaremill.sttp.client3" %% "zio" % "3.8.15",
  "com.softwaremill.sttp.client3" %% "circe" % "3.8.15",
  "io.circe" %% "circe-generic" % "0.14.1"
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-kafka"   % "2.3.1",
)
libraryDependencies += "dev.zio" %% "zio-akka-cluster" % "0.3.0"
val circeVersion = "0.14.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
