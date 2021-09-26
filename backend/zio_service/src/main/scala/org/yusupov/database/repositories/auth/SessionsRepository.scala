package org.yusupov.database.repositories.auth

import org.yusupov.database.dao.auth.SessionDao
import org.yusupov.database.repositories.Repository
import org.yusupov.structures.{SessionId, UserId}
import zio.{Has, ULayer, ZLayer}

object SessionsRepository extends Repository {

  import dbContext._

  type SessionsRepository = Has[Service]

  trait Service {
    def insert(session: SessionDao): Result[Unit]

    def get(id: SessionId): Result[Option[SessionDao]]

    def getByUser(userId: UserId): Result[List[SessionDao]]

    def delete(id: SessionId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val sessionsTable = quote {
      querySchema[SessionDao](""""Sessions"""")
    }

    override def insert(session: SessionDao) =
      dbContext.run(sessionsTable.insert(lift(session))).unit

    override def get(id: SessionId) =
      dbContext.run(sessionsTable.filter(_.id == lift(id))).map(_.headOption)

    override def getByUser(userId: UserId) =
      dbContext.run(sessionsTable.filter(_.userId == lift(userId)))

    override def delete(id: SessionId) =
      dbContext.run(sessionsTable.filter(_.id == lift(id)).delete).unit
  }

  lazy val live: ULayer[SessionsRepository.SessionsRepository] =
    ZLayer.succeed(new ServiceImpl())

}
