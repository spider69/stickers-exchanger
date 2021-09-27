package org.yusupov.structures.collections

import org.yusupov.database.dao.collections
import org.yusupov.database.dao.collections.CollectionDao
import org.yusupov.structures.CollectionId

sealed trait CollectionInterface {
  val name: String
  val numberOfStickers: Int
  val description: String
  val image: String
}

case class Collection(
  id: CollectionId,
  name: String,
  numberOfStickers: Int,
  description: String = "",
  image: String
) extends CollectionInterface {
  def toDao: CollectionDao =
    collections.CollectionDao(id, name, numberOfStickers, description, image)
}

case class CollectionInsertion(
  name: String,
  numberOfStickers: Int,
  description: String = "",
  image: String
) extends CollectionInterface {
  def toDao(collectionId: CollectionId): CollectionDao =
    collections.CollectionDao(collectionId, name, numberOfStickers, description, image)
}
