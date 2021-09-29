package org.yusupov.services.stickers

import org.yusupov.database.repositories.stickers.StickersRepository.StickersRepository
import org.yusupov.database.repositories.stickers.UserStickersRepository.UserStickersRepository
import org.yusupov.database.repositories.stickers.{StickersRepository, UserStickersRepository}
import org.yusupov.database.services.TransactorService
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.errors.{BadStickersCount, StickerNotFound}
import org.yusupov.services.collections.UserCollectionsService
import org.yusupov.services.collections.UserCollectionsService.UserCollectionsService
import org.yusupov.structures.UserId
import org.yusupov.structures.auth.User
import org.yusupov.structures.stickers.{Sticker, UserStickerRelation}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, RLayer, ZIO, ZLayer}

import java.util.UUID

@accessible
object UserStickersService {

  type UserStickersService = Has[Service]

  trait Service {
    def addUserSticker(userId: UserId, stickerId: String, count: Int): RIO[DBTransactor with UserCollectionsService, Unit]

    def getUserStickers(userId: UserId, collectionId: String): RIO[DBTransactor, List[Sticker]]

    def getUserStickersRelations(userId: String, collectionId: String): RIO[DBTransactor, List[UserStickerRelation]]

    def getUsersBySticker(stickerId: String): RIO[DBTransactor, List[User]]

    def updateStickersCount(userId: UserId, stickerId: String, count: Int): RIO[DBTransactor, Unit]

    def deleteUserSticker(userId: UserId, stickerId: String): RIO[DBTransactor, Unit]

    def deleteUserCollectionStickers(userId: UserId, collectionId: String): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    stickersRepository: StickersRepository.Service,
    userStickersRepository: UserStickersRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def addUserSticker(userId: UserId, stickerId: String, count: Int): RIO[DBTransactor with UserCollectionsService, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(stickerId))
        stickerOpt <- stickersRepository.get(id).transact(transactor)
        sticker <- ZIO.fromOption(stickerOpt).orElseFail(StickerNotFound)
        _ <- UserCollectionsService.addUserCollection(userId, sticker.collectionId.toString)
        _ <- userStickersRepository.addSticker(userId, sticker, count).transact(transactor)
      } yield ()

    override def getUserStickers(userId: UserId, collectionId: String): RIO[DBTransactor, List[Sticker]] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        stickersDao <- userStickersRepository.getStickers(userId, id).transact(transactor)
        stickers = stickersDao.map(_.toSticker)
      } yield stickers

    override def getUserStickersRelations(userId: String, collectionId: String): RIO[DBTransactor, List[UserStickerRelation]] =
      for {
        transactor <- TransactorService.databaseTransactor
        uId <- ZIO.effect(UUID.fromString(userId))
        colId <- ZIO.effect(UUID.fromString(collectionId))
        stickerRelationsDao <- userStickersRepository.getStickersRelations(uId, colId).transact(transactor)
        stickersRelations = stickerRelationsDao.map(_.toUserStickerRelation)
      } yield stickersRelations

    override def getUsersBySticker(stickerId: String): RIO[DBTransactor, List[User]] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(stickerId))
        usersDao <- userStickersRepository.getUsersByStickers(List(id)).transact(transactor)
        users = usersDao.map(_._1.toUser)
      } yield users

    override def updateStickersCount(userId: UserId, stickerId: String, count: Int): RIO[DBTransactor, Unit] =
      for {
        _ <- ZIO.when(count < 0)(ZIO.fail(BadStickersCount))
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(stickerId))
        _ <- userStickersRepository.updateStickerExchangeInfo(userId, id, count).transact(transactor)
      } yield ()

    override def deleteUserSticker(userId: UserId, stickerId: String): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(stickerId))
        _ <- userStickersRepository.deleteSticker(userId, id).transact(transactor)
      } yield ()

    override def deleteUserCollectionStickers(userId: UserId, collectionId: String): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        _ <- userStickersRepository.deleteCollectionStickers(userId, id).transact(transactor)
      } yield ()
  }

  lazy val live: RLayer[StickersRepository with UserStickersRepository, UserStickersService] =
    ZLayer.fromServices[StickersRepository.Service, UserStickersRepository.Service, UserStickersService.Service] {
      (stickersRepo, userStickersRepo) => new ServiceImpl(stickersRepo, userStickersRepo)
    }
}
