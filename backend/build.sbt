import Dependencies._

ThisBuild / scalacOptions += "-Ymacro-annotations"

ThisBuild / scalaVersion := "2.13.6"

lazy val backend = (project in file("."))
  .aggregate(
    zio_service
  )

lazy val zio_service = (project in file("zio_service"))
  .settings(
    name := "zio_service",
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
