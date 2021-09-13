package org.yusupov.database.dao

import org.yusupov.structures.{User, UserId}

case class UserDao(
  id: UserId,
  name: String,
  passwordHash: String,
  salt: Array[Byte]
) {
  def toUser: User = User(id, name)
}
