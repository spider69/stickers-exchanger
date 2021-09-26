package org.yusupov.database.dao.stickers

import org.yusupov.structures.stickers.Sticker
import org.yusupov.structures.{CollectionId, StickerId, stickers}

case class StickerDao(
  id: StickerId,
  collectionId: CollectionId,
  number: String,
  description: String = "",
  image: String
) {
  def toSticker: Sticker = stickers.Sticker(id, number, description, image)
}
