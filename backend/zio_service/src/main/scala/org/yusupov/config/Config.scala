package org.yusupov.config

case class ApiConfig(
  host: String,
  port: Int,
  cookieDomain: String
)

case class LiquibaseConfig(
  changeLog: String
)

case class DatabaseConfig(
  driver: String,
  url: String,
  user: String,
  password: String
)

case class Config(
  api: ApiConfig,
  liquibase: LiquibaseConfig,
  database: DatabaseConfig,
  migrate: Boolean
)
