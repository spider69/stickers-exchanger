package org.yusupov.database.repositories

import org.yusupov.structures.{Sticker, StickerId}
import zio.{Has, ULayer, ZLayer}

object StickersRepository extends Repository {

  import dbContext._

  type StickersRepository = Has[Service]

  trait Service {
    def get(stickerId: StickerId): Result[Option[Sticker]]
    def getAll: Result[List[Sticker]]
    def insert(sticker: Sticker): Result[Unit]
    def update(sticker: Sticker): Result[Unit]
    def delete(stickerId: StickerId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val stickersTable = quote {
      querySchema[Sticker](""""Stickers"""")
    }

    override def get(stickerId: StickerId): Result[Option[Sticker]] =
      dbContext.run(stickersTable.filter(_.id == lift(stickerId))).map(_.headOption)

    override def getAll: Result[List[Sticker]] =
      dbContext.run(stickersTable)

    override def insert(sticker: Sticker): Result[Unit] =
      dbContext.run(stickersTable.insert(lift(sticker))).unit

    override def update(sticker: Sticker): Result[Unit] =
      dbContext.run(stickersTable.filter(_.id == lift(sticker.id)).update(lift(sticker))).unit

    override def delete(stickerId: StickerId): Result[Unit] =
      dbContext.run(stickersTable.filter(_.id == lift(stickerId)).delete).unit
  }

  lazy val live: ULayer[StickersRepository.StickersRepository] =
    ZLayer.succeed(new ServiceImpl())

}
