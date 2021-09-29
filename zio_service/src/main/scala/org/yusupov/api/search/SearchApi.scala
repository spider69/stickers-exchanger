package org.yusupov.api.search

import io.circe.generic.auto._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.yusupov.api.Api
import org.yusupov.services.search.SearchService
import org.yusupov.services.search.SearchService.SearchService
import org.yusupov.structures.auth.UserWithSession
import zio.interop.catz._
import zio.logging.log

class SearchApi[R <: Api.DefaultApiEnv with SearchService] extends Api[R] {

  import dsl._

  val searchRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root as UserWithSession(user, session) =>
      log.info("Search possible exchanges") *> SearchService.search(user.id.toString).foldM(
        errorToResultCode,
        result => okWithCookie(result, session.id)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(searchRoutes)

}
