package org.yusupov.api

import cats.implicits._
import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.services.UsersService
import org.yusupov.services.UsersService.UsersService
import org.yusupov.structures.{Password, User, UserWithSession}
import zio.interop.catz._
import zio.random.Random

class AuthApi[R <: UsersService with DBTransactor with Random] extends Api[R] {

  import dsl._

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

  val authedRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case DELETE -> Root / "sign_out" as UserWithSession(session, _) =>
      UsersService.deleteSession(session.id).foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
     standardRoutes <+> authMiddleware(authedRoutes)

}
