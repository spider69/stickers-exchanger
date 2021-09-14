package org.yusupov.structures

import org.yusupov.database.dao.CollectionDao

case class Collection(
  id: CollectionId,
  name: String,
  numberOfStickers: Int,
  description: String = ""
) {
  def toDao: CollectionDao =
    CollectionDao(id, name, numberOfStickers, description)
}

case class CollectionInsertion(
  name: String,
  numberOfStickers: Int,
  description: String = ""
) {
  def toDao(collectionId: CollectionId): CollectionDao =
    CollectionDao(collectionId, name, numberOfStickers, description)
}
