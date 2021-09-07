package org.yusupov.structures

case class Sticker(
  id: StickerId,
  collectionId: CollectionId,
  description: String = ""
)
