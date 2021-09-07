package org.yusupov.database.services

import doobie.util.transactor.Transactor
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.{ClassLoaderResourceAccessor, CompositeResourceAccessor, FileSystemResourceAccessor}
import org.yusupov.config.Config
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.services.TransactorService.DBTransactor
import zio.macros.accessible
import zio.interop.catz._
import zio.{Has, RIO, Task, ULayer, URIO, ZIO, ZLayer, ZManaged}

@accessible
object MigrationService {

  type Liqui = Has[Liquibase]
  type MigrationService = Has[MigrationService.Service]

  trait Service {
    def performMigration: RIO[Liqui, Unit]
  }

  class MigrationServiceImpl extends Service {
    override def performMigration = liquibase.map(_.update("dev"))
  }

  private def makeLiquibase(
    config: Config,
    transactor: Transactor[Task]
  ): ZManaged[Any, Throwable, Liquibase] =
    for {
      connection <- transactor.connect(transactor.kernel).toManagedZIO
      fileAccessor <- ZIO.effect(new FileSystemResourceAccessor()).toManaged_
      classLoader <- ZIO.effect(classOf[MigrationService].getClassLoader).toManaged_
      classLoaderAccessor <- ZIO.effect(new ClassLoaderResourceAccessor(classLoader)).toManaged_
      fileOpener <- ZIO.effect(new CompositeResourceAccessor(fileAccessor, classLoaderAccessor)).toManaged_
      jdbcConnection <- ZManaged.makeEffect(new JdbcConnection(connection))(c => c.close())
      liqui <- ZIO.effect(new Liquibase(config.liquibase.changeLog, fileOpener, jdbcConnection)).toManaged_
    } yield liqui

  val liquibaseLayer: ZLayer[DBTransactor with Configuration, Throwable, Liqui] = ZLayer.fromManaged(
    for {
      config <- zio.config.getConfig[Config].toManaged_
      transactor <- TransactorService.databaseTransactor.toManaged_
      liquibase <- makeLiquibase(config, transactor)
    } yield liquibase
  )

  def liquibase: URIO[Liqui, Liquibase] = ZIO.service[Liquibase]

  lazy val live: ULayer[MigrationService] = ZLayer.succeed(new MigrationServiceImpl)

}
