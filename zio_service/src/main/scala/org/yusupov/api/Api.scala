package org.yusupov.api

import cats.data.{Kleisli, OptionT}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.header._
import org.http4s._
import org.yusupov.database.services.TransactorService
import org.yusupov.errors
import org.yusupov.errors.{AbsentAuthHeader, SessionNotFound, UserNotFound}
import org.yusupov.services.UsersService
import org.yusupov.structures.UserWithSession
import zio.interop.catz._
import zio.random.Random
import zio.{RIO, ZIO}

import java.util.UUID
import scala.util.Try

trait Api[R <: UsersService.UsersService with TransactorService.DBTransactor with Random] {

  type ApiTask[A] = RIO[R, A]

  val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]
  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  private val TokenPrefix = "Token "

  def extractTokenHeader(request: Request[ApiTask]): ZIO[Any, errors.Error, String] =
    ZIO.fromEither(
      request.headers.get[Authorization]
        .map(_.value.replaceFirst(TokenPrefix, ""))
        .toRight(AbsentAuthHeader)
    )

  private def getUser(request: Request[ApiTask]): ApiTask[Either[errors.Error, UserWithSession]] = {
    for {
      id <- extractTokenHeader(request)
      sessionId <- ZIO.fromTry(Try(UUID.fromString(id)))
      session <- UsersService.getSession(sessionId).map(_.toRight(SessionNotFound))
      userId <- ZIO.fromEither(session.map(_.userId))
      user <- UsersService.getUser(userId).map(_.toRight(UserNotFound))
    } yield user.flatMap(u => session.map(s => UserWithSession(s, u)))
  }

  private val authUser: Kleisli[ApiTask, Request[ApiTask], Either[String, UserWithSession]] = Kleisli { request =>
    getUser(request).map { e =>
      e.left.map(_.getMessage)
    }.foldM(
      error => ZIO.left(error.getMessage),
      {
        case Left(error) => ZIO.left(error)
        case Right(user) => ZIO.right(user)
      }
    )
  }

  private val onFailure: AuthedRoutes[String, ApiTask] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
  val authMiddleware: AuthMiddleware[ApiTask, UserWithSession] = AuthMiddleware(authUser, onFailure)

  def routes: HttpRoutes[ApiTask]
}
