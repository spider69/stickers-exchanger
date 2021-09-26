package org.yusupov.config

import zio.config.ReadError
import zio.config.typesafe.TypesafeConfig
import zio.{Has, Layer}
import zio.config.magnolia.DeriveConfigDescriptor.descriptor

object ConfigService {

  type Configuration = Has[Config]

  lazy val live: Layer[ReadError[String], Configuration] = {
    val configDescriptor = descriptor[Config]
    TypesafeConfig.fromDefaultLoader(configDescriptor)
  }
}
