package org.yusupov.database.repositories

import doobie.quill.DoobieContext
import io.getquill.{CompositeNamingStrategy2, Escape, Literal}
import org.yusupov.database.services.TransactorService

trait Repository {
  val dbContext: DoobieContext.Postgres[CompositeNamingStrategy2[Escape.type, Literal.type]] =
    TransactorService.doobieContext

  import dbContext._

  class ExtendedResult[T](val result: Result[T]) {
    def unit: Result[Unit] = result.map(_ => ())
  }

  implicit def resultToUnit[T](result: Result[T]) = new ExtendedResult(result)
}
