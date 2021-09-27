package org.yusupov.database.services

import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.quill.DoobieContext
import io.getquill.{Escape, Literal, NamingStrategy}
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.config.{Config, DatabaseConfig}
import zio.blocking.Blocking
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{Has, Managed, Task, URIO, ZIO, ZLayer}

import scala.concurrent.ExecutionContext

object TransactorService {

  type DBTransactor = Has[Transactor[Task]]

  lazy val doobieContext = new DoobieContext.Postgres(NamingStrategy(Escape, Literal))

  private def makeTransactor(
    conf: DatabaseConfig,
    connectEc: ExecutionContext
  ): Managed[Throwable, Transactor[Task]] = conf.herokuUrl match {
    case Some(url) =>
      val databaseUrlWithUser = url.replaceFirst("postgres://", "")
      val splitUrl = databaseUrlWithUser.split("@")
      val splitUser = splitUrl(0).split(":")

      val databaseUrl = "jdbc:postgresql://" + splitUrl(1)
      val user = splitUser(0)
      val password = splitUser(1)

      HikariTransactor.newHikariTransactor[Task](
        conf.driver,
        databaseUrl,
        user,
        password,
        connectEc
      ).toManagedZIO
    case _ =>
      HikariTransactor.newHikariTransactor[Task](
        conf.driver,
        conf.url,
        conf.user,
        conf.password,
        connectEc
      ).toManagedZIO

  }

  def databaseTransactor: URIO[DBTransactor, Transactor[Task]] = ZIO.service[Transactor[Task]]

  lazy val live: ZLayer[Configuration with Blocking, Throwable, DBTransactor] = ZLayer.fromManaged(
    for {
      config <- zio.config.getConfig[Config].toManaged_
      ec <- ZIO.descriptor.map(_.executor.asEC).toManaged_
      transactor <- TransactorService.makeTransactor(config.database, ec)
    } yield transactor
  )

}
