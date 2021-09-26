package org.yusupov.structures.auth

import org.yusupov.database.dao.auth
import org.yusupov.database.dao.auth.UserDao
import org.yusupov.structures.{Password, UserId}
import org.yusupov.utils.SecurityUtils

case class User(
  id: UserId,
  name: String,
  email: String
) {
  def toDAO(password: Password): UserDao = {
    val salt = SecurityUtils.generateSalt()
    auth.UserDao(id, name, email, SecurityUtils.countSecretHash(password, salt), salt)
  }
}