package org.yusupov.services

import org.yusupov.database.repositories.StickersRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.structures.{Sticker, StickerId}
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZLayer}

@accessible
object StickersService {

  type StickersService = Has[Service]

  trait Service {
    def getAll: RIO[TransactorService.DBTransactor, List[Sticker]]
    def insert(sticker: Sticker): RIO[TransactorService.DBTransactor with Random, StickerId]
    def delete(stickerId: StickerId): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(stickersRepository: StickersRepository.Service) extends Service {
    import doobie.implicits._

    override def getAll: RIO[TransactorService.DBTransactor, List[Sticker]] =
      for {
        transactor <- TransactorService.databaseTransactor
        stickers <- stickersRepository.getAll.transact(transactor)
      } yield stickers

    override def insert(sticker: Sticker): RIO[TransactorService.DBTransactor with Random, String] =
      for {
        transactor <- TransactorService.databaseTransactor
        uuid <- zio.random.nextUUID.map(_.toString)
        _ <- stickersRepository.insert(sticker.copy(id = uuid)).transact(transactor)
      } yield uuid

    override def delete(stickerId: StickerId): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- stickersRepository.delete(stickerId).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[StickersRepository.StickersRepository, Nothing, StickersService.StickersService] =
    ZLayer.fromService[StickersRepository.Service, StickersService.Service](repo => new ServiceImpl(repo))

}
