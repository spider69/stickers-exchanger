package org.yusupov.api.auth

import cats.implicits._
import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.services.auth.UsersService
import org.yusupov.structures.Password
import org.yusupov.structures.auth.{User, UserWithSession}
import zio.interop.catz._
import zio.logging._

class AuthApi[R <: Api.DefaultApiEnv] extends Api[R] {

  import dsl._

  case class SignInUser(login: String, password: Password)
  case class SignUpUser(login: String, email: String, password: Password)

  val standardRoutes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask] {
    case req @ POST -> Root / "sign_up" =>
      val requestHandler = for {
        _ <- log.info("Sign up user")
        user <- req.as[SignUpUser]
        id <- zio.random.nextUUID
        _ <- UsersService.addUser(User(id, user.login, user.email), user.password)
        sessionId <- UsersService.addSession(id)
        _ <- log.info(s"User ${user.login} signed up successfully")
      } yield (id, sessionId)

      requestHandler.foldM(
        errorToResultCode,
        { case (userId, sessionId) => okWithCookie(userId, sessionId) }
      )

    case req @ POST -> Root / "sign_in" =>
      val requestHandler = for {
        apiUser <- req.as[SignInUser]
        _ <- log.info(s"User ${apiUser.login} try to login")
        user <- UsersService.checkUser(apiUser.login, apiUser.password)
        sessionId <- UsersService.addSession(user.id)
        _ <- log.info(s"User ${apiUser.login} logged in successfully")
      } yield (user.id, sessionId)

      requestHandler.foldM(
        errorToResultCode,
        { case (userId, sessionId) => okWithCookie(userId, sessionId) }
      )

    case req @ GET -> Root / "check_session" =>
      val requestHandler = for {
        _ <- log.info("Check session")
        sessionId <- extractCookie(req)
        session <- UsersService.getSession(sessionId)
      } yield session

      requestHandler.foldM(
        errorToResultCode,
        session => okWithCookie(session.userId, session.id)
      )
  }

  val authedRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root / "users" as UserWithSession(user, session) =>
      log.info("Get users") *>
      UsersService.getUsers(Some(user.id)).foldM(
        errorToResultCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root / "user" / userId as UserWithSession(_, session) =>
      log.info("Get user") *> UsersService.getUser(userId).foldM(
        errorToResultCode,
        result => okWithCookie(result, session.id)
      )

    case DELETE -> Root / "sign_out" as UserWithSession(user, session) =>
      log.info("Sign out") *>
      UsersService.deleteSession(session.id).foldM(
        errorToResultCode,
        result => Ok(result)
      ) <* log.info(s"User ${user.name} signed out successfully")
  }

  override def routes: HttpRoutes[ApiTask] =
     standardRoutes <+> authMiddleware(authedRoutes)

}
