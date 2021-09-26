package org.yusupov.api

import cats.data.{Kleisli, OptionT}
import io.circe.{Decoder, Encoder}
import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Cookie
import org.http4s.server.AuthMiddleware
import org.yusupov.config.Config
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.errors
import org.yusupov.errors.SessionCookieIsAbsent
import org.yusupov.logging.LoggerService.LoggerService
import org.yusupov.services.auth.UsersService
import org.yusupov.services.auth.UsersService.UsersService
import org.yusupov.structures.auth.UserWithSession
import org.yusupov.structures.{SessionId, auth}
import zio.console.Console
import zio.interop.catz._
import zio.random.Random
import zio.{RIO, ZIO}
import zio.logging._

trait Api[R <: UsersService with DBTransactor with Random with Console with Configuration with LoggerService] {

  type ApiTask[A] = RIO[R, A]

  val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]
  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  object UserIdParamMatcher extends QueryParamDecoderMatcher[String]("userId")

  def okWithCookie[A](result: A, sessionId: SessionId)(implicit encoder: EntityEncoder[ApiTask, A]) =
    for {
      config <- zio.config.getConfig[Config]
      result <- Ok(result).map(_.addCookie(ResponseCookie(
        name = "ssid",
        content = sessionId.toString,
        path = Some("/"),
        domain = Some(config.api.cookieDomain)
      )))
    } yield result

  def extractCookie(request: Request[ApiTask]): ZIO[Any, errors.Error, String] =
    ZIO.fromEither(
      request.headers.get[Cookie]
        .flatMap(_.values.toList.find(_.name == "ssid"))
        .map(_.content)
        .toRight(SessionCookieIsAbsent)
    )

  private def getUser(request: Request[ApiTask]): ApiTask[UserWithSession] = {
    for {
      sessionId <- extractCookie(request)
      session <- UsersService.getSession(sessionId)
      userId = session.userId.toString
      user <- UsersService.getUser(userId)
    } yield auth.UserWithSession(user, session)
  }

  private val authUser: Kleisli[ApiTask, Request[ApiTask], Either[String, UserWithSession]] = Kleisli { request =>
    getUser(request).foldM(
      error => ZIO.left(error.getMessage),
      result => ZIO.right(result)
    )
  }

  private val onFailure: AuthedRoutes[String, ApiTask] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
  val authMiddleware: AuthMiddleware[ApiTask, UserWithSession] = AuthMiddleware(authUser, onFailure)

  def routes: HttpRoutes[ApiTask]
}
