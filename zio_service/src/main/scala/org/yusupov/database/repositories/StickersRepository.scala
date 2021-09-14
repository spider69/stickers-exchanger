package org.yusupov.database.repositories

import org.yusupov.database.dao.StickerDao
import org.yusupov.structures.StickerId
import zio.{Has, ULayer, ZLayer}

object StickersRepository extends Repository {

  import dbContext._

  type StickersRepository = Has[Service]

  trait Service {
    def get(stickerId: StickerId): Result[Option[StickerDao]]
    def getAll: Result[List[StickerDao]]
    def insert(sticker: StickerDao): Result[Unit]
    def update(sticker: StickerDao): Result[Unit]
    def delete(stickerId: StickerId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val stickersTable = quote {
      querySchema[StickerDao](""""Stickers"""")
    }

    override def get(stickerId: StickerId): Result[Option[StickerDao]] =
      dbContext.run(stickersTable.filter(_.id == lift(stickerId))).map(_.headOption)

    override def getAll: Result[List[StickerDao]] =
      dbContext.run(stickersTable)

    override def insert(sticker: StickerDao): Result[Unit] =
      dbContext.run(stickersTable.insert(lift(sticker))).unit

    override def update(sticker: StickerDao): Result[Unit] =
      dbContext.run(stickersTable.filter(_.id == lift(sticker.id)).update(lift(sticker))).unit

    override def delete(stickerId: StickerId): Result[Unit] =
      dbContext.run(stickersTable.filter(_.id == lift(stickerId)).delete).unit
  }

  lazy val live: ULayer[StickersRepository.StickersRepository] =
    ZLayer.succeed(new ServiceImpl())

}
