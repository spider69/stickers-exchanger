import Dependencies._

ThisBuild / scalacOptions += "-Ymacro-annotations"

ThisBuild / scalaVersion := "2.13.6"

ThisBuild / version := "1.0.0"

lazy val backend = (project in file("."))
  .aggregate(
    stickers_exchanger
  )

lazy val stickers_exchanger = (project in file("zio_service"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "stickers_exchanger",
    libraryDependencies ++= Seq(
      logback,
      zio,
      zioConfig,
      pureConfig,
      http4sServer,
      circe,
      doobie,
      liquiBase,
      postgres,
      security
    ).flatten,
    dependencyOverrides += "org.slf4j" % "slf4j-api" % Versions.sl4jVersion,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )