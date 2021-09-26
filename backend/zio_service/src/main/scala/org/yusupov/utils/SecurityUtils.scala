package org.yusupov.utils

import java.security.SecureRandom

import org.apache.commons.codec.digest.DigestUtils

object SecurityUtils {
  def countSecretHash(secret: String, salt: Array[Byte]): String =
    DigestUtils.sha512Hex(secret.getBytes ++ salt)

  def checkSecret(secret: String, salt: Array[Byte], passwordHash: String): Boolean =
    DigestUtils.sha512Hex(secret.getBytes ++ salt) == passwordHash

  def generateSalt(length: Int = 64) = {
    val salt = Array.fill(length)(0.byteValue())
    new SecureRandom().nextBytes(salt)
    salt
  }
}
