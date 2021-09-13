package org.yusupov.api

import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.yusupov.database.services.TransactorService
import org.yusupov.services.StickersService
import org.yusupov.structures.Sticker
import zio.interop.catz._
import zio.random.Random

class StickersApi[R <: StickersService.StickersService with TransactorService.DBTransactor with Random] extends Api[R] {

  import dsl._

  override def routes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask]{
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
