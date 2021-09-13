package org.yusupov.api

import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import zio.RIO
import zio.interop.catz._

trait Api[R] {

  type ApiTask[A] = RIO[R, A]

  val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]
  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  def routes: HttpRoutes[ApiTask]
}
