ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

resolvers ++= Seq(
  "confluent" at "https://packages.confluent.io/maven/",
)

lazy val root = (project in file("."))
  .settings(
    name := "crawler",
//    idePackagePrefix := Some("com.dyptan.crawler")
  )

libraryDependencies ++= List(
  "com.softwaremill.sttp.client3" %% "zio" % "3.8.15",
  "com.softwaremill.sttp.client3" %% "circe" % "3.8.15",
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-kafka"   % "2.4.2",
  "dev.zio" %% "zio-http" % "3.0.0-RC2",
  "dev.zio" %% "zio-akka-cluster" % "0.3.0",
  "dev.zio" %% "zio-cache" % "0.2.3"
)

val circeVersion = "0.14.3"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % "2.0.15" % Test,
  "dev.zio" %% "zio-test-sbt"      % "2.0.15" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.0.15" % Test
)

libraryDependencies ++= Seq(
  "io.confluent" % "kafka-avro-serializer" % "7.0.0",
  "com.sksamuel.avro4s" %% "avro4s-core" % "4.0.6"
)


testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
libraryDependencies += "org.apache.avro" % "avro" % "1.11.3"
