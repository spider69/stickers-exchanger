package org.yusupov.api

import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.yusupov.database.services.TransactorService
import org.yusupov.services.CollectionsService
import org.yusupov.services.CollectionsService.CollectionsService
import org.yusupov.structures.Collection
import zio.interop.catz._
import zio.random.Random

class CollectionsApi[R <: CollectionsService with TransactorService.DBTransactor with Random] extends Api[R] {

  import dsl._

  override def routes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask]{
    case GET -> Root =>
      CollectionsService.getAll.foldM(
        _ => NotFound(),
        result => Ok(result)
      )
    case req @ POST -> Root =>
      (
        for {
          record <- req.as[Collection]
          result <- CollectionsService.insert(record)
        } yield result
        ).foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )
    case DELETE -> Root / collectionId =>
      CollectionsService.delete(collectionId).foldM(
        _ => BadRequest(s"Not found: $collectionId"),
        result => Ok(result)
      )
  }

}
