package org.yusupov.api.collections

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.logging.LoggerService.LoggerService
import org.yusupov.services.collections.UserCollectionsService.UserCollectionsService
import org.yusupov.services.auth.UsersService.UsersService
import org.yusupov.services.collections.UserCollectionsService
import org.yusupov.services.stickers.UserStickersService.UserStickersService
import org.yusupov.structures.auth.UserWithSession
import zio.console.Console
import zio.interop.catz._
import zio.logging.log
import zio.random.Random

object UserCollectionsApi {
  type ApiEnv =
    UserStickersService with UserCollectionsService with
      DBTransactor with UsersService with
      Random with Console with Configuration with LoggerService
}

import UserCollectionsApi._

class UserCollectionsApi[R <: ApiEnv] extends Api[R] {

  import dsl._

  val usersCollectionsRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root / "relations" :? UserIdParamMatcher(userId) as UserWithSession(_, session) =>
      log.info("Get user collections relations") *>
        UserCollectionsService.getUserCollectionsRelations(userId).foldM(
          e => log.error(e.getMessage) *> BadRequest(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case GET -> Root :? UserIdParamMatcher(userId) as UserWithSession(_, session) =>
      log.info("Get user collections") *>
        UserCollectionsService.getUserCollections(userId).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case PUT -> Root / collectionId as UserWithSession(user, session) =>
      log.info("Add user collection") *>
        UserCollectionsService.addUserCollection(user.id, collectionId).foldM(
          e => log.error(e.getMessage) *> BadRequest(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case DELETE -> Root / collectionId as UserWithSession(user, session) =>
      log.info("Delete user collection") *>
        UserCollectionsService.deleteUserCollection(user.id, collectionId).foldM(
          e => log.error(e.getMessage) *> BadRequest(e.getMessage),
          result => okWithCookie(result, session.id)
        )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(usersCollectionsRoutes)
}
