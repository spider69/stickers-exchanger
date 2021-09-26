package org.yusupov.structures.stickers

import org.yusupov.database.dao.stickers
import org.yusupov.database.dao.stickers.StickerDao
import org.yusupov.structures.{CollectionId, StickerId}

trait StickerInterface {
  val number: String
  val description: String
  val image: String
}

case class Sticker(
  id: StickerId,
  number: String,
  description: String = "",
  image: String
) extends StickerInterface {
  def toDao(collectionId: CollectionId): StickerDao =
    stickers.StickerDao(id, collectionId, number, description, image)
}

case class StickerInsertion(
  number: String,
  description: String = "",
  image: String
) extends StickerInterface {
  def toDao(id: StickerId, collectionId: CollectionId): StickerDao =
    stickers.StickerDao(id, collectionId, number, description, image)
}
