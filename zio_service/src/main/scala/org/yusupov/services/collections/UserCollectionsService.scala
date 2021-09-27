package org.yusupov.services.collections

import org.yusupov.database.repositories.collections.UserCollectionsRepository
import org.yusupov.database.repositories.collections.UserCollectionsRepository.UserCollectionsRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.structures.UserId
import org.yusupov.structures.collections.{Collection, UserCollectionRelation}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, RLayer, ZIO, ZLayer}

import java.util.UUID

@accessible
object UserCollectionsService {

  type UserCollectionsService = Has[Service]

  trait Service {
    def addUserCollection(userId: UserId, collectionId: String): RIO[DBTransactor, Unit]

    def getUserCollections(userId: String): RIO[DBTransactor, List[Collection]]

    def getUserCollectionsRelations(userId: String): RIO[DBTransactor, List[UserCollectionRelation]]

    def deleteUserCollection(userId: UserId, collectionId: String): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    userCollectionsRepository: UserCollectionsRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def addUserCollection(userId: UserId, collectionId: String) =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        _ <- userCollectionsRepository.addCollection(userId, id).transact(transactor)
      } yield ()

    override def getUserCollections(userId: String) =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(userId))
        collectionsDao <- userCollectionsRepository.getCollections(id).transact(transactor)
        collections = collectionsDao.map(_.toCollection)
      } yield collections

    override def getUserCollectionsRelations(userId: String) =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(userId))
        collectionsRelationsDao <- userCollectionsRepository.getCollectionsRelations(id).transact(transactor)
        collectionsRelations = collectionsRelationsDao.map(_.toUserCollectionRelation)
      } yield collectionsRelations

    override def deleteUserCollection(userId: UserId, collectionId: String) =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(collectionId))
        _ <- userCollectionsRepository.deleteCollection(userId, id).transact(transactor)
      } yield ()
  }

  lazy val live: RLayer[UserCollectionsRepository, UserCollectionsService] =
    ZLayer.fromService[UserCollectionsRepository.Service, UserCollectionsService.Service] {
      usersCollectionsRepo => new ServiceImpl(usersCollectionsRepo)
    }
}
