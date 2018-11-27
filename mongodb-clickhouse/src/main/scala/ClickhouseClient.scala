
import org.slf4j.LoggerFactory
import utilities.{Config, LoggerMagnet}

import scala.concurrent.{ExecutionContext, Future}


class ClickhouseClient(implicit executionContext: ExecutionContext, config: Config) extends ApiClient {

  val user = config.clickhouse.user
  val password = config.clickhouse.password
  val url = config.clickhouse.host

  val log:LoggerMagnet = LoggerFactory.getLogger(this.getClass)

  def insertRecords(records: Seq[String]):Future[Int] = {
    log.debug(s"inserting records number of records: ${records.length}")
    val statement = records.mkString(s"INSERT INTO default.events FORMAT JSONEachRow\n", "\n", "")
    log.trace(statement)
    postString("/",statement, Map("user"->user))
      .map(_ => records.length)
  }

  def createUserLogTable:Future[String] = {
    val tableCreateStatement: String = s"CREATE TABLE IF NOT EXISTS default.events " +
      s"(timestamp DateTime, userId String, eventType String)"+
      s"ENGINE = MergeTree() " +
      s"PARTITION BY toYYYYMM(timestamp) " +
      s"ORDER BY (eventType, userId, timestamp, cityHash64(userId)) " +
      s"SAMPLE BY cityHash64(userId);"
    log.debug(s"Creating replicated table : $tableCreateStatement")
    postString("/", tableCreateStatement, Map("user"->user))
  }

  def createAggregatingView:Future[String] = {
    val tableCreateStatement: String = """CREATE MATERIALIZED VIEW default.events_agg
      ENGINE = AggregatingMergeTree()
      PARTITION BY toYYYYMM(date)
      ORDER BY (eventType, date)
      AS SELECT
      eventType,
      toDate(timestamp) as date,
      countState() AS action,
      uniqState(userId) AS user
      FROM default.events
      GROUP BY eventType, date;"""
    log.debug(s"Creating replicated table : $tableCreateStatement")
    postString("/", tableCreateStatement, Map("user"->user))
  }
}
