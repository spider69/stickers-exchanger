package org.yusupov.api.collections

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.services.collections.CollectionsService
import org.yusupov.services.collections.CollectionsService.CollectionsService
import org.yusupov.structures.auth.UserWithSession
import org.yusupov.structures.collections.CollectionInsertion
import zio.interop.catz._
import zio.logging._

class CollectionsApi[R <: Api.DefaultApiEnv with CollectionsService] extends Api[R] {

  import dsl._

  val collectionsRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root / collectionId as UserWithSession(_, session) =>
      log.info("Get collection by id") *>
        CollectionsService.get(collectionId).foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case GET -> Root as UserWithSession(_, session) =>
      log.info("Get all collections") *>
        CollectionsService.getAll.foldM(
          e => log.error(e.getMessage) *> NotFound(e.getMessage),
          result => okWithCookie(result, session.id)
        )

    case authReq @ POST -> Root as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info("Add new collection")
        collection <- authReq.req.as[CollectionInsertion]
        result <- CollectionsService.insert(List(collection))
      } yield result

      handleRequest.foldM(
        e => log.error(e.getMessage) *> BadRequest(e.getMessage),
        result => okWithCookie(result, session.id)
      )

    case DELETE -> Root / collectionId as UserWithSession(_, session) =>
      log.info("Delete collection") *>
        CollectionsService.delete(collectionId).foldM(
          e => log.error(e.getMessage) *> BadRequest(e.getMessage),
          result => okWithCookie(result, session.id)
        )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(collectionsRoutes)

}
