
import akka.actor.ActorSystem
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, KillSwitches, Supervision}
import com.mongodb.client.model.changestream.FullDocument
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import org.mongodb.scala.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Aggregates, Filters}
import org.slf4j.LoggerFactory
import pureconfig.loadConfigOrThrow
import utilities.{Config, LoggerMagnet}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main {

  def main(args: Array[String]): Unit = {

    val log:LoggerMagnet = LoggerFactory.getLogger(this.getClass)

    implicit val config = loadConfigOrThrow[Config]("mongodb-clickhouse")
    implicit val system = ActorSystem.create("mongodb-clickhouse")
    implicit val mat = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy { e =>
      log.error(s"Exception Received", e)
      Supervision.Resume
    })

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val formats = DefaultFormats ++ org.json4s.ext.JavaTimeSerializers.all


    val clickhouseClient = new ClickhouseClient

    Await.result(clickhouseClient.createUserLogTable, Duration.Inf)

    val usersCodecRegistry = fromRegistries(fromProviders(classOf[User]), DEFAULT_CODEC_REGISTRY)
    val logsCodecRegistry = fromRegistries(fromProviders(classOf[Logs]), DEFAULT_CODEC_REGISTRY)

    val mongoClient = MongoClient(config.mongo.url)
    val db: MongoDatabase = mongoClient.getDatabase(config.mongo.database)
    val usersColl = db.getCollection("Users").withCodecRegistry(usersCodecRegistry)
    val logsColl =  db.getCollection("Logs").withCodecRegistry(logsCodecRegistry)

    val pipeline: Bson = Aggregates.`match`(Filters.in("operationType", "insert", "update"))


    log.info("Starting akka stream mongodb to clickhouse")
    val (killSwitchUser, futureUser) =
      MongoSource(usersColl.watch(Seq(pipeline)).fullDocument(FullDocument.UPDATE_LOOKUP))
      .map(userChg => userChg.getFullDocument)
      .map(user => UserEvent(user.getDate("updated_at"), user.getString("_id"), "sign-up"))
      .map(event => write(event))
      .groupedWithin (config.clickhouse.batchSize, config.clickhouse.batchTime)
      .mapAsync(config.concurrence) { records =>
        clickhouseClient.insertRecords(records)
      }
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.ignore)(Keep.both)
        .run()

    val (killSwitchLogs, futureLogs) =
      MongoSource(logsColl.watch(Seq(pipeline)).fullDocument(FullDocument.UPDATE_LOOKUP))
        .map(userChg => userChg.getFullDocument)
        .map(log => UserEvent(log.getDate("date"), log.getString("userId"), log.getString("event")))
        .map(event => write(event))
        .groupedWithin (config.clickhouse.batchSize, config.clickhouse.batchTime)
        .mapAsync(config.concurrence) { records =>
          clickhouseClient.insertRecords(records)
        }
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.ignore)(Keep.both)
        .run()


    sys.addShutdownHook({
      killSwitchUser.shutdown()
      killSwitchLogs.shutdown()
    })

    log.info("akka stream started, to shutdown send ctrl+c or call /shutdown")
    val result = Await.result(Future.sequence(Seq(futureUser, futureLogs)), Duration.Inf)
    log.info(s"Finishing with value : $result")
    mat.shutdown()
    Await.result(system.terminate(), config.shutdownTimeout)
    log.info("Terminated... Bye")
    sys.exit()
  }
}
