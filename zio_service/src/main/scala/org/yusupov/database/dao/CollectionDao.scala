package org.yusupov.database.dao

import org.yusupov.structures.{Collection, CollectionId}

case class CollectionDao(
  id: CollectionId,
  name: String,
  numberOfStickers: Int,
  description: String = ""
) {
  def toCollection: Collection = Collection(id, name, numberOfStickers, description)
}
