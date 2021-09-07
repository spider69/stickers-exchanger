package org.yusupov.structures

case class Collection(
  id: CollectionId,
  name: String,
  numberOfStickers: Int,
  description: String = ""
)
