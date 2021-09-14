package org.yusupov

import cats.effect.{ExitCode => CatsExitCode}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.yusupov.api.{AuthApi, CollectionsApi, StickersApi, UsersCollectionsApi}
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.config.{Config, ConfigService}
import org.yusupov.database.repositories.CollectionsRepository.CollectionsRepository
import org.yusupov.database.repositories.SessionsRepository.SessionsRepository
import org.yusupov.database.repositories.StickersRepository.StickersRepository
import org.yusupov.database.repositories.UsersCollectionsRepository.UsersCollectionsRepository
import org.yusupov.database.repositories.UsersRepository.UsersRepository
import org.yusupov.database.repositories.{CollectionsRepository, SessionsRepository, StickersRepository, UsersCollectionsRepository, UsersRepository}
import org.yusupov.database.services.MigrationService.{Liqui, MigrationService}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.database.services.{MigrationService, TransactorService}
import org.yusupov.services.CollectionsService.CollectionsService
import org.yusupov.services.StickersService.StickersService
import org.yusupov.services.UsersCollectionsService.UsersCollectionsService
import org.yusupov.services.UsersService.UsersService
import org.yusupov.services.{CollectionsService, StickersService, UsersCollectionsService, UsersService}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.random.Random
import zio.{RIO, ZIO}

object Main extends zio.App {

  type AppEnvironment =
    StickersService with StickersRepository with
      CollectionsService with CollectionsRepository with
      UsersService with UsersRepository with SessionsRepository with
      UsersCollectionsService with UsersCollectionsRepository with
      Configuration with Clock with Blocking with Random with
      Liqui with MigrationService with DBTransactor

  val appEnvironment =
    ConfigService.live >+> Blocking.live >+>
      TransactorService.live >+> MigrationService.liquibaseLayer >+> MigrationService.live >+>
      StickersRepository.live >+> StickersService.live >+>
      UsersCollectionsRepository.live >+> UsersCollectionsService.live >+>
      CollectionsRepository.live >+> CollectionsService.live >+>
      UsersRepository.live >+> SessionsRepository.live >+> UsersService.live

  type AppTask[A] = RIO[AppEnvironment, A]

  val httpApp = Router[AppTask](
    "/stickers" -> new StickersApi().routes,
    "/collections" -> new CollectionsApi().routes,
    "/auth" -> new AuthApi().routes,
    "/user" -> new UsersCollectionsApi().routes
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
