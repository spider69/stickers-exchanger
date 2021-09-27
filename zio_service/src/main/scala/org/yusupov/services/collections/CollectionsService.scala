package org.yusupov.services.collections

import org.yusupov.database.repositories.collections.CollectionsRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.errors.CollectionNotFound
import org.yusupov.structures.collections.{Collection, CollectionInsertion}
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, ZIO, ZLayer}

import java.util.UUID

@accessible
object CollectionsService {
  type CollectionsService = Has[Service]

  trait Service {
    def get(collectionId: String): RIO[TransactorService.DBTransactor, Collection]

    def getAll: RIO[TransactorService.DBTransactor, List[Collection]]

    def insert(collection: List[CollectionInsertion]): RIO[TransactorService.DBTransactor with Random, Unit]

    def delete(collectionId: String): RIO[TransactorService.DBTransactor, Unit]
  }

  class ServiceImpl(collectionsRepository: CollectionsRepository.Service) extends Service {

    import doobie.implicits._

    override def get(collectionId: String): RIO[TransactorService.DBTransactor, Collection] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        collectionDao <- collectionsRepository.get(id).transact(transactor)
        collection <- ZIO.fromEither(collectionDao.map(_.toCollection).toRight(CollectionNotFound))
      } yield collection

    override def getAll: RIO[TransactorService.DBTransactor, List[Collection]] =
      for {
        transactor <- TransactorService.databaseTransactor
        collectionsDao <- collectionsRepository.getAll.transact(transactor)
        collections = collectionsDao.map(_.toCollection)
      } yield collections

    override def insert(collections: List[CollectionInsertion]): RIO[TransactorService.DBTransactor with Random, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        collectionIds <- ZIO.foreach(collections)(_ => zio.random.nextUUID)
        _ <- collectionsRepository.insert(collectionIds.zip(collections).map { case (id, c) => c.toDao(id) }).transact(transactor)
      } yield ()

    override def delete(collectionId: String): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        _ <- collectionsRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[CollectionsRepository.CollectionsRepository, Nothing, CollectionsService.CollectionsService] =
    ZLayer.fromService[CollectionsRepository.Service, CollectionsService.Service](repo => new ServiceImpl(repo))

}
