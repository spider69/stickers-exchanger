package org.yusupov.structures.auth

case class UserWithSession(
  user: User,
  session: Session
)
