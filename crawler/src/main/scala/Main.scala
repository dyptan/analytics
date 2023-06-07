package com.dyptan.crawler

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import org.apache.kafka.common.Uuid
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import zio.akka.cluster.pubsub.PubSub
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Console, Queue, ZIO, ZIOAppDefault, ZLayer}

object Main extends ZIOAppDefault {
  private val url = uri"https://developers.ria.com/auto/search?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl&category_id=1&bodystyle%5B0%5D=3&bodystyle%5B4%5D=2&marka_id%5B0%5D=79&model_id%5B0%5D=0&s_yers%5B0%5D=2010&po_yers%5B0%5D=2017&marka_id%5B1%5D=84&model_id%5B1%5D=0&s_yers%5B1%5D=2012&po_yers%5B1%5D=2016&brandOrigin%5B0%5D=276&brandOrigin%5B1%5D=392&price_ot=1000&price_do=60000&currency=1&auctionPossible=1&state%5B0%5D=1&city%5B0%5D=0&state%5B1%5D=2&city%5B1%5D=0&state%5B2%5D=10&city%5B2%5D=0&abroad=2&custom=1&auto_options%5B477%5D=477&type%5B0%5D=1&type%5B1%5D=2&type%5B3%5D=4&type%5B7%5D=8&gearbox%5B0%5D=1&gearbox%5B1%5D=2&gearbox%5B2%5D=3&engineVolumeFrom=1.4&engineVolumeTo=3.2&powerFrom=90&powerTo=250&power_name=1&countpage=50&with_photo=1"
  private val request = basicRequest
    .get(url)
    .response(asJson[searchRoot])
  case class L2(ids: Array[String], count: Int)
  case class L1(search_result: L2)
  case class searchRoot(result: L1)
  case class Geography(stateId: Int, cityId: Int)
  case class Details(year: Int, autoId: Int,
                     raceInt: Int, fuelId: Int, gearBoxId: Int, driveId: Int)
  case class advRoot(USD: Int, addDate: String, autoData: Details,
                    markId: Int, modelId: Int, stateData: Geography)

  val configString: String =
    """
      |akka {
      | actor {
      |   provider = akka.cluster.ClusterActorRefProvider
      |   }
      | cluster {
      |   min-nr-of-members=1
      |   }
      |}
      |""".stripMargin

  private val config = ConfigFactory.parseString(configString)
  val actorSystem: ZLayer[Any, Throwable, ActorSystem] =
    ZLayer
      .scoped(
        ZIO.acquireRelease(ZIO.attempt(ActorSystem("Test", config)))(sys => ZIO.fromFuture(_ => sys.terminate()).either)
      )
  private def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List("localhost:9092"))
      )
    )
  private def consumerLayer =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:9092")).withGroupId("group")
      )
    )

  private def process(ids: Array[String]) = {
    for {
      pubSub <- PubSub.createPubSub[String]
      idsQueue <- pubSub.listen("ids")
      adsQueue <- pubSub.listen("ads")
      _ <- ZIO.foreach(ids){ id => pubSub.publish("ids", id) }
      loop = for {
        id <- idsQueue.take
        _ <- ZIO.debug(s"fetching id: $id")
        ad <- fetch(id)
        _ <- pubSub.publish("ads",ad)
        _ <- ZIO.debug(s"published to pubsub: $ad")
      } yield ()
      _ <- loop.forever.fork
    } yield adsQueue
  }

  private def producer(records: Queue[String]): ZStream[Producer, Throwable, Nothing] =
    ZStream.fromQueue(records)
      .mapZIO { record =>
        Producer.produce[Any, Long, String](
          topic = "random",
          key = Uuid.randomUuid().hashCode(),
          value = record,
          keySerializer = Serde.long,
          valueSerializer = Serde.string
        )
      }
      .drain

  private val consumer: ZStream[Consumer, Throwable, Nothing] =
    Consumer
      .plainStream(Subscription.topics("random"), Serde.long, Serde.string)
      .tap(r => Console.printLine("Received on kafka: "+r.value))
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapZIO(_.commit)
      .drain
  private def fetch(id: String) = {
    val urlbase = uri"https://developers.ria.com/auto/info?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl"
    for {
      response <- send(basicRequest.get(urlbase.addParam("auto_id", id)).response(asJson[advRoot]))
      root <- ZIO.fromEither(response.body)
      data <- ZIO.succeed(root.toString)
      _ <- ZIO.debug(s"fetched from API: $data")
    } yield data
  }
  override def run: ZIO[Any, Throwable, Unit] = {
    for {
      response <- send(request)
      root <- ZIO.fromEither(response.body)
      ids <- ZIO.succeed(root.result.search_result.ids)
      adsQ <- process(ids.take(2))
      _ <- producer(adsQ).merge(consumer).runDrain
    } yield ()

  }.provide(HttpClientZioBackend.layer(),
    producerLayer,
    consumerLayer,
    actorSystem)
}