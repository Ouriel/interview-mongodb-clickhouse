package utilities

import scala.concurrent.duration.FiniteDuration

case class Config(mongo: MongoDBConfig,
                  clickhouse: ClickHouseConfig,
                  concurrence: Int,
                  shutdownTimeout: FiniteDuration
                 )

case class MongoDBConfig(url: String,
                         database: String,
                         replicasSet: String
                        )

case class ClickHouseConfig(host: String,
                            user: String,
                            password: String,
                            batchSize: Int,
                            batchTime: FiniteDuration
                           )
