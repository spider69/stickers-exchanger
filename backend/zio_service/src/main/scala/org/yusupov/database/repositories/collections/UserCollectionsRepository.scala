package org.yusupov.database.repositories.collections

import org.yusupov.database.dao.collections
import org.yusupov.database.dao.collections.{CollectionDao, UserCollectionDao, UserCollectionRelationDao}
import org.yusupov.database.repositories.Repository
import org.yusupov.structures.{CollectionId, UserId}
import zio.{Has, ULayer, ZLayer}

object UserCollectionsRepository extends Repository {

  import dbContext._

  type UserCollectionsRepository = Has[Service]

  trait Service {
    type Effect[A] = Result[A]

    def addCollection(userId: UserId, collectionId: CollectionId): Effect[Unit]

    def addCollections(userCollections: List[UserCollectionDao]): Effect[Unit]

    def getCollections(userId: UserId): Effect[List[CollectionDao]]

    def getCollectionsRelations(userId: UserId): Effect[List[UserCollectionRelationDao]]

    def deleteCollection(userId: UserId, collectionId: CollectionId): Effect[Unit]
  }

  class ServiceImpl extends Service {
    lazy val usersCollectionsTable = quote {
      querySchema[UserCollectionDao](""""UsersCollections"""")
    }

    lazy val collectionsTable = quote {
      querySchema[CollectionDao](""""Collections"""")
    }

    override def addCollection(userId: UserId, collectionId: CollectionId) =
      dbContext.run(usersCollectionsTable.insert(lift(collections.UserCollectionDao(userId, collectionId))).onConflictIgnore).unit

    override def addCollections(userCollections: List[UserCollectionDao]) =
      dbContext.run(liftQuery(userCollections).foreach(uc => usersCollectionsTable.insert(uc))).unit

    override def getCollections(userId: UserId): Result[List[CollectionDao]] =
      dbContext.run {
        for {
          userCollections <- usersCollectionsTable.filter(_.userId == lift(userId))
          collections <- collectionsTable.join(_.id == userCollections.collectionId)
        } yield collections
      }

    override def getCollectionsRelations(userId: UserId): Result[List[UserCollectionRelationDao]] = {
      val userCollectionsRelations = dbContext.run {
        for {
          collections <- collectionsTable
          userCollection <- usersCollectionsTable.filter(_.userId == lift(userId)).leftJoin(_.collectionId == collections.id)
        } yield (collections, userCollection)
      }

      userCollectionsRelations.map(relations =>
        relations.map {
          case (collection, userCollection) => UserCollectionRelationDao(collection, userCollection.isDefined)
        }
      )
    }

    override def deleteCollection(userId: UserId, collectionId: CollectionId) =
      dbContext.run {
        usersCollectionsTable
          .filter(_.userId == lift(userId))
          .filter(_.collectionId == lift(collectionId))
          .delete
      }.unit
  }

  lazy val live: ULayer[UserCollectionsRepository] =
    ZLayer.succeed(new ServiceImpl())
}
