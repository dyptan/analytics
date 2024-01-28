package com.dyptan

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.confluent.kafka.serializers.{AbstractKafkaSchemaSerDeConfig, KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.header.Headers
import sttp.client3.circe._
import sttp.client3.{UriContext, basicRequest}
import sttp.model
import sttp.model.Uri
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.{Deserializer, Serializer}
import zio.{Task, ZIO, ZLayer}

import java.io.File
import scala.jdk.CollectionConverters.MapHasAsJava

object Conf {
  val config = ConfigFactory.load()
  val authKey = model.QueryParams().param("api_key", config.getConfig("crawler").getString("autoriaApiKey"))
  val infoBase = uri"https://developers.ria.com/auto/info".addParams(authKey)
//  val infoBase = uri"http://localhost:8090/info.json".addParams(authKey)
//  val searchBase = uri"http://localhost:8090/search.json".addParams(authKey).addParam("countpage", "100")
  val searchBase = uri"https://developers.ria.com/auto/search".addParams(authKey).addParam("countpage", "100")
  val searchDefault = searchBase.addParams("category_id" -> "1", "s_yers[0]" -> "2000", "price_ot" -> "3000",
    "price_do" -> "60000", "countpage" -> "100", "top" -> "1")
  var searchWithParameters: Uri = searchDefault
  val topicName: String = config.getConfig("producer").getString("kafkaTopic")
  val registryUrl = "http://schema-registry:8081"
  val searchIntervalSec = 5
  val httpServerPort = 8083

  def searchRequest = basicRequest
    .get(searchWithParameters)
    .response(asJson[searchRoot])


  val actorSystem: ZLayer[Any, Throwable, ActorSystem] =
    ZLayer
      .scoped(
        ZIO.acquireRelease(
          ZIO.attempt(ActorSystem("Test", config.getConfig("akkaConf"))))(sys => ZIO.fromFuture(_ => sys.terminate()).either)
      )

  def kafkaProducerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List(config.getConfig("producer").getString("kafkaServer")))
          .withProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
          .withProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.getConfig("producer").getString("schema-registry.url"))
    )
    )



}
