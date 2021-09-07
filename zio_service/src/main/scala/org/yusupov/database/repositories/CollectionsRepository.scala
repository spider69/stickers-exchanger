package org.yusupov.database.repositories

import doobie.quill.DoobieContext
import io.getquill.{CompositeNamingStrategy2, Escape, Literal}
import org.yusupov.database.services.TransactorService
import org.yusupov.structures.{Collection, CollectionId}
import zio.{Has, ULayer, ZLayer}

object CollectionsRepository extends RepositoryImplicits {

  lazy val dbContext: DoobieContext.Postgres[CompositeNamingStrategy2[Escape.type, Literal.type]] = TransactorService.doobieContext
  import dbContext._

  type CollectionsRepository = Has[Service]

  trait Service {
    def get(collectionId: CollectionId): Result[Option[Collection]]
    def getAll: Result[List[Collection]]
    def insert(collection: Collection): Result[Unit]
    def update(collection: Collection): Result[Unit]
    def delete(collectionId: CollectionId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val collectionsTable = quote {
      querySchema[Collection](""""Collections"""")
    }

    override def get(collectionId: CollectionId): Result[Option[Collection]] =
      dbContext.run(collectionsTable.filter(_.id == lift(collectionId))).map(_.headOption)

    override def getAll: Result[List[Collection]] =
      dbContext.run(collectionsTable)

    override def insert(collection: Collection): Result[Unit] =
      dbContext.run(collectionsTable.insert(lift(collection))).unit

    override def update(collection: Collection): Result[Unit] =
      dbContext.run(collectionsTable.filter(_.id == lift(collection.id)).update(lift(collection))).unit

    override def delete(collectionId: CollectionId): Result[Unit] =
      dbContext.run(collectionsTable.filter(_.id == lift(collectionId)).delete).unit
  }

  lazy val live: ULayer[CollectionsRepository.CollectionsRepository] =
    ZLayer.succeed(new ServiceImpl())

}
