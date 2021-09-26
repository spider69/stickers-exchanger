package org.yusupov.services.auth

import org.yusupov.database.dao.auth
import org.yusupov.database.repositories.auth.{SessionsRepository, UsersRepository}
import org.yusupov.database.services.TransactorService
import org.yusupov.errors.{IncorrectUserPassword, SessionNotFound, UserNotExist, UserNotFound}
import org.yusupov.structures.auth.{Session, User}
import org.yusupov.structures.{Password, SessionId, UserId}
import org.yusupov.utils.SecurityUtils
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, RLayer, ZIO, ZLayer}

import java.util.UUID
import scala.util.Try

@accessible
object UsersService {

  type UsersService = Has[Service]

  trait Service {
    def getUsers(except: Option[UserId] = None): RIO[TransactorService.DBTransactor, List[User]]

    def getUser(id: String): RIO[TransactorService.DBTransactor, User]

    def checkUser(name: String, password: Password): RIO[TransactorService.DBTransactor, User]

    def addUser(user: User, password: Password): RIO[TransactorService.DBTransactor, Unit]

    def deleteUser(id: UserId): RIO[TransactorService.DBTransactor, Unit]

    def getSession(sessionId: String): RIO[TransactorService.DBTransactor, Session]

    def getSessions(userId: UserId): RIO[TransactorService.DBTransactor, List[Session]]

    def addSession(userId: UserId): RIO[TransactorService.DBTransactor with Random, SessionId]

    def deleteSession(id: SessionId): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(
    usersRepository: UsersRepository.Service,
    sessionsRepository: SessionsRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def getUsers(except: Option[UserId]) =
      for {
        transactor <- TransactorService.databaseTransactor
        usersDao <- usersRepository.getAll(except).transact(transactor)
        users = usersDao.map(_.toUser)
      } yield users

    override def getUser(id: String): RIO[TransactorService.DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userId <- ZIO.fromTry(Try(UUID.fromString(id)))
        userDao <- usersRepository.get(userId).transact(transactor)
        user <- ZIO.fromEither(userDao.map(_.toUser).toRight(UserNotFound))
      } yield user

    override def checkUser(name: String, password: Password): RIO[TransactorService.DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao <- usersRepository.getByName(name).transact(transactor)
        user <- ZIO.fromEither(userDao.toRight(UserNotExist(name)))
        isPasswordCorrect <- ZIO.effect(SecurityUtils.checkSecret(password, user.salt, user.passwordHash))
        user <- ZIO.cond(isPasswordCorrect, user.toUser, IncorrectUserPassword)
      } yield user

    override def addUser(user: User, password: Password): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao = user.toDAO(password)
        _ <- usersRepository.insert(List(userDao)).transact(transactor)
      } yield ()

    override def deleteUser(id: UserId): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.delete(id).transact(transactor)
      } yield ()

    override def getSession(sessionId: String) =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.fromTry(Try(UUID.fromString(sessionId)))
        sessionDto <- sessionsRepository.get(id).transact(transactor)
        session <- ZIO.fromEither(sessionDto.map(_.toSession).toRight(SessionNotFound))
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
        _ <- sessionsRepository.insert(auth.SessionDao(uuid, userId)).transact(transactor)
      } yield uuid

    override def deleteSession(id: SessionId) =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- sessionsRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: RLayer[UsersRepository.UsersRepository with SessionsRepository.SessionsRepository, UsersService.UsersService] =
    ZLayer.fromServices[UsersRepository.Service, SessionsRepository.Service, UsersService.Service] {
      (usersRepo, sessionsRepo) => new ServiceImpl(usersRepo, sessionsRepo)
    }
}
