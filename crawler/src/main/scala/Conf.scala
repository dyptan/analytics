package com.dyptan.crawler

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.confluent.kafka.serializers.{AbstractKafkaSchemaSerDeConfig, KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.header.Headers
import sttp.client3.circe._
import sttp.client3.{UriContext, basicRequest}
import sttp.model
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.{Deserializer, Serializer}
import zio.{Task, ZIO, ZLayer}

import java.io.File
import scala.jdk.CollectionConverters.MapHasAsJava

object Conf {
  val config = ConfigFactory.parseFile(new File("application.conf"))
  val authKey = model.QueryParams().param("api_key", config.getConfig("crawler").getString("autoriaApiKey"))
//  val infoBase = uri"https://developers.ria.com/auto/info".addParams(authKey)
  val infoBase = uri"http://localhost:8080/info.json".addParams(authKey)
  val searchBase = uri"http://localhost:8080/search.json".addParams(authKey).addParam("countpage", "100")
//  val searchBase = uri"https://developers.ria.com/auto/search".addParams(authKey).addParam("countpage", "100")
  val searchDefault = searchBase.addParams("category_id" -> "1", "s_yers[0]" -> "2000", "price_ot" -> "3000",
    "price_do" -> "60000", "countpage" -> "100", "top" -> "1")
  var searchWithParameters = searchDefault

  def searchRequest = basicRequest
    .get(searchWithParameters)
    .response(asJson[searchRoot])

  case class L2(ids: Array[String], count: Int)

  case class L1(search_result: L2)

  case class searchRoot(result: L1)

  case class Geo(stateId: Int, cityId: Int, regionNameEng: String)

  case class Dtls(year: Int, autoId: Int, bodyId: Int, raceInt: Int, fuelId: Int,
                     fuelNameEng: String, gearBoxId: Int, gearboxName: String, driveId: Int, driveName: String,
                     categoryId: Int, categoryNameEng: String, subCategoryNameEng: String)

  case class Ad(USD: Int, addDate: String, soldDate: String, autoData: Dtls,
                     markId: Int, markNameEng: String, modelId: Int, modelNameEng: String, linkToView: String,
                     stateData: Geo)
  case class AdWithId(id: Int, advertisement: Ad)

  val actorSystem: ZLayer[Any, Throwable, ActorSystem] =
    ZLayer
      .scoped(
        ZIO.acquireRelease(ZIO.attempt(ActorSystem("Test", config.getConfig("akkaConf"))))(sys => ZIO.fromFuture(_ => sys.terminate()).either)
      )





  def kafkaProducerLayer =

    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List(
          config.getConfig("producer").getString("kafkaServer")
          )
        )
          .withProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
          .withProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081")

    )
    )



}
