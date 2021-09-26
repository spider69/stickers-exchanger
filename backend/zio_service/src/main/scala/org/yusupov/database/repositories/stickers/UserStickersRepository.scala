package org.yusupov.database.repositories.stickers

import org.yusupov.database.dao.auth.UserDao
import org.yusupov.database.dao.stickers
import org.yusupov.database.dao.stickers.{StickerDao, UserStickerDao, UserStickerRelationDao}
import org.yusupov.database.repositories.Repository
import org.yusupov.structures.{CollectionId, StickerId, UserId}
import zio.macros.accessible
import zio.{Has, ULayer, ZLayer}

object UserStickersRepository extends Repository {

  import dbContext._

  type UserStickersRepository = Has[Service]

  trait Service {
    def addSticker(userId: UserId, sticker: StickerDao, count: Int): Result[Unit]

    def addStickers(userStickers: List[UserStickerDao]): Result[Unit]

    def getStickers(userId: UserId, collectionId: CollectionId): Result[List[StickerDao]]

    def getStickersRelations(userId: UserId, collectionId: CollectionId): Result[List[UserStickerRelationDao]]

    def getUsersBySticker(stickerId: StickerId): Result[List[UserDao]]

    def updateStickerExchangeInfo(userId: UserId, stickerId: StickerId, count: Int): Result[Unit]

    def deleteSticker(userId: UserId, stickerId: StickerId): Result[Unit]

    def deleteCollectionStickers(userId: UserId, collectionId: CollectionId): Result[Unit]
  }

  class ServiceImpl extends Service {
    lazy val usersStickersTable = quote {
      querySchema[UserStickerDao](""""UsersStickers"""")
    }

    lazy val stickersTable = quote {
      querySchema[StickerDao](""""Stickers"""")
    }

    lazy val usersTable = quote {
      querySchema[UserDao](""""Users"""")
    }

    override def addSticker(userId: UserId, sticker: StickerDao, count: Int) =
      dbContext.run(usersStickersTable.insert(lift(stickers.UserStickerDao(userId, sticker.collectionId, sticker.id, count)))).unit

    override def addStickers(userStickers: List[UserStickerDao]) =
      dbContext.run(liftQuery(userStickers).foreach(us => usersStickersTable.insert(us))).unit

    override def getStickers(userId: UserId, collectionId: CollectionId): Result[List[StickerDao]] =
      dbContext.run {
        for {
          userStickers <- usersStickersTable.filter(s => s.userId == lift(userId) && s.userCollectionId == lift(collectionId))
          stickers <- stickersTable.join(_.id == userStickers.stickerId)
        } yield stickers
      }

    def getStickersRelations(userId: UserId, collectionId: CollectionId): Result[List[UserStickerRelationDao]] = {
      val userStickersRelations = dbContext.run {
        for {
          stickers <- stickersTable.filter(_.collectionId == lift(collectionId))
          userStickers <- usersStickersTable.filter(_.userId == lift(userId)).leftJoin(_.stickerId == stickers.id)
        } yield (stickers, userStickers)
      }

      userStickersRelations.map(relations =>
        relations.map {
          case (stickers, userStickers) =>
            UserStickerRelationDao(stickers, userStickers.map(_.count).getOrElse(0), userStickers.isDefined)
        }.sortBy(_.stickerDao.number)
      )
    }

    override def getUsersBySticker(stickerId: StickerId): Result[List[UserDao]] =
      dbContext.run {
        for {
          usersStickers <- usersStickersTable.filter(_.stickerId == lift(stickerId))
          users <- usersTable.join(_.id == usersStickers.userId)
        } yield users
      }

    override def updateStickerExchangeInfo(userId: UserId, stickerId: StickerId, count: Int): Result[Unit] =
      dbContext.run {
        usersStickersTable
          .filter(_.userId == lift(userId))
          .filter(_.stickerId == lift(stickerId))
          .update(_.count -> lift(count))
      }.unit

    override def deleteSticker(userId: UserId, stickerId: StickerId) =
      dbContext.run(
        usersStickersTable
          .filter(_.userId == lift(userId))
          .filter(_.stickerId == lift(stickerId))
          .delete
      ).unit

    override def deleteCollectionStickers(userId: UserId, collectionId: CollectionId): Result[Unit] =
      dbContext.run(
        usersStickersTable
          .filter(_.userCollectionId == lift(collectionId))
          .delete
      ).unit
  }

  lazy val live: ULayer[UserStickersRepository] =
    ZLayer.succeed(new ServiceImpl())
}
