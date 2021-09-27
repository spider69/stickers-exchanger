package org.yusupov.server

import org.yusupov.config.ConfigService
import org.yusupov.config.ConfigService.Configuration
import org.yusupov.database.repositories.auth.SessionsRepository.SessionsRepository
import org.yusupov.database.repositories.auth.UsersRepository.UsersRepository
import org.yusupov.database.repositories.auth.{SessionsRepository, UsersRepository}
import org.yusupov.database.repositories.collections.CollectionsRepository.CollectionsRepository
import org.yusupov.database.repositories.collections.UserCollectionsRepository.UserCollectionsRepository
import org.yusupov.database.repositories.collections.{CollectionsRepository, UserCollectionsRepository}
import org.yusupov.database.repositories.stickers.StickersRepository.StickersRepository
import org.yusupov.database.repositories.stickers.UserStickersRepository.UserStickersRepository
import org.yusupov.database.repositories.stickers.{StickersRepository, UserStickersRepository}
import org.yusupov.database.services.DataGenerationService.DataGenerationService
import org.yusupov.database.services.MigrationService.{Liqui, MigrationService}
import org.yusupov.database.services.TransactorService.DBTransactor
import org.yusupov.database.services.{DataGenerationService, MigrationService, TransactorService}
import org.yusupov.logging.LoggerService
import org.yusupov.logging.LoggerService.LoggerService
import org.yusupov.services.auth.UsersService
import org.yusupov.services.auth.UsersService.UsersService
import org.yusupov.services.collections.CollectionsService.CollectionsService
import org.yusupov.services.collections.UserCollectionsService.UserCollectionsService
import org.yusupov.services.collections.{CollectionsService, UserCollectionsService}
import org.yusupov.services.stickers.StickersService.StickersService
import org.yusupov.services.stickers.UserStickersService.UserStickersService
import org.yusupov.services.stickers.{StickersService, UserStickersService}
import zio.{Has, ZLayer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.random.Random

trait Environment {

  type AppEnvironment =
    StickersService with StickersRepository with
      CollectionsService with CollectionsRepository with
      UsersService with UsersRepository with SessionsRepository with
      UserCollectionsService with UserCollectionsRepository with
      UserStickersService with UserStickersRepository with
      Configuration with Clock with Blocking with Random with Console with
      Liqui with MigrationService with DBTransactor with DataGenerationService with LoggerService

  val appEnvironment =
    LoggerService.live >+> ConfigService.live >+> Blocking.live >+> Clock.live >+>
      TransactorService.live >+>
      CollectionsRepository.live >+> CollectionsService.live >+>
      MigrationService.liquibaseLayer >+> MigrationService.live >+>
      StickersRepository.live >+> StickersService.live >+>
      UserCollectionsRepository.live >+> UserStickersRepository.live >+>
      UserCollectionsService.live >+> UserStickersService.live >+>
      UsersRepository.live >+> SessionsRepository.live >+> UsersService.live >+> DataGenerationService.live

}
