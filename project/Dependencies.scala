import sbt._
import Dependencies.Versions._

object Dependencies {

  object Versions {
    lazy val LogbackVersion = "1.2.5"
    lazy val sl4jVersion = "1.7.30"
    lazy val PureconfigVersion = "0.16.0"

    // zio
    lazy val ZioVersion = "1.0.11"
    lazy val ZioInteropCatsVersion = "3.1.1.0"
    lazy val ZioLoggingVersion = "0.5.11"
    lazy val ZioConfigVersion = "1.0.6"

    // http4s
    lazy val Http4sVersion = "0.23.3"
    lazy val CirceVersion = "0.14.1"
    lazy val ReactormonkVersion = "1.3"

    // database
    lazy val DoobieVersion = "1.0.0-RC1"
    lazy val LiquibaseVersion = "4.4.3"
    lazy val PostgresVersion = "42.2.23"

    // security
    lazy val CommonsCodecVersion = "1.15"
  }

  lazy val logback = Seq(
    "ch.qos.logback"  %  "logback-classic" % LogbackVersion
  )

  lazy val zio = Seq(
    "dev.zio" %% "zio" % ZioVersion,
    "dev.zio" %% "zio-interop-cats" % ZioInteropCatsVersion,
    "dev.zio" %% "zio-logging-slf4j" % ZioLoggingVersion,
    "dev.zio" %% "zio-test" % ZioVersion,
    "dev.zio" %% "zio-test-sbt" % ZioVersion,
    "dev.zio" %% "zio-macros" % ZioVersion
  )

  lazy val zioConfig = Seq(
    "dev.zio" %% "zio-config" % ZioConfigVersion,
    "dev.zio" %% "zio-config-magnolia" % ZioConfigVersion,
    "dev.zio" %% "zio-config-typesafe" % ZioConfigVersion
  )

  lazy val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % PureconfigVersion,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % PureconfigVersion
  )

  lazy val http4sServer = Seq(
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
    "org.reactormonk" %% "cryptobits" % ReactormonkVersion
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-generic-extras"% CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion
  )

  lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core"     % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari"    % DoobieVersion,
    "org.tpolecat" %% "doobie-quill"    % DoobieVersion
  )

  lazy val liquiBase = Seq(
    "org.liquibase" % "liquibase-core" % LiquibaseVersion
  )

  lazy val postgres = Seq(
    "org.postgresql" % "postgresql" % PostgresVersion
  )

  lazy val security = Seq(
    "commons-codec" % "commons-codec" % CommonsCodecVersion
  )

}
