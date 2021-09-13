package org.yusupov.services

import org.yusupov.database.dao.SessionDao
import org.yusupov.database.repositories.{SessionsRepository, UsersRepository}
import org.yusupov.database.services.TransactorService
import org.yusupov.structures.{Password, Session, SessionId, User, UserId}
import org.yusupov.utils.SecurityUtils
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZIO, ZLayer}

@accessible
object UsersService {

  type UsersService = Has[Service]

  trait Service {
    def getUser(id: UserId): RIO[TransactorService.DBTransactor, Option[User]]
    def checkUser(name: String, password: Password): RIO[TransactorService.DBTransactor, User]
    def addUser(user: User, password: Password): RIO[TransactorService.DBTransactor, Unit]
    def deleteUser(id: UserId): RIO[TransactorService.DBTransactor, Unit]

    def getSession(sessionId: SessionId): RIO[TransactorService.DBTransactor, Option[Session]]
    def getSessions(userId: UserId): RIO[TransactorService.DBTransactor, List[Session]]
    def addSession(userId: UserId): RIO[TransactorService.DBTransactor with Random, SessionId]
    def deleteSession(userId: UserId): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(
    usersRepository: UsersRepository.Service,
    sessionsRepository: SessionsRepository.Service
  ) extends Service {
    import doobie.implicits._

    override def getUser(id: UserId): RIO[TransactorService.DBTransactor, Option[User]] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao <- usersRepository.get(id).transact(transactor)
        user = userDao.map(_.toUser)
      } yield user

    override def checkUser(name: String, password: Password): RIO[TransactorService.DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao <- usersRepository.getByName(name).transact(transactor)
        user <- ZIO.fromEither(userDao.toRight(new Exception(s"User with name=$name does not exist")))
        isPasswordCorrect <- ZIO.effect(SecurityUtils.checkSecret(password, user.salt, user.passwordHash))
        user <- if(isPasswordCorrect) ZIO.succeed(user.toUser) else ZIO.fail(new Exception("Password is incorrect"))
      } yield user

    override def addUser(user: User, password: Password): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao = user.toDAO(password)
        _ <- usersRepository.insert(userDao).transact(transactor)
      } yield ()

    override def deleteUser(id: UserId): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.delete(id).transact(transactor)
      } yield ()

    override def getSession(sessionId: SessionId) =
      for {
        transactor <- TransactorService.databaseTransactor
        sessionDto <- sessionsRepository.get(sessionId).transact(transactor)
        session = sessionDto.map(_.toSession)
      } yield session

    override def getSessions(userId: UserId) =
      for {
        transactor <- TransactorService.databaseTransactor
        sessionsDao <- sessionsRepository.getByUser(userId).transact(transactor)
        sessions = sessionsDao.map(_.toSession)
      } yield sessions

    override def addSession(userId: UserId) =
      for {
        transactor <- TransactorService.databaseTransactor
        uuid <- zio.random.nextUUID
        _ <- sessionsRepository.insert(SessionDao(uuid, userId)).transact(transactor)
      } yield uuid

    override def deleteSession(userId: UserId) =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- sessionsRepository.delete(userId).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[UsersRepository.UsersRepository with SessionsRepository.SessionsRepository, Nothing, UsersService.UsersService] =
    ZLayer.fromServices[UsersRepository.Service, SessionsRepository.Service, UsersService.Service] {
      (usersRepo, sessionsRepo) => new ServiceImpl(usersRepo, sessionsRepo)
    }
}
