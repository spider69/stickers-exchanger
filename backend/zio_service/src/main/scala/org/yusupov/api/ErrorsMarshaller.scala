package org.yusupov.api

import org.http4s.Response
import org.yusupov.errors._
import zio.interop.catz._
import zio.logging._

trait ErrorsMarshaller[R <: Api.DefaultApiEnv] {
  this: Api[R] =>

  import dsl._

  def errorToResultCode(e: Throwable): ApiTask[Response[ApiTask]] = {
    val resultCode = e match {
      case SessionCookieIsAbsent => BadRequest(e.getMessage)
      case BadStickersCount => BadRequest(e.getMessage)
      case IncorrectUserPassword => BadRequest(e.getMessage)

      case UserNotExist(_) => NotFound(e.getMessage)
      case UserNotFound => NotFound(e.getMessage)
      case SessionNotFound => NotFound(e.getMessage)
      case StickerNotFound => NotFound(e.getMessage)
      case CollectionNotFound => NotFound(e.getMessage)

      case _ => InternalServerError(e.getMessage)
    }
    log.error(e.getMessage) *> resultCode
  }
}
