package org.yusupov.structures.stickers

case class UserStickerRelation(
  sticker: Sticker,
  count: Int,
  belongsToUser: Boolean
)
