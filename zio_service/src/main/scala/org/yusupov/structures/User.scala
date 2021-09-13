package org.yusupov.structures

import org.yusupov.database.dao.UserDao
import org.yusupov.utils.SecurityUtils

case class User(
  id: UserId,
  name: String
) {
  def toDAO(password: Password): UserDao = {
    val salt = SecurityUtils.generateSalt()
    UserDao(id, name, SecurityUtils.countSecretHash(password, salt), salt)
  }
}
