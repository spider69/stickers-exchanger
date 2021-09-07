package org.yusupov

import cats.effect.{ExitCode => CatsExitCode}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.yusupov.api.{CollectionsApi, StickersApi}
import org.yusupov.config.{Config, ConfigService}
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.repositories.{CollectionsRepository, StickersRepository}
import org.yusupov.database.services.{MigrationService, TransactorService}
import org.yusupov.database.services.MigrationService.{Liqui, MigrationService}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.services.{CollectionsService, StickersService}
import org.yusupov.services.StickersService.StickersService
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.random.Random
import zio.{RIO, ZIO}

object Main extends zio.App {

  type AppEnvironment = StickersService with StickersRepository.StickersRepository
    with CollectionsService.CollectionsService with CollectionsRepository.CollectionsRepository
    with Configuration with Clock with Blocking with Random
    with Liqui with MigrationService with DBTransactor

  val appEnvironment = ConfigService.live >+> Blocking.live >+>
    TransactorService.live >+> MigrationService.liquibaseLayer >+> MigrationService.live >+>
    StickersRepository.live >+> StickersService.live >+>
    CollectionsRepository.live >+> CollectionsService.live

  type AppTask[A] = RIO[AppEnvironment, A]

  val httpApp = Router[AppTask](
    "/stickers" -> new StickersApi().route,
    "/collections" -> new CollectionsApi().route
  ).orNotFound

  val server = for {
    _ <- zio.console.putStrLn("Reading config")
    config <- zio.config.getConfig[Config]
    _ <- zio.console.putStrLn("Performing migration")
    _ <- MigrationService.performMigration
    _ <- zio.console.putStrLn("Starting server")
    _ <- ZIO.runtime[AppEnvironment].flatMap { runtime =>
      val ec = runtime.platform.executor.asEC
      val F: cats.effect.Async[AppTask] = implicitly
      BlazeServerBuilder[AppTask](ec)(F)
        .bindHttp(config.api.port, config.api.host)
        .withHttpApp(httpApp)
        .serve
        .compile[AppTask, AppTask, CatsExitCode]
        .drain
    }
  } yield ()

  override def run(args: List[String]) =
    server
      .provideSomeLayer[zio.ZEnv](appEnvironment)
      .tapError(err => zio.console.putStrLn(s"Execution failed with: $err"))
      .exitCode

}
