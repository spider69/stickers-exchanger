package org.yusupov.database.repositories.stickers

import org.yusupov.database.dao.stickers.StickerDao
import org.yusupov.database.repositories.Repository
import org.yusupov.structures.{CollectionId, StickerId}
import zio.{Has, ULayer, ZLayer}

object StickersRepository extends Repository {

  import dbContext._

  type StickersRepository = Has[Service]

  trait Service {
    def insert(stickers: List[StickerDao]): Result[Unit]

    def get(stickerId: StickerId): Result[Option[StickerDao]]

    def getAll(collectionId: CollectionId): Result[List[StickerDao]]

    def update(sticker: StickerDao): Result[Unit]

    def delete(stickerId: StickerId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val stickersTable = quote {
      querySchema[StickerDao](""""Stickers"""")
    }

    override def get(stickerId: StickerId): Result[Option[StickerDao]] =
      dbContext.run(stickersTable.filter(_.id == lift(stickerId))).map(_.headOption)

    override def getAll(collectionId: CollectionId): Result[List[StickerDao]] =
      dbContext.run(stickersTable.filter(_.collectionId == lift(collectionId)).sortBy(_.number))

    override def insert(stickers: List[StickerDao]): Result[Unit] =
      dbContext.run(liftQuery(stickers).foreach(s => stickersTable.insert(s))).unit

    override def update(sticker: StickerDao): Result[Unit] =
      dbContext.run(stickersTable.filter(_.id == lift(sticker.id)).update(lift(sticker))).unit

    override def delete(stickerId: StickerId): Result[Unit] =
      dbContext.run(stickersTable.filter(_.id == lift(stickerId)).delete).unit
  }

  lazy val live: ULayer[StickersRepository.StickersRepository] =
    ZLayer.succeed(new ServiceImpl())

}
