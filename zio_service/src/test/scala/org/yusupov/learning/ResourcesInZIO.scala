package org.yusupov.learning

import zio.console.{Console, putStrLn}
import zio.{IO, UIO, URIO, ZIO}

import java.io.{Closeable, IOException}
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ResourcesInZIO {

  object Traditional {
    trait Resource

    lazy val acquireResource: Resource = ???

    def use(resource: Resource): Unit = ???

    def releaseResource(resource: Resource): Unit = ???

    lazy val result1 = {
      val resource = acquireResource
      try {
        use(resource)
      } finally {
        releaseResource(resource)
      }
    }

    def withResource[R <: Closeable](resource: R)(use: R => Any) =
      try {
        use(resource)
      } finally {
        resource.close()
      }
  }

  object FutureResources {

    import Traditional.Resource

    implicit val global = scala.concurrent.ExecutionContext.global

    lazy val acquireFutureResource: Future[Resource] = ???

    def use(resource: Resource): Future[Unit] = ???

    def releaseResource(resource: Resource): Future[Unit] = ???

    lazy val result2 = {
      for {
        resource <- acquireFutureResource
        result <- use(resource).recoverWith(_ => releaseResource(resource))
        _ <- releaseResource(resource)
      } yield result
    }

    def withResource[T](useResource: Future[T])(release: Resource => Future[T]): Resource => Future[T] = {
      resource =>
        useResource.transformWith {
          case Failure(exception) =>
            release(resource).flatMap(_ => Future.failed(exception))
          case Success(value) =>
            release(resource).flatMap(_ => Future.successful(value))
        }
    }

    def ensuring[T](acquire: Future[Resource])(useResource: Resource => Future[T])(release: Resource => Future[Any]): Future[T] =
      acquire.flatMap { resource =>
        useResource(resource).transformWith {
          case Failure(exception) =>
            release(resource).flatMap(_ => Future.failed(exception))
          case Success(value) =>
            release(resource).flatMap(_ => Future.successful(value))
        }
      }

    ensuring(acquireFutureResource)(use)(releaseResource)
    acquireFutureResource.flatMap(r => withResource(use(r))(r => releaseResource(r))(r))
  }

  object ZioBracket {

    trait File {
      def name: String

      def close(): Unit = println(s"File -$name- closed")

      def readLines: List[String] = List("Hello world", "Scala is cool")
    }

    object File {
      def apply(_name: String): File = new File {
        override def name = _name
      }

      def apply(_name: String, lines: List[String]): File = new File {
        override def name = _name

        override def readLines = lines
      }
    }

    def openFile(fileName: String): IO[IOException, File] =
      ZIO.fromEither(Right(File(fileName)))

    def openFile(fileName: String, lines: List[String]): IO[IOException, File] =
      ZIO.fromEither(Right(File(fileName, lines)))

    def closeFile(file: File): UIO[Unit] = UIO(file.close())

//    def handleFile(file: File): ZIO[Console, Nothing, List[Unit]] =
//      ZIO.foreach(file.readLines)(line => putStrLn(line).orElseFail(ZIO.succeed()))
//
//    val twoFiles: ZIO[Console, IOException, List[Unit]] =
//      withFile("test1") { f1 =>
//        withFile("test2") { f2 =>
//          handleFile(f1) *> handleFile(f2)
//        }
//      }

    def withFile[R, A](fileName: String)(use: File => ZIO[R, IOException, A]) =
      openFile(fileName).bracket(closeFile)(use)

  }

  object ToyZManaged {

    final case class ZManaged[-R, +E, A](
      acquire: ZIO[R, E, A],
      release: A => URIO[R, Any]
    ) {
      self =>


    }

  }

}
