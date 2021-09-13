package org.yusupov.database.repositories

import org.yusupov.database.dao.SessionDao
import org.yusupov.structures.{SessionId, UserId}
import zio.{Has, ULayer, ZLayer}

object SessionsRepository extends Repository {

  import dbContext._

  type SessionsRepository = Has[Service]

  trait Service {
    def get(id: SessionId): Result[Option[SessionDao]]
    def getByUser(userId: UserId): Result[List[SessionDao]]
    def insert(session: SessionDao): Result[Unit]
    def delete(userId: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val sessionsTable = quote {
      querySchema[SessionDao](""""Sessions"""")
    }

    override def get(id: SessionId) =
      dbContext.run(sessionsTable.filter(_.id == lift(id))).map(_.headOption)

    override def getByUser(userId: UserId) =
      dbContext.run(sessionsTable.filter(_.userId == lift(userId)))

    override def insert(session: SessionDao) =
      dbContext.run(sessionsTable.insert(lift(session))).unit

    override def delete(userId: UserId) =
      dbContext.run(sessionsTable.filter(_.userId == lift(userId)).delete).unit
  }

  lazy val live: ULayer[SessionsRepository.SessionsRepository] =
    ZLayer.succeed(new ServiceImpl())

}