package org.yusupov.api

import cats.data.{Kleisli, OptionT}
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.header._
import org.http4s.{AuthedRoutes, HttpRoutes, Request}
import org.yusupov.database.services.TransactorService
import org.yusupov.services.UsersService
import org.yusupov.structures.{Password, User}
import zio.ZIO
import zio.interop.catz._
import zio.random.Random

import java.util.UUID
import scala.util.Try

class AuthApi[R <: UsersService.UsersService with TransactorService.DBTransactor with Random] extends Api[R] {

  import dsl._

  def getUser(request: Request[ApiTask]): ApiTask[Either[Throwable, User]] = {
    val sessionId =
      request.headers.get[Authorization]
        .map(_.value.replaceFirst("Token ", ""))
        .toRight(new Exception("Auth header is absent"))
    for {
      id <- ZIO.fromEither(sessionId)
      sessionId <- ZIO.fromTry(Try(UUID.fromString(id)))
      session <- UsersService.getSession(sessionId).map(_.toRight(new Exception("Session not found")))
      userId <- ZIO.fromEither(session.map(_.userId))
      user <- UsersService.getUser(userId).map(_.toRight(new Exception("User not found")))
    } yield user
  }

  val authUser: Kleisli[ApiTask, Request[ApiTask], Either[String, User]] = Kleisli { request =>
    getUser(request).map { e =>
      e.left.map(_.toString)
    }.foldM(
      e => ZIO.left(e.toString),
      {
        case Left(value) => ZIO.left(value)
        case Right(value) => ZIO.right(value)
      }
    )
  }
  val onFailure: AuthedRoutes[String, ApiTask] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
  val middleware: AuthMiddleware[ApiTask, User] = AuthMiddleware(authUser, onFailure)

  case class ApiUser(login: String, password: Password)

  val standardRoutes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask] {
    case req @ POST -> Root / "sign_up" =>
      val requestHandler = for {
        user <- req.as[ApiUser]
        id <- zio.random.nextUUID
        _ <- UsersService.addUser(User(id, user.login), user.password)
      } yield ()

      requestHandler.foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )

    case req @ POST -> Root / "sign_in" =>
      val requestHandler = for {
        apiUser <- req.as[ApiUser]
        user <- UsersService.checkUser(apiUser.login, apiUser.password)
        sessionId <- UsersService.addSession(user.id)
      } yield sessionId

      requestHandler.foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )
  }

  val authedRoutes: AuthedRoutes[User, ApiTask] = AuthedRoutes.of[User, ApiTask] {
    case GET -> Root as user =>
      Ok(s"Welcome, ${user.name}")
  }

  override def routes: HttpRoutes[ApiTask] =
     standardRoutes <+> middleware(authedRoutes)

}
