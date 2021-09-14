package org.yusupov.structures

import org.yusupov.database.dao.StickerDao

case class Sticker(
  id: StickerId,
  number: String,
  description: String = ""
) {
  def toDao(collectionId: CollectionId): StickerDao =
    StickerDao(id, collectionId, number, description)
}

case class StickerInsertion(
  number: String,
  description: String = ""
) {
  def toDao(id: StickerId, collectionId: CollectionId): StickerDao =
    StickerDao(id, collectionId, number, description)
}
