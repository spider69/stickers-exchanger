package org.yusupov.api.stickers

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.services.stickers.StickersService
import org.yusupov.services.stickers.StickersService.StickersService
import org.yusupov.structures.auth.UserWithSession
import org.yusupov.structures.stickers.StickerInsertion
import zio.interop.catz._
import zio.logging.log

class StickersApi[R <: Api.DefaultApiEnv with StickersService] extends Api[R] {

  import dsl._

  object CollectionIdParamMatcher extends QueryParamDecoderMatcher[String]("collectionId")

  val stickersRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root / stickerId as UserWithSession(_, session) =>
      log.info("Get sticker by id") *>
        StickersService.get(stickerId).foldM(
          errorToResultCode,
          result => okWithCookie(result, session.id)
        )

    case GET -> Root :? CollectionIdParamMatcher(collectionId) as UserWithSession(_, session)  =>
      log.info("Get stickers by collection") *>
        StickersService.getForCollection(collectionId).foldM(
          errorToResultCode,
          result => okWithCookie(result, session.id)
        )

    case authReq @ POST -> Root / collectionId as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info("Add new sticker")
        sticker <- authReq.req.as[StickerInsertion]
        result <- StickersService.insert(collectionId, sticker)
      } yield result

      handleRequest.foldM(
        errorToResultCode,
        result => okWithCookie(result, session.id)
      )

    case DELETE -> Root / stickerId as UserWithSession(_, session) =>
      log.info("Delete sticker") *>
        StickersService.delete(stickerId).foldM(
          errorToResultCode,
          result => okWithCookie(result, session.id)
        )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(stickersRoutes)

}
