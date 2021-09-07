package org.yusupov.learning

import zio.clock.Clock
import zio.console.Console
import zio.duration.durationInt
import zio.{IO, Task, UIO, URIO, ZIO}

import scala.concurrent.{Future, blocking}
import scala.io.StdIn
import scala.util.Try

object PracticeZIO {

  object Guide {
    object TypeAliases {
      type Task[A] = ZIO[Any, Throwable, A]
      type IO[E, A] = ZIO[Any, E, A]
      type RIO[R, A] = ZIO[R, Throwable, A]
      type URIO[R, A] = ZIO[R, Nothing, A]
      type UIO[A] = ZIO[Any, Nothing, A]
    }

    object RealZIO {
      //      val _: UIO[Int] = ZIO.succeed(7)
      //      val _: Task[Unit] = ZIO.effect(println("Hello"))
      //      val _: UIO[Unit] = ZIO.effectTotal(println(""))
      //
      //      val f: Future[Int] = ???
      //      val _: Task[Int] = ZIO.fromFuture(ec => f)
      //
      //      val t: Try[String] = ???
      //      val _: Task[String] = ZIO.fromTry(t)
      //
      //      val e: Either[String, Int] = ???
      //      val _: IO[String, Int] = ZIO.fromEither(e)
      //
      //      val opt: Option[Int] = ???
      //      val z: IO[Option[Nothing], Int] = ZIO.fromOption(opt)
      //      val zz: UIO[Option[Int]] = z.option
      //      val _: IO[Option[Nothing], Int] = zz.some

      val _: URIO[String, Unit] = ZIO.fromFunction[String, Unit](str => println(str))

      val _: UIO[Unit] = ZIO.unit
      val _: UIO[Option[Nothing]] = ZIO.none
      val _: UIO[Nothing] = ZIO.never // while(true)
      val _: UIO[Nothing] = ZIO.die(new Throwable("Died"))
      val _: IO[Int, Nothing] = ZIO.fail(7)

      lazy val readLine: Task[String] = ZIO.effect(StdIn.readLine())

      def writeLine(str: String): Task[Unit] = ZIO.effectTotal(println(str))

      lazy val lineToInt: ZIO[Any, Throwable, Int] = readLine.flatMap(str => ZIO.effect(str.toInt))

      lazy val echo = readLine.flatMap(str => writeLine(str))

      lazy val greetAndEcho: ZIO[Any, Throwable, (Unit, Unit)] = writeLine("Greetings!").zip(echo)

      //      lazy val a1: Task[Int] = ???
      //      lazy val b1: Task[String] = ???
      //
      //      val _: ZIO[Any, Throwable, (Int, String)] = a1.zip(b1)
      //      val _: ZIO[Any, Throwable, String] = a1 zipRight b1 // *>
      //      val _: ZIO[Any, Throwable, Int] = a1 zipLeft b1 // <*

      lazy val _: ZIO[Any, Throwable, Unit] = writeLine("Greetings!") *> echo

      val r1: ZIO[Any, Throwable, Int] = (lineToInt zip lineToInt) map { case (l, r) => l + r }
      val r2: ZIO[Any, Throwable, Int] =
        for {
          n1 <- lineToInt
          n2 <- lineToInt
        } yield n1 + n2

      lazy val r3 = r1.flatMap(n => writeLine(n.toString))

      //lazy val ab4: ZIO[Any, Throwable, String] = b1.zipWith(b1)(_ + _)
      lazy val c: ZIO[Clock, Throwable, Int] = ZIO.sleep(5.seconds).as(7)

      lazy val readInt: ZIO[Console, Throwable, Int] =
        ZIO.effect(StdIn.readLine()).flatMap(str => ZIO.effect(str.toInt))

      lazy val readIntAndRetry: ZIO[Console, Throwable, Int] = readInt.orElse {
        ZIO.effectTotal(println("Retry input:")) *> readIntAndRetry
      }

      def factorial(n: Int): Int = {
        if (n <= 1) n
        else n * factorial(n - 1)
      }

