package org.yusupov.api.stickers

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.logging.LoggerService.LoggerService
import org.yusupov.services.stickers.UserStickersService.UserStickersService
import org.yusupov.services.auth.UsersService.UsersService
import org.yusupov.services.stickers.UserStickersService
import org.yusupov.structures.auth.UserWithSession
import zio.console.Console
import zio.interop.catz._
import zio.random.Random
import zio.logging._

class UserStickersApi[R <: UserStickersService with DBTransactor with UsersService with Random with Console with Configuration with LoggerService] extends Api[R] {

  import dsl._

  object CollectionIdParamMatcher extends QueryParamDecoderMatcher[String]("collectionId")
  object CountParamMatcher extends QueryParamDecoderMatcher[Int]("count")

  val usersStickersRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case PUT -> Root  / "update" / stickerId :? CountParamMatcher(count) as UserWithSession(user, session) =>
      log.info("Update user sticker count") *>
        UserStickersService.updateStickersCount(user.id, stickerId, count).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case GET -> Root :? CollectionIdParamMatcher(collectionId) as UserWithSession(user, session) =>
      log.info("Get user stickers by collection") *>
        UserStickersService.getUserStickers(user.id, collectionId).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case GET -> Root / "users_by" / stickerId as UserWithSession(_, session) =>
      log.info("Get users having sticker") *>
        UserStickersService.getUsersBySticker(stickerId).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case GET -> Root / "relations" :? CollectionIdParamMatcher(collectionId) +& UserIdParamMatcher(userId) as UserWithSession(_, session) =>
      log.info("Get user stickers relations") *>
        UserStickersService.getUserStickersRelations(userId, collectionId).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case PUT -> Root / stickerId :? CountParamMatcher(count) as UserWithSession(user, session) =>
      log.info("Add user sticker") *>
        UserStickersService.addUserSticker(user.id, stickerId, count).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case DELETE -> Root / stickerId as UserWithSession(user, session) =>
      log.info("Delete user sticker") *>
        UserStickersService.deleteUserSticker(user.id, stickerId).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(usersStickersRoutes)

}
