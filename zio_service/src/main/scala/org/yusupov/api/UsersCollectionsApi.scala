package org.yusupov.api

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.services.UsersCollectionsService
import org.yusupov.services.UsersCollectionsService.UsersCollectionsService
import org.yusupov.services.UsersService.UsersService
import org.yusupov.structures.UserWithSession
import zio.interop.catz._
import zio.random.Random

class UsersCollectionsApi[R <: UsersCollectionsService with DBTransactor with UsersService with Random] extends Api[R] {

  import dsl._

  val usersCollectionsRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root / "stickers" as UserWithSession(_, user) =>
      UsersCollectionsService.getUserStickers(user.id).foldM(
        _ => NotFound(),
        result => Ok(result)
      )
    case PUT -> Root / "stickers" / stickerId as UserWithSession(_, user) =>
      UsersCollectionsService.addUserSticker(user.id, stickerId).foldM(
        e => NotFound(e.getMessage),
        result => Ok(result)
      )
    case DELETE -> Root / "stickers" / stickerId as UserWithSession(_, user) =>
      UsersCollectionsService.deleteUserSticker(user.id, stickerId).foldM(
        e => NotFound(e.getMessage),
        result => Ok(result)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(usersCollectionsRoutes)
}
