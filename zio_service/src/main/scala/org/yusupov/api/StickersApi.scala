package org.yusupov.api

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.services.StickersService
import org.yusupov.services.StickersService.StickersService
import org.yusupov.services.UsersService.UsersService
import org.yusupov.structures.{StickerInsertion, UserWithSession}
import zio.interop.catz._
import zio.random.Random

class StickersApi[R <: StickersService with DBTransactor with UsersService with Random] extends Api[R] {

  import dsl._

  val stickersRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root as _ =>
      StickersService.getAll.foldM(
        _ => NotFound(),
        result => Ok(result)
      )
    case authReq @ POST -> Root / collectionId as _ =>
      val handleRequest = for {
        sticker <- authReq.req.as[StickerInsertion]
        result <- StickersService.insert(collectionId, sticker)
      } yield result

      handleRequest.foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )
    case DELETE -> Root / stickerId as _ =>
      StickersService.delete(stickerId).foldM(
        _ => BadRequest(s"Not found: $stickerId"),
        result => Ok(result)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(stickersRoutes)

}
