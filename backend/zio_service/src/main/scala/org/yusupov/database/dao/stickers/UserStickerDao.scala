package org.yusupov.database.dao.stickers

import org.yusupov.structures.{CollectionId, StickerId, UserId}

case class UserStickerDao(
  userId: UserId,
  userCollectionId: CollectionId,
  stickerId: StickerId,
  count: Int
)
