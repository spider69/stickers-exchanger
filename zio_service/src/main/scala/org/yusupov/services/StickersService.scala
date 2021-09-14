package org.yusupov.services

import org.yusupov.database.repositories.StickersRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.structures.{Sticker, StickerId, StickerInsertion}
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZLayer}

import java.util.UUID

@accessible
object StickersService {

  type StickersService = Has[Service]

  trait Service {
    def getAll: RIO[TransactorService.DBTransactor, List[Sticker]]
    def insert(collectionId: String, sticker: StickerInsertion): RIO[TransactorService.DBTransactor with Random, StickerId]
    def delete(stickerId: String): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(stickersRepository: StickersRepository.Service) extends Service {
    import doobie.implicits._

    override def getAll: RIO[TransactorService.DBTransactor, List[Sticker]] =
      for {
        transactor <- TransactorService.databaseTransactor
        stickersDao <- stickersRepository.getAll.transact(transactor)
        stickers = stickersDao.map(_.toSticker)
      } yield stickers

    override def insert(collectionIdAsString: String, sticker: StickerInsertion): RIO[TransactorService.DBTransactor with Random, StickerId] =
      for {
        transactor <- TransactorService.databaseTransactor
        stickerId <- zio.random.nextUUID
        collectionId = UUID.fromString(collectionIdAsString)
        _ <- stickersRepository.insert(sticker.toDao(stickerId, collectionId)).transact(transactor)
      } yield stickerId

    override def delete(stickerId: String): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id = UUID.fromString(stickerId)
        _ <- stickersRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[StickersRepository.StickersRepository, Nothing, StickersService.StickersService] =
    ZLayer.fromService[StickersRepository.Service, StickersService.Service](repo => new ServiceImpl(repo))

}
