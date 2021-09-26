package org.yusupov.api.collections

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.services.collections.UserCollectionsService
import org.yusupov.services.collections.UserCollectionsService.UserCollectionsService
import org.yusupov.structures.auth.UserWithSession
import zio.interop.catz._
import zio.logging.log

class UserCollectionsApi[R <: Api.DefaultApiEnv with UserCollectionsService] extends Api[R] {

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
