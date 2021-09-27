package org.yusupov.database.services

import org.yusupov.database.dao
import org.yusupov.database.dao.auth.UserDao
import org.yusupov.database.dao.collections.{CollectionDao, UserCollectionDao}
import org.yusupov.database.dao.stickers.{StickerDao, UserStickerDao}
import org.yusupov.database.dao.{auth, collections}
import org.yusupov.database.repositories.auth.UsersRepository
import org.yusupov.database.repositories.auth.UsersRepository.UsersRepository
import org.yusupov.database.repositories.collections.CollectionsRepository.CollectionsRepository
import org.yusupov.database.repositories.collections.UserCollectionsRepository.UserCollectionsRepository
import org.yusupov.database.repositories.collections.{CollectionsRepository, UserCollectionsRepository}
import org.yusupov.database.repositories.stickers.StickersRepository.StickersRepository
import org.yusupov.database.repositories.stickers.UserStickersRepository.UserStickersRepository
import org.yusupov.database.repositories.stickers.{StickersRepository, UserStickersRepository}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.utils.SecurityUtils
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, RLayer, ZLayer}

import java.util.UUID

@accessible
object DataGenerationService {

  type DataGenerationService = Has[Service]

  trait Service {
    def initUsers: RIO[DBTransactor with Random, Unit]
    def initCollections: RIO[DBTransactor with Random, Unit]
    def initUsersCollections: RIO[DBTransactor with Random, Unit]
  }

  class ServiceImpl(
    usersRepository: UsersRepository.Service,
    collectionsRepository: CollectionsRepository.Service,
    stickersRepository: StickersRepository.Service,
    userCollectionsRepository: UserCollectionsRepository.Service,
    userStickersRepository: UserStickersRepository.Service
  ) extends Service {
    import doobie.implicits._

    val collectionsInsertions = Map(
      "spider-man" -> CollectionDao(UUID.randomUUID(), "Spider-man", 8, "Spider-man cartoon stickers collection", "spider-man.jpg"),
      "star_wars" -> collections.CollectionDao(UUID.randomUUID(), "Star wars. Episode I. Phantom menace.", 4, "Star wars episode I collection", "star-wars-ep1.jpg"),
      "avengers" -> collections.CollectionDao(UUID.randomUUID(), "Avengers: Infinity war", 6, "Stickers collection for famous movie", "avengers-iw.jpg"),
      "lotr" -> collections.CollectionDao(UUID.randomUUID(), "The Lord of the Rings. The return of the king", 5, "Epic saga", "lotr-king.jpg"),
      "turtles" -> collections.CollectionDao(UUID.randomUUID(), "Teenage mutant ninja turtles", 7, "Cartoon from 80s", "turtles.jpg")
    )

    def generateStickers(name: String) =
      (1 to collectionsInsertions(name).numberOfStickers).map { i =>
        StickerDao(UUID.randomUUID(), collectionsInsertions(name).id, s"$i", s"Sticker with number $i", s"$name-$i.jpg")
      }.toList

    val userNames = List(
      "Alice", "Bob", "Jack", "Rachel", "Ross", "Chandler", "Joey", "Monica", "Phoebe", "Oleg"
    )

    def generateUsers(): List[UserDao] =
      userNames.map { name =>
        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.countSecretHash(name, salt)
        auth.UserDao(UUID.randomUUID(), name, s"$name@mail.ru", passwordHash, salt)
      }

    val stickersInsertions = collectionsInsertions.keys.flatMap(name => generateStickers(name)).toList
    val users = generateUsers()

    def generateUsersCollections(): List[(UserCollectionDao, List[UserStickerDao])] = {
      val random = new util.Random()
      users.map(_.id).flatMap { userId =>
        val shuffled = random.shuffle(collectionsInsertions.values)
        val n = random.nextInt(shuffled.size - 1) + 1
        shuffled.take(n).map { c =>
          val userCollection = collections.UserCollectionDao(userId, c.id)
          val stickers = stickersInsertions.filter(_.collectionId == c.id)
          val shuffledStickers = random.shuffle(stickers)
          val n = random.nextInt(shuffledStickers.size)
          val userStickers = shuffledStickers.take(n).map(s => dao.stickers.UserStickerDao(userId, s.collectionId, s.id, random.nextInt(3)))
          (userCollection, userStickers)
        }
      }
    }
    val generatedUserCollectionsAndStickers = generateUsersCollections()
    val usersCollections = generatedUserCollectionsAndStickers.map(_._1)
    val usersStickers = generatedUserCollectionsAndStickers.flatMap(_._2)

    override def initUsers =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.insert(users).transact(transactor)
      } yield ()

    override def initCollections =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- collectionsRepository.insert(collectionsInsertions.values.toList).transact(transactor)
        _ <- stickersRepository.insert(stickersInsertions).transact(transactor)
      } yield ()

    override def initUsersCollections =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- userCollectionsRepository.addCollections(usersCollections).transact(transactor)
        _ <- userStickersRepository.addStickers(usersStickers).transact(transactor)
      } yield ()
  }

  lazy val live: RLayer[UsersRepository with CollectionsRepository with StickersRepository with UserCollectionsRepository with UserStickersRepository, DataGenerationService] =
    ZLayer.fromServices[UsersRepository.Service, CollectionsRepository.Service, StickersRepository.Service, UserCollectionsRepository.Service, UserStickersRepository.Service, DataGenerationService.Service](
      (usersRepo, collectionsRepo, stickersRepo, usersCollectionsRepo, userStickersRepo) =>
        new ServiceImpl(usersRepo, collectionsRepo, stickersRepo, usersCollectionsRepo, userStickersRepo)
    )

}
