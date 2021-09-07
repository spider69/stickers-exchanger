package org.yusupov.learning

import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.internal.Executor
import zio.{UIO, URIO, ZIO}

import java.io.IOException
import java.util.concurrent.TimeUnit
import zio.duration.durationInt

object ConcurrencyInZIO {

  val currentTime: URIO[Clock, Long] = zio.clock.currentTime(TimeUnit.SECONDS)

  def printEffectRunningTime[R, E, A](zio: ZIO[R, E, A]): ZIO[Console with Clock with R, Any, A] =
    for {
      startTime <- currentTime
      result <- zio
      finishTime <- currentTime
      totalTime = finishTime - startTime
      _ <- putStrLn(totalTime.toString)
    } yield result

  val sleep1Second = ZIO.sleep(1.second)
  val sleep3Seconds = ZIO.sleep(3.seconds)

  lazy val getExchangeRatesLocation1 = sleep3Seconds *> putStrLn("GetExchangeRatesLocation1")
  lazy val getExchangeRatesLocation2 = sleep1Second *> putStrLn("GetExchangeRatesLocation2")
  lazy val getFrom2Locations: ZIO[Console with Clock, IOException, (Unit, Unit)] = getExchangeRatesLocation1 zip getExchangeRatesLocation2

  lazy val getFrom2LocationsInParallel =
    for {
      fiber1 <- getExchangeRatesLocation1.fork
      fiber2 <- getExchangeRatesLocation2.fork
      r1 <- fiber1.join
      r2 <- fiber2.join
    } yield (r1, r2)

  lazy val writeUserToDB = sleep1Second *> putStrLn("User in DB")
  lazy val sendEmail = sleep1Second *> putStrLn("Mail sent")

  lazy val writeAndSend = writeUserToDB zipPar sendEmail

  lazy val greeter = for {
    _ <- putStrLn("Hello").forever.fork
  } yield ()

  lazy val greeter2 = ZIO.effectTotal(while (true) println("Hello"))
  val greeterApp = for {
    _ <- greeter2.fork
  } yield ()

  val app3 = for {
    fiber <- getExchangeRatesLocation1.fork
    _ <- getExchangeRatesLocation2
    _ <- fiber.interrupt
    _ <- ZIO.sleep(3.seconds)
  } yield ()

  val _ = getExchangeRatesLocation1 zipPar getExchangeRatesLocation2
  val _ = ZIO.foreachPar(List("1", "2", "3"))(str => putStrLn(str))
  val _ = getExchangeRatesLocation1 race getExchangeRatesLocation2

  lazy val doSomething: UIO[Unit] = ???
  lazy val doSomethingElse: UIO[Unit] = ???

  lazy val executor: Executor = ???

  val eff = for {
    f <- doSomething.fork
    _ <- doSomethingElse
    _ <- f.join
  } yield ()

  val result = eff.lock(executor)

  lazy val executor1: Executor = ???
  lazy val executor2: Executor = ???

  val eff2 = for {
    f <- doSomething.lock(executor2).fork
    _ <- doSomethingElse
    _ <- f.join
  } yield ()

  val result2 = eff2.lock(executor1)
}
