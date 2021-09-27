package org.yusupov.database.dao.auth

import org.yusupov.structures.{UserId, auth}
import org.yusupov.structures.auth.User

case class UserDao(
  id: UserId,
  name: String,
  email: String,
  passwordHash: String,
  salt: Array[Byte]
) {
  def toUser: User = auth.User(id, name, email)
}
