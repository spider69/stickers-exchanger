package org.yusupov.database.repositories.auth

import org.yusupov.database.dao.auth.UserDao
import org.yusupov.database.repositories.Repository
import org.yusupov.structures.UserId
import zio.{Has, ULayer, ZLayer}

object UsersRepository extends Repository {

  import dbContext._

  type UsersRepository = Has[Service]

  trait Service {
    def insert(users: List[UserDao]): Result[Unit]

    def get(id: UserId): Result[Option[UserDao]]

    def getAll(except: Option[UserId] = None): Result[List[UserDao]]

    def getByName(name: String): Result[Option[UserDao]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val usersTable = quote {
      querySchema[UserDao](""""Users"""")
    }

    override def insert(users: List[UserDao]) =
      dbContext.run(liftQuery(users).foreach(u => usersTable.insert(u))).unit

    override def get(id: UserId) =
      dbContext.run(usersTable.filter(_.id == lift(id))).map(_.headOption)

    override def getAll(except: Option[UserId]) = except match {
      case Some(userId) =>
        dbContext.run(usersTable.filter(_.id != lift(userId)))
      case None =>
        dbContext.run(usersTable)
    }

    override def getByName(name: String): Result[Option[UserDao]] =
      dbContext.run(usersTable.filter(_.name == lift(name))).map(_.headOption)

    override def delete(id: UserId) =
      dbContext.run(usersTable.filter(_.id == lift(id)).delete).unit
  }

  lazy val live: ULayer[UsersRepository.UsersRepository] =
    ZLayer.succeed(new ServiceImpl())

}
