package org.yusupov.services

import org.yusupov.database.repositories.StickersRepository.StickersRepository
import org.yusupov.database.repositories.UsersCollectionsRepository.UsersCollectionsRepository
import org.yusupov.database.repositories.{StickersRepository, UsersCollectionsRepository}
import org.yusupov.database.services.TransactorService
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.errors.StickerNotFound
import org.yusupov.structures.{Sticker, StickerId, UserId}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, RLayer, ZIO, ZLayer}

import java.util.UUID
import scala.util.Try

@accessible
object UsersCollectionsService {

  type UsersCollectionsService = Has[Service]

  trait Service {
    def getUserStickers(userId: UserId): RIO[DBTransactor, List[Sticker]]
    def addUserSticker(userId: UserId, stickerId: String): RIO[DBTransactor, Unit]
    def deleteUserSticker(userId: UserId, stickerId: String): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    stickersRepository: StickersRepository.Service,
    usersCollectionsRepository: UsersCollectionsRepository.Service
  ) extends Service {
    import doobie.implicits._

    override def getUserStickers(userId: UserId): RIO[DBTransactor, List[Sticker]] =
      for {
        transactor <- TransactorService.databaseTransactor
        stickersDao <- usersCollectionsRepository.getStickers(userId).transact(transactor)
        stickers = stickersDao.map(_.toSticker)
      } yield stickers

    override def addUserSticker(userId: UserId, stickerId: String): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.fromTry(Try(UUID.fromString(stickerId)))
        stickerOpt <- stickersRepository.get(id).transact(transactor)
        sticker <- ZIO.fromOption(stickerOpt).orElseFail(StickerNotFound)
        _ <- usersCollectionsRepository.insertSticker(userId, sticker).transact(transactor)
      } yield ()

    override def deleteUserSticker(userId: UserId, stickerId: String): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.fromTry(Try(UUID.fromString(stickerId)))
        _ <- usersCollectionsRepository.deleteSticker(userId, id).transact(transactor)
      } yield ()
  }

  lazy val live: RLayer[StickersRepository with UsersCollectionsRepository, UsersCollectionsService] =
    ZLayer.fromServices[StickersRepository.Service, UsersCollectionsRepository.Service, UsersCollectionsService.Service] {
      (stickersRepo, usersCollectionsRepo) => new ServiceImpl(stickersRepo, usersCollectionsRepo)
    }
}
