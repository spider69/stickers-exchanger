package org.yusupov.database.dao.auth

import org.yusupov.structures.auth.Session
import org.yusupov.structures.{SessionId, UserId, auth}

case class SessionDao(
  id: SessionId,
  userId: UserId
) {
  def toSession: Session = auth.Session(id, userId)
}
