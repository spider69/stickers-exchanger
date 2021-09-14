package org.yusupov.database.dao

import org.yusupov.structures.{CollectionId, StickerId, UserId}

case class UsersCollectionDao(
  userId: UserId,
  collectionId: CollectionId,
  stickerId: StickerId
)
