package org.yusupov.learning

import zio.Cause.Both
import zio.console.Console
import zio.{IO, Task, UIO, ZIO}

import java.io.IOException
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object ErrorsInZIO {

  // error vs defect
  // error ожидаемая ошибка, фиксируемая на уровне типа
  val eff: IO[NumberFormatException, Int] = ???

  // defect непредвиденная ошибка, не знаем как восстановиться
  val deff: UIO[Int] = ???

  def readFile(fileName: String): ZIO[Any, IOException, List[String]] = ??? // ZIO[Any, Nothing, List[String]]

  trait Config

  def parseConfig(fileName: String): ZIO[Any, IOException, Config] = ???


  object Otus {
    /* ZIO[-R, +E, +A] ----> R => Either[E, A]
    val ff: String => Either[Throwable, Int] = ???
    ff("") // = ZIO[String, Throwable, Int] */

    case class MyZIO[-R, +E, +A](run: R => Either[E, A]) {
      self =>

      def foldM[R1 <: R, E1, B](
        failure: E => MyZIO[R1, E1, B],
        success: A => MyZIO[R1, E1, B]
      ): MyZIO[R1, E1, B] =
        MyZIO { r =>
          self
            .run(r)
            .fold(failure, success)
            .run(r)
        }

      def orElse[R1 <: R, E1, A1 >: A](other: MyZIO[R1, E1, A1]): MyZIO[R1, E1, A1] =
        foldM(
          _ => other,
          a => MyZIO(_ => Right(a))
        )

      def option: MyZIO[R, Nothing, Option[A]] =
        foldM(
          _ => MyZIO(_ => Right(None)),
          a => MyZIO(_ => Right(Some(a)))
        )

      def mapError[E1](f: E => E1): MyZIO[R, E1, A] =
        foldM(
          e => MyZIO(_ => Left(f(e))),
          a => MyZIO(_ => Right(a))
        )

      def map[B](f: A => B): MyZIO[R, E, B] =
        flatMap(a => MyZIO(_ => Right(f(a))))
      //ZIO(r => self.run(r).map(f))

      def flatMap[R1 <: R, E1 >: E, B](f: A => MyZIO[R1, E1, B]): MyZIO[R1, E1, B] =
        MyZIO(r => self.run(r).fold(MyZIO.fail, f).run(r))
    }

    object MyZIO {

      def effect[A](value: => A): MyZIO[Any, Throwable, A] = Try {
        MyZIO((_: Any) => Right(value))
      } match {
        case Failure(exception) =>
          MyZIO(_ => Left(exception))
        case Success(value) =>
          value
      }

      def fail[E](e: E): MyZIO[Any, E, Nothing] = MyZIO(_ => Left(e))

    }

    val echo: MyZIO[Any, Throwable, Unit] = for {
      str <- MyZIO.effect(StdIn.readLine())
      _ <- MyZIO.effect(println(str))
    } yield ()
  }

  object Errors {
    sealed trait UserRegistrationError

    case object InvalidEmail extends UserRegistrationError

    case object WeakPassword extends UserRegistrationError

    lazy val checkEmail: IO[InvalidEmail.type, String] = ???
    lazy val checkPassword: IO[WeakPassword.type, String] = ???
    lazy val userRegistrationCheck: ZIO[Any, UserRegistrationError, (String, String)] = checkEmail.zip(checkPassword)

    lazy val io1: IO[String, String] = ???
    lazy val io2: IO[Int, String] = ???
    val _: ZIO[Any, Any, (String, String)] = io1.zip(io2)
    val io3: ZIO[Any, Either[String, Int], (String, String)] = io1.mapError(Left(_)) zip io2.mapError(Right(_))

    def either: Either[String, Int] = ???

    def errorToErrorCode(str: String): Int = ???

    lazy val effFromEither: IO[String, Int] = zio.ZIO.fromEither(either)
    lazy val _: ZIO[Console, String, Int] = effFromEither.tapError { str =>
      zio.console.putStrLn(str)
    }

    lazy val _: ZIO[Any, Int, Int] = effFromEither.mapError(errorToErrorCode)

    lazy val effEitherErrorOrResult: UIO[Either[String, Int]] = effFromEither.either
    lazy val _: IO[String, Int] = effEitherErrorOrResult.absolve


    type User = String
    type UserId = Int

    sealed trait NotificationError

    case object NotificationByEmailFailed extends NotificationError

    case object NotificationBySMSFailed extends NotificationError

    def getUserById(userId: UserId): Task[User] = ???

    def sendEmail(user: User, email: String): IO[NotificationByEmailFailed.type, Unit] = ???

    def sendSMS(user: User, phone: String): IO[NotificationBySMSFailed.type, Unit] = ???

    def sendNotification(userId: UserId): IO[NotificationError, Unit] = {
      for {
        user <- getUserById(userId).orDie
        _ <- sendEmail(user, "email")
        _ <- sendSMS(user, "88000")
      } yield ()
    }

    val z1 = ZIO.fail(NotificationByEmailFailed)
    val z2 = ZIO.fail(NotificationBySMSFailed)

    val app = z1.zipPar(z2).tapCause {
      case Both(c1, c2) =>
        zio.console.putStrLn(c1.failureOption.toString) *> zio.console.putStrLn(c2.failureOption.toString)
    }.orElse(zio.console.putStrLn("app is failed"))

  }

}
