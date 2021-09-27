package org.yusupov

package object errors {

  abstract class Error(val message: String) extends Throwable(message)

  case object SessionCookieIsAbsent extends Error("Session cookie is absent")
  case object SessionNotFound extends Error("Session not found")
  case object UserNotFound extends Error("User not found for session")

  case class UserNotExist(name: String) extends Error(s"User with name=$name does not exist")
  case object IncorrectUserPassword extends Error("User password is incorrect")

  case object StickerNotFound extends Error("Sticker not found")
  case object BadStickersCount extends Error("Stickers count must be >= 0")
  case object CollectionNotFound extends Error("Collection not found")
}
