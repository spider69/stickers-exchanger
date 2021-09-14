package org.yusupov.api

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.services.CollectionsService
import org.yusupov.services.CollectionsService.CollectionsService
import org.yusupov.services.UsersService.UsersService
import org.yusupov.structures.{CollectionInsertion, UserWithSession}
import zio.interop.catz._
import zio.random.Random

class CollectionsApi[R <: CollectionsService with DBTransactor with UsersService with Random] extends Api[R] {

  import dsl._

  val collectionsRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root as _ =>
      CollectionsService.getAll.foldM(
        _ => NotFound(),
        result => Ok(result)
      )
    case authReq @ POST -> Root as _ =>
      val handleRequest = for {
        collection <- authReq.req.as[CollectionInsertion]
        result <- CollectionsService.insert(collection)
      } yield result

      handleRequest.foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )
    case DELETE -> Root / collectionId as _ =>
      CollectionsService.delete(collectionId).foldM(
        _ => BadRequest(s"Not found: $collectionId"),
        result => Ok(result)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(collectionsRoutes)

}
