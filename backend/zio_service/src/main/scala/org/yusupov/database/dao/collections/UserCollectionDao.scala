package org.yusupov.database.dao.collections

import org.yusupov.structures.{CollectionId, UserId}

case class UserCollectionDao(
  userId: UserId,
  collectionId: CollectionId
)
