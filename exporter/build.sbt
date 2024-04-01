
// see https://github.com/spotify/scio/blob/v0.14.2/build.sbt
val scioVersion = "0.14.2"
val beamVersion = "2.49.0"
val slf4jVersion = "1.7.30"
lazy val commonSettings = Def.settings(
  scalaVersion := "2.13.13",
  resolvers += Resolver.mavenLocal,
  scalacOptions ++= Seq(
    "-release", "21",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Ymacro-annotations"
  ),
  javacOptions ++= Seq("--release", "21"),
  // add extra resolved and remove exclude if you need kafka
  // resolvers += "confluent" at "https://packages.confluent.io/maven/",
  excludeDependencies += "org.apache.beam" % "beam-sdks-java-io-kafka",
)

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "exporter",
    organization := "com.dyptan",
    version := "0.1.0-SNAPSHOT",
    publish / skip := true,
    fork := true,
    libraryDependencies ++= Seq(
      "com.spotify" %% "scio-core" % scioVersion,
      "org.apache.beam" % "beam-runners-direct-java" % beamVersion,
      "org.apache.beam" % "beam-sdks-java-io-mongodb" % beamVersion,
      "org.apache.beam" % "beam-sdks-java-io-amazon-web-services2" % beamVersion,
      "com.dyptan" % "exporter-api" % "1.6",
      "org.slf4j" % "slf4j-api" % slf4jVersion,
      "com.spotify" %% "scio-test" % scioVersion % Test,
      "org.slf4j" % "slf4j-simple" % slf4jVersion % Test,
      "ch.qos.logback" % "logback-classic" % "1.4.13"
    ),
  )

lazy val repl: Project = project
  .in(file(".repl"))
  .settings(commonSettings)
  .settings(
    name := "repl",
    description := "Scio REPL for scio job",
    libraryDependencies ++= Seq(
      "com.spotify" %% "scio-repl" % scioVersion
    ),
    Compile / mainClass := Some("com.spotify.scio.repl.ScioShell"),
    publish / skip := true,
    fork := false,
  )
  .dependsOn(root)
