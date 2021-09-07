package org.yusupov.api

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.yusupov.database.services.TransactorService
import org.yusupov.services.StickersService
import org.yusupov.structures.Sticker
import zio.RIO
import zio.interop.catz._
import zio.random.Random

class StickersApi[R <: StickersService.StickersService with TransactorService.DBTransactor with Random] {

  type StickersTask[A] = RIO[R, A]

  val dsl = Http4sDsl[StickersTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[StickersTask, A] = jsonOf[StickersTask, A]
  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[StickersTask, A] = jsonEncoderOf[StickersTask, A]

  def route = HttpRoutes.of[StickersTask]{
    case GET -> Root =>
      StickersService.getAll.foldM(
        _ => NotFound(),
        result => Ok(result)
      )
    case req @ POST -> Root =>
      (
        for {
          record <- req.as[Sticker]
          result <- StickersService.insert(record)
        } yield result
      ).foldM(
        err => BadRequest(err.getMessage),
        result => Ok(result)
      )
    case DELETE -> Root / stickerId =>
      StickersService.delete(stickerId).foldM(
        _ => BadRequest(s"Not found: $stickerId"),
        result => Ok(result)
      )
  }

}
