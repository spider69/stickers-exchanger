package org.yusupov.services.search

import doobie.Transactor
import org.yusupov.database.repositories.stickers.UserStickersRepository
import org.yusupov.database.repositories.stickers.UserStickersRepository.UserStickersRepository
import org.yusupov.database.services.TransactorService
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.structures.{StickerId, UserId}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, RLayer, Task, ZIO, ZLayer}
import zio.logging._

import java.util.UUID

@accessible
object SearchService {
  type SearchService = Has[Service]

  trait Service {
    def search(userId: String): RIO[DBTransactor with Logging, List[ExchangeInfo]]
  }

  case class ExchangeInfo(
    userId: UserId,
    stickersNeededToUser: List[StickerId],
    stickersNeededFromUser: List[StickerId]
  )

  class ServiceImpl(
    userStickersRepository: UserStickersRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def search(id: String): RIO[DBTransactor with Logging, List[ExchangeInfo]] = {
      def getNeededStickersForUsers(userIds: List[UserId], transactor: Transactor[Task]) =
        ZIO.foreach(userIds){ id =>
          ZIO.succeed(id) zip userStickersRepository.getNeededStickersForUsers(id).transact(transactor)
        }

      def concatLists(
        neededStickersToUserFromOthers: List[(UserId, List[StickerId])],
        neededStickersForEachUser: List[(UserId, List[StickerId])]
      ): List[ExchangeInfo] = {
        neededStickersToUserFromOthers.sortBy(_._1).zip(neededStickersForEachUser.sortBy(_._1)).flatMap {
          case ((leftId, fromStickers), (rightId, toStickers)) =>
            if (leftId == rightId)
              List(ExchangeInfo(leftId, fromStickers, toStickers))
            else
              List(
                ExchangeInfo(leftId, fromStickers, List.empty),
                ExchangeInfo(rightId, List.empty, toStickers)
              )
        }
      }

      for {
        transactor <- TransactorService.databaseTransactor
        userId <- ZIO.effect(UUID.fromString(id))
        userStickers <- userStickersRepository.getStickersWithCount(userId, 0).transact(transactor)
        neededStickersToUser <- userStickersRepository.getNeededStickersForUsers(userId).transact(transactor)
        othersWithNeededStickers <- userStickersRepository.getUsersByStickers(neededStickersToUser.map(_.id)).transact(transactor)
        stickersNeededToEachOtherUser <- getNeededStickersForUsers(othersWithNeededStickers.map(_._1.id).distinct, transactor)
        neededStickersForEachUser = stickersNeededToEachOtherUser.map {
          case (id, stickers) => (id, stickers.filter(s => userStickers.map(_.stickerId).contains(s.id)).map(_.id))
        }
        neededStickersToUserFromOthers = othersWithNeededStickers
          .map(_._2)
          .groupBy(_.userId)
          .map {
            case (id, stickers) => (id, stickers.map(_.stickerId))
          }.toList
      } yield concatLists(neededStickersToUserFromOthers, neededStickersForEachUser)
    }
  }

  lazy val live: RLayer[UserStickersRepository, SearchService] =
    ZLayer.fromService[UserStickersRepository.Service, SearchService.Service] {
      userStickersRepo => new ServiceImpl(userStickersRepo)
    }

}