      def factorialZ(n: BigDecimal): Task[BigDecimal] = {
        if (n <= 1)
          ZIO.succeed(n)
        else
          ZIO.succeed(n).zipWith(factorialZ(n - 1))(_ * _)
      }

      lazy val guessProgram = {
        lazy val readInt: ZIO[Console, Throwable, Int] = zio.console.getStrLn.flatMap(str => ZIO.effect(str.toInt))
        lazy val readIntAndRetry: ZIO[Console, Throwable, Int] = readInt.orElse(
          zio.console.putStrLn("Ошибка. Повторите ввод.") *> readIntAndRetry
        )

        def guessNumber(guessedNum: Int): ZIO[Console, Throwable, Unit] =
          readIntAndRetry
            .flatMap {
              case num if num == guessedNum =>
                zio.console.putStrLn("Число угадано!")
              case _ =>
                zio.console.putStrLn("Неверно. Введите число ещё раз.") *> guessNumber(guessedNum)
            }

        for {
          num <- zio.random.nextIntBetween(1, 4)
          _ <- zio.console.putStrLn("Угадайте число от 1 до 3! Введите число:")
          _ <- guessNumber(num)
        } yield ()
      }

      def doWhile[R, E, A](body: ZIO[R, E, A])(condition: A => Boolean): ZIO[R, E, A] =
        body.flatMap { value =>
          if (condition(value)) ZIO.succeed(value) else doWhile(body)(condition)
        }

      lazy val printNumbers = doWhile(zio.random.nextIntBetween(1, 11).map {
        i =>
          println(i)
          i
      })(_ == 10)

    }

    val s1 = ZIO.succeed(42)
    val s2: Task[Int] = Task.succeed(42)

    val now = ZIO.effectTotal(System.currentTimeMillis())

    val f1 = ZIO.fail("Uh oh!")
    val f2 = Task.fail(new Exception("Uh oh!"))

    val zoption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(2))
    val zoption2: IO[String, Int] = zoption.mapError(_ => "It wasn't there!")

    case class User(teamId: String)

    case class Team()

    val maybeId: IO[Option[Nothing], String] = ZIO.fromOption(Some("abc123"))

    def getUser(userId: String): IO[Throwable, Option[User]] = ???

    def getTeam(teamId: String): IO[Throwable, Team] = ???

    val result: IO[Throwable, Option[(User, Team)]] = (
      for {
        id <- maybeId
        user <- getUser(id).some
        team <- getTeam(user.teamId).asSomeError
      } yield (user, team)
      ).optional

    val zeither = ZIO.fromEither(Right("Success!"))
    val ztry = ZIO.fromTry(Try(42 / 0))
    val zfun: URIO[Int, Int] = ZIO.fromFunction((i: Int) => i * i)

    lazy val future = Future.successful("Hello!")
    val zfuture: Task[String] = ZIO.fromFuture { implicit ec =>
      future.map(_ => "Goodbye!")
    }

    // side-effects
    val getStrLn: Task[String] = ZIO.effect(StdIn.readLine())

    def putStrLn(line: String): UIO[Unit] = ZIO.effectTotal(println(line))

    //val getStrLn2: IO[IOException, String] = ZIO.effect(StdIn.readLine()).refineOrDie[IOException]

    case class AuthError()

    object legacy {
      def login(onSuccess: User => Unit, onFailure: AuthError => Unit): Unit = ???
    }

    val login: IO[AuthError, User] =
      IO.effectAsync[AuthError, User] { callback =>
        legacy.login(
          user => callback(IO.succeed(user)),
          err => callback(IO.fail(err))
        )
      }

    //val sleeping = zio.blocking.effectBlocking(Thread.sleep(Long.MaxValue))

    //def accept(socket: ServerSocket) =
    // zio.blocking.effectBlockingCancelable(socket.accept())(UIO.effectTotal(socket.close()))

    import scala.io.{Codec, Source}

    def download(url: String) =
      Task.effect {
        Source.fromURL(url)(Codec.UTF8).mkString
      }

    def safeDownload(url: String) = blocking(download(url))
  }

  //Otus.echo.run(())

}
