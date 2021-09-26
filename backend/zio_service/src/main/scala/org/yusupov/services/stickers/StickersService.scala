package org.yusupov.services.stickers

import org.yusupov.database.repositories.stickers.StickersRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.errors.StickerNotFound
import org.yusupov.structures.StickerId
import org.yusupov.structures.stickers.{Sticker, StickerInsertion}
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZIO, ZLayer}

import java.util.UUID
import scala.util.Try

@accessible
object StickersService {

  type StickersService = Has[Service]

  trait Service {
    def get(stickerId: String): RIO[TransactorService.DBTransactor, Sticker]

    def getForCollection(collectionId: String): RIO[TransactorService.DBTransactor, List[Sticker]]

    def insert(collectionId: String, sticker: StickerInsertion): RIO[TransactorService.DBTransactor with Random, StickerId]

    def delete(stickerId: String): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(stickersRepository: StickersRepository.Service) extends Service {

    import doobie.implicits._

    override def get(stickerId: String): RIO[TransactorService.DBTransactor, Sticker] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(stickerId))
        stickerDao <- stickersRepository.get(id).transact(transactor)
        sticker <- ZIO.fromEither(stickerDao.map(_.toSticker).toRight(StickerNotFound))
      } yield sticker

    override def getForCollection(collectionId: String): RIO[TransactorService.DBTransactor, List[Sticker]] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        stickersDao <- stickersRepository.getAll(id).transact(transactor)
        stickers = stickersDao.map(_.toSticker)
      } yield stickers

    override def insert(collectionIdAsString: String, sticker: StickerInsertion): RIO[TransactorService.DBTransactor with Random, StickerId] =
      for {
        transactor <- TransactorService.databaseTransactor
        stickerId <- zio.random.nextUUID
        collectionId <- ZIO.effect(UUID.fromString(collectionIdAsString))
        _ <- stickersRepository.insert(List(sticker.toDao(stickerId, collectionId))).transact(transactor)
      } yield stickerId

    override def delete(stickerId: String): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(stickerId))
        _ <- stickersRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[StickersRepository.StickersRepository, Nothing, StickersService.StickersService] =
    ZLayer.fromService[StickersRepository.Service, StickersService.Service](repo => new ServiceImpl(repo))

}
