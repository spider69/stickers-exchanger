package org.yusupov.structures.auth

import org.yusupov.structures.{SessionId, UserId}

case class Session(
  id: SessionId,
  userId: UserId
)
