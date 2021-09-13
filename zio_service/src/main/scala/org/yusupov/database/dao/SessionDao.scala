package org.yusupov.database.dao

import org.yusupov.structures.{Session, SessionId, UserId}

case class SessionDao(
  id: SessionId,
  userId: UserId
) {
  def toSession: Session = Session(id, userId)
}
