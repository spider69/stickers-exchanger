package org.yusupov.database.dao.collections

import org.yusupov.structures.{CollectionId, collections}
import org.yusupov.structures.collections.Collection

case class CollectionDao(
  id: CollectionId,
  name: String,
  numberOfStickers: Int,
  description: String = "",
  image: String
) {
  def toCollection: Collection = collections.Collection(id, name, numberOfStickers, description, image)
}
