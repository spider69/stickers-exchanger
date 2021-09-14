package org.yusupov.services

import org.yusupov.database.repositories.CollectionsRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.structures.{Collection, CollectionId, CollectionInsertion}
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZLayer}

import java.util.UUID

@accessible
object CollectionsService {
  type CollectionsService = Has[Service]

  trait Service {
    def getAll: RIO[TransactorService.DBTransactor, List[Collection]]
    def insert(collection: CollectionInsertion): RIO[TransactorService.DBTransactor with Random, CollectionId]
    def delete(collectionId: String): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(collectionsRepository: CollectionsRepository.Service) extends Service {
    import doobie.implicits._

    override def getAll: RIO[TransactorService.DBTransactor, List[Collection]] =
      for {
        transactor <- TransactorService.databaseTransactor
        collectionsDao <- collectionsRepository.getAll.transact(transactor)
        collections = collectionsDao.map(_.toCollection)
      } yield collections

    override def insert(collection: CollectionInsertion): RIO[TransactorService.DBTransactor with Random, CollectionId] =
      for {
        transactor <- TransactorService.databaseTransactor
        collectionId <- zio.random.nextUUID
        _ <- collectionsRepository.insert(collection.toDao(collectionId)).transact(transactor)
      } yield collectionId

    override def delete(collectionId: String): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id = UUID.fromString(collectionId)
        _ <- collectionsRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[CollectionsRepository.CollectionsRepository, Nothing, CollectionsService.CollectionsService] =
    ZLayer.fromService[CollectionsRepository.Service, CollectionsService.Service](repo => new ServiceImpl(repo))

}
