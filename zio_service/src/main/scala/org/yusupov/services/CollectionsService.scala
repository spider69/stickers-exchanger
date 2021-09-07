package org.yusupov.services

import org.yusupov.database.repositories.CollectionsRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.structures.{Collection, CollectionId}
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZLayer}

@accessible
object CollectionsService {
  type CollectionsService = Has[Service]

  trait Service {
    def getAll: RIO[TransactorService.DBTransactor, List[Collection]]
    def insert(collection: Collection): RIO[TransactorService.DBTransactor with Random, CollectionId]
    def delete(collectionId: CollectionId): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(collectionsRepository: CollectionsRepository.Service) extends Service {
    import doobie.implicits._

    override def getAll: RIO[TransactorService.DBTransactor, List[Collection]] =
      for {
        transactor <- TransactorService.databaseTransactor
        collections <- collectionsRepository.getAll.transact(transactor)
      } yield collections

    override def insert(collection: Collection): RIO[TransactorService.DBTransactor with Random, CollectionId] =
      for {
        transactor <- TransactorService.databaseTransactor
        uuid <- zio.random.nextUUID.map(_.toString)
        _ <- collectionsRepository.insert(collection.copy(id = uuid)).transact(transactor)
      } yield uuid

    override def delete(collectionId: CollectionId): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- collectionsRepository.delete(collectionId).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[CollectionsRepository.CollectionsRepository, Nothing, CollectionsService.CollectionsService] =
    ZLayer.fromService[CollectionsRepository.Service, CollectionsService.Service](repo => new ServiceImpl(repo))

}
