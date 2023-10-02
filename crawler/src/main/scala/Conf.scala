package com.dyptan.crawler

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import org.apache.kafka.common.Uuid
import sttp.client3.circe._
import sttp.client3.{UriContext, basicRequest}
import sttp.model
import zio.kafka._
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Queue, ZIO, ZLayer}

import com.google.cloud.pubsublite.kafka.{ ProducerSettings => GProducerSettings }
import com.google.cloud.pubsublite.{ CloudZone, ProjectNumber, TopicName, TopicPath }

import java.io.File

object Conf {
  val config = ConfigFactory.parseFile(new File("application.conf"))
  val authKey = model.QueryParams().param("api_key", config.getConfig("source").getString("autoriaApiKey"))
  val infoBase = uri"https://developers.ria.com/auto/info".addParams(authKey)
  val searchBase = uri"https://developers.ria.com/auto/search".addParams(authKey).addParam("countpage", "100")
  val searchDefault = searchBase.addParams("category_id" -> "1", "s_yers[0]" -> "2000", "price_ot" -> "3000",
    "price_do" -> "60000", "countpage" -> "100", "top" -> "1")
  var searchWithParameters = searchDefault
  def searchRequest = basicRequest
    .get(searchWithParameters)
    .response(asJson[searchRoot])
  case class L2(ids: Array[String], count: Int)
  case class L1(search_result: L2)
  case class searchRoot(result: L1)
  case class Geography(stateId: Int, cityId: Int, regionNameEng: String)
  case class Details(year: Int, autoId: Int, bodyId: Int, raceInt: Int, fuelId: Int,
                     fuelNameEng: String, gearBoxId: Int, gearboxName: String, driveId: Int, driveName: String,
                     categoryId: Int, categoryNameEng: String, subCategoryNameEng: String)
  case class advRoot(USD: Int, addDate: String, soldDate: String, autoData: Details,
                     markId: Int, markNameEng: String, modelId: Int, modelNameEng: String, linkToView: String,
                     stateData: Geography)

  val actorSystem: ZLayer[Any, Throwable, ActorSystem] =
    ZLayer
      .scoped(
        ZIO.acquireRelease(ZIO.attempt(ActorSystem("Test", config.getConfig("akkaConf"))))(sys => ZIO.fromFuture(_ => sys.terminate()).either)
      )

  def pubSubLiteProducerLayer =
    ZLayer.scoped(
      pubsublite.producer.Producer.make(
        settings = pubsublite.producer.ProducerSettings(
          config.getConfig("producer").getString("pubSubLiteRegion"),
          config.getConfig("producer").getLong("pubSubLiteProjectNumber"),
          config.getConfig("producer").getString("topicName")
        )
      )
    )

  def kafkaProducerLayer =
    ZLayer.scoped(
      zio.kafka.producer.Producer.make(
        settings = zio.kafka.producer.ProducerSettings(List(
          config.getConfig("producer").getString("topicName")
          )
        )
      )
    )
//  def consumerLayer =
//    ZLayer.scoped(
//      Consumer.make(
//        ConsumerSettings(List("localhost:9092")).withGroupId("group")
//      )
//    )

  val kafkaTopic: String = {
    TopicPath.newBuilder
      .setLocation(CloudZone.parse(config.getConfig("producer").getString("pubSubLiteRegion")))
      .setProject(ProjectNumber.of(config.getConfig("producer").getLong("pubSubLiteProjectNumber")))
      .setName(TopicName.of(config.getConfig("producer").getString("topicName"))).build.toString()
  }

  def producerPubSubLite(records: Queue[String]) =
    ZStream.fromQueue(records)
      .mapZIO { record =>
        pubsublite.producer.Producer.produce[Any, Long, String](
          topic = kafkaTopic,
          key = Uuid.randomUuid().hashCode(),
          value = record,
          keySerializer = Serde.long,
          valueSerializer = Serde.string
        )
      }
      .drain

  def producerKafka(records: Queue[String]): ZStream[zio.kafka.producer.Producer, Throwable, Nothing] =
    ZStream.fromQueue(records)
      .mapZIO { record =>
        zio.kafka.producer.Producer.produce[Any, Long, String](
          topic = kafkaTopic,
          key = Uuid.randomUuid().hashCode(),
          value = record,
          keySerializer = Serde.long,
          valueSerializer = Serde.string
        )
      }
      .drain

  def producer(records: Queue[String]) = {
    if (config.getConfig("producer").getBoolean("useOubSubLite")) {
      producerPubSubLite(records)
    } else {
      producerKafka(records)
    }
  }
//  val consumer: ZStream[Consumer, Throwable, Nothing] =
//    Consumer
//      .plainStream(Subscription.topics("random"), Serde.long, Serde.string)
//      .map(_.offset)
//      .aggregateAsync(Consumer.offsetBatches)
//      .mapZIO(_.commit)
//      .drain
}
