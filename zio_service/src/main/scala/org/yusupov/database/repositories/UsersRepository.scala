package org.yusupov.database.repositories

import org.yusupov.database.dao.UserDao
import org.yusupov.structures.UserId
import zio.{Has, ULayer, ZLayer}

object UsersRepository extends Repository {

  import dbContext._

  type UsersRepository = Has[Service]

  trait Service {
    def get(id: UserId): Result[Option[UserDao]]
    def getByName(name: String): Result[Option[UserDao]]
    def insert(user: UserDao): Result[Unit]
    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val usersTable = quote {
      querySchema[UserDao](""""Users"""")
    }

    override def get(id: UserId) =
      dbContext.run(usersTable.filter(_.id == lift(id))).map(_.headOption)

    override def getByName(name: String): Result[Option[UserDao]] =
      dbContext.run(usersTable.filter(_.name == lift(name))).map(_.headOption)

    override def insert(user: UserDao) =
      dbContext.run(usersTable.insert(lift(user))).unit

    override def delete(id: UserId) =
      dbContext.run(usersTable.filter(_.id == lift(id)).delete).unit
  }

  lazy val live: ULayer[UsersRepository.UsersRepository] =
    ZLayer.succeed(new ServiceImpl())

}
