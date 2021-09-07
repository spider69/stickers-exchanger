package org.yusupov.api

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.yusupov.database.services.TransactorService
import org.yusupov.services.CollectionsService
import org.yusupov.services.CollectionsService.CollectionsService
import org.yusupov.structures.Collection
import zio.RIO
import zio.interop.catz._
import zio.random.Random

class CollectionsApi[R <: CollectionsService with TransactorService.DBTransactor with Random] {

  type CollectionsTask[A] = RIO[R, A]

  val dsl = Http4sDsl[CollectionsTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[CollectionsTask, A] = jsonOf[CollectionsTask, A]
  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[CollectionsTask, A] = jsonEncoderOf[CollectionsTask, A]

  def route = HttpRoutes.of[CollectionsTask]{
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
