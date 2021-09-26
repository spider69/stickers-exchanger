package org.yusupov.database.repositories.collections

import org.yusupov.database.dao.collections.CollectionDao
import org.yusupov.database.repositories.Repository
import org.yusupov.structures.CollectionId
import zio.{Has, ULayer, ZLayer}

object CollectionsRepository extends Repository {

  import dbContext._

  type CollectionsRepository = Has[Service]

  trait Service {
    def insert(collection: List[CollectionDao]): Result[Unit]

    def get(collectionId: CollectionId): Result[Option[CollectionDao]]

    def getAll: Result[List[CollectionDao]]

    def update(collection: CollectionDao): Result[Unit]

    def delete(collectionId: CollectionId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val collectionsTable = quote {
      querySchema[CollectionDao](""""Collections"""")
    }

    override def insert(collection: List[CollectionDao]): Result[Unit] =
      dbContext.run(liftQuery(collection).foreach(c => collectionsTable.insert(c))).unit

    override def get(collectionId: CollectionId): Result[Option[CollectionDao]] =
      dbContext.run(collectionsTable.filter(_.id == lift(collectionId))).map(_.headOption)

    override def getAll: Result[List[CollectionDao]] =
      dbContext.run(collectionsTable)

    override def update(collection: CollectionDao): Result[Unit] =
      dbContext.run(collectionsTable.filter(_.id == lift(collection.id)).update(lift(collection))).unit

    override def delete(collectionId: CollectionId): Result[Unit] =
      dbContext.run(collectionsTable.filter(_.id == lift(collectionId)).delete).unit
  }

  lazy val live: ULayer[CollectionsRepository.CollectionsRepository] =
    ZLayer.succeed(new ServiceImpl())

}
