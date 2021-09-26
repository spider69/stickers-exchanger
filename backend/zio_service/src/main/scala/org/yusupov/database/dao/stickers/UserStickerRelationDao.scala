package org.yusupov.database.dao.stickers

import org.yusupov.structures.stickers.UserStickerRelation

case class UserStickerRelationDao(
  stickerDao: StickerDao,
  count: Int,
  belongsToUser: Boolean
) {
  def toUserStickerRelation = UserStickerRelation(stickerDao.toSticker, count, belongsToUser)
}
