package org.yusupov.database.dao.collections

import org.yusupov.structures.collections.UserCollectionRelation

case class UserCollectionRelationDao(
  collectionDao: CollectionDao,
  belongsToUser: Boolean
) {
  def toUserCollectionRelation: UserCollectionRelation =
    UserCollectionRelation(collectionDao.toCollection, belongsToUser)
}
