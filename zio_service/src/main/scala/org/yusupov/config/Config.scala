package org.yusupov.config

case class ApiConfig(
  host: String,
  port: Int,
  cookieDomain: Option[String]
)

case class LiquibaseConfig(
  changeLog: String
)

case class DatabaseConfig(
  driver: String,
  url: String,
  user: String,
  password: String,
  herokuUrl: Option[String]
)

case class Config(
  api: ApiConfig,
  liquibase: LiquibaseConfig,
  database: DatabaseConfig,
  migrate: Boolean
)
