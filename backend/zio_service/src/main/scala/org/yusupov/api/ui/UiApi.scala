package org.yusupov.api.ui

import org.http4s.{HttpRoutes, Request, StaticFile}
import org.yusupov.api.Api
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.services.TransactorService
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.logging.LoggerService.LoggerService
import org.yusupov.services.auth.UsersService
import org.yusupov.services.auth.UsersService.UsersService
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz._
import zio.random.Random

class UiApi[R <: Clock with Blocking with UsersService with DBTransactor with Random with Console with Configuration with LoggerService] extends Api[R] {

  import dsl._

  private val supportedFileFormats = List(".js", ".css", ".map", ".html", ".webm", ".png", ".jpg", ".json", ".ico")
  private val uiRoutes = List("login", "signup", "home", "collections", "stickers", "users")

  def static(file: String, request: Request[ApiTask]) =
    StaticFile.fromResource("web/" + file, Some(request)).getOrElseF(NotFound())

  val standardRoutes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask] {
    case request @ GET -> path if supportedFileFormats.exists(path.segments.mkString("/").endsWith) =>
      static(path.segments.mkString("/"), request)
    case request @ GET -> path if uiRoutes.exists(path.segments.map(_.toString).contains) =>
      static("index.html", request)
    case request @ GET -> Root =>
      static("index.html", request)
  }

  override def routes: HttpRoutes[ApiTask] =
    standardRoutes
}
