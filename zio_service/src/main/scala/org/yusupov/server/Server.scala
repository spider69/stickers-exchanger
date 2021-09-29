package org.yusupov.server

import cats.effect.{ExitCode => CatsExitCode}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.yusupov.api.auth.AuthApi
import org.yusupov.api.collections.{CollectionsApi, UserCollectionsApi}
import org.yusupov.api.search.SearchApi
import org.yusupov.api.stickers.{StickersApi, UserStickersApi}
import org.yusupov.api.ui.UiApi
import org.yusupov.config.Config
import org.yusupov.database.services.{DataGenerationService, MigrationService}
import zio.{RIO, ZIO}
import zio.interop.catz._

object Server extends Environment {

  type AppTask[A] = RIO[AppEnvironment, A]

  private val httpApp = Router[AppTask](
    "" -> new UiApi().routes,
    "/api/auth" -> new AuthApi().routes,
    "/api/stickers" -> new StickersApi().routes,
    "/api/collections" -> new CollectionsApi().routes,
    "/api/user/stickers" -> new UserStickersApi().routes,
    "/api/user/collections" -> new UserCollectionsApi().routes,
    "/api/exchanges" -> new SearchApi().routes
  ).orNotFound

  private val server = for {
    _ <- zio.console.putStrLn("Reading config")
    config <- zio.config.getConfig[Config]
    _ <- zio.console.putStrLn("Performing migration")
    _ <- MigrationService.performMigration(clean = config.migrate)
    _ <- zio.console.putStrLn("Generating data")
    _ <- ZIO.when(config.migrate)(DataGenerationService.initCollections)
    _ <- ZIO.when(config.migrate)(DataGenerationService.initUsers)
    _ <- ZIO.when(config.migrate)(DataGenerationService.initUsersCollections)
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

  def start() =
    server
      .provideSomeLayer[zio.ZEnv](appEnvironment)
      .tapError(err => zio.console.putStrLn(s"Execution failed with: $err"))
      .exitCode
}
