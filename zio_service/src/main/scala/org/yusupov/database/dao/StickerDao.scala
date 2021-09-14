package org.yusupov.database.dao

import org.yusupov.structures.{CollectionId, Sticker, StickerId}

case class StickerDao(
  id: StickerId,
  collectionId: CollectionId,
  number: String,
  description: String = ""
) {
  def toSticker: Sticker = Sticker(id, number, description)
}
