package org.yusupov.database.repositories

import org.yusupov.database.dao.{CollectionDao, StickerDao, UsersCollectionDao}
import org.yusupov.structures.{StickerId, UserId}
import zio.{Has, ULayer, ZLayer}

object UsersCollectionsRepository extends Repository {

  import dbContext._

  type UsersCollectionsRepository = Has[Service]

  trait Service {
    def getStickers(userId: UserId): Result[List[StickerDao]]
    def insertSticker(userId: UserId, sticker: StickerDao): Result[Unit]
    def deleteSticker(userId: UserId, stickerId: StickerId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val collectionsTable = quote {
      querySchema[CollectionDao](""""Collections"""")
    }

    lazy val stickersTable = quote {
      querySchema[StickerDao](""""Stickers"""")
    }

    lazy val usersCollectionsTable = quote {
      querySchema[UsersCollectionDao](""""UsersCollections"""")
    }

    override def getStickers(userId: UserId): Result[List[StickerDao]] =
      dbContext.run {
        for {
          users <- usersCollectionsTable.filter(_.userId == lift(userId))
          collections <- collectionsTable.join(_.id == users.collectionId)
          stickers <- stickersTable.join(_.collectionId == collections.id)
        } yield stickers
      }

    override def insertSticker(userId: UserId, sticker: StickerDao) =
      dbContext.run(usersCollectionsTable.insert(lift(UsersCollectionDao(userId, sticker.collectionId, sticker.id)))).unit

    override def deleteSticker(userId: UserId, stickerId: StickerId) =
      dbContext.run(
        usersCollectionsTable
          .filter(_.userId == lift(userId))
          .filter(_.stickerId == lift(stickerId))
          .delete
      ).unit
  }

  lazy val live: ULayer[UsersCollectionsRepository] =
    ZLayer.succeed(new ServiceImpl())

}
