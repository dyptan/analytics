package com.dyptan

import io.circe.generic.auto._
import sttp.client3.basicRequest
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import zio.akka.cluster.pubsub.PubSub
import zio.cache.{Cache, Lookup}
import zio.kafka.producer.Producer
import zio.kafka.serde.{Serde, Serializer}
import zio.stream.ZStream
import zio.{Duration, Queue, Schedule, ZIO, ZIOAppDefault}
import com.ria.avro.scala.{Advertisement, SearchRoot}
class Service extends ZIOAppDefault {

  import Conf._
  private val advertisementSerializer: Serializer[Any, Advertisement] = new AdvertisementSerializer
  override def run: ZIO[Any, Throwable, Unit] = {
    {
      for {
        pubSubOfIds <- PubSub.createPubSub[String]
        pubSubOfAds <- PubSub.createPubSub[AdWithId]
        cache <- Cache.make(
          capacity = 100000,
          timeToLive = Duration.fromSeconds(3600),
          lookup = Lookup(fetchAdToQ(_, pubSubOfAds))
        )
        idsQueue <- pubSubOfIds.listen("ids")
        adsQueue <- pubSubOfAds.listen("ads")
        // Takes records from remote API and pushes to internal queue
        loop = for {
          _ <- ZIO.logInfo("searching with request:" + searchRequest.toString)
          response <- send(searchRequest)
          root <- ZIO.fromEither(response.body).tapError { e => ZIO.logError("search failed: " + e) }
          pageCount <- ZIO.succeed(root.result.search_result.count / 100)
          // iterates over paged records and publishes to local Q
          _ <- recProduceIds(pageCount, pubSubOfIds)
        } yield ()
        _ <- loop
          .repeat(Schedule.spaced(Duration.fromSeconds(searchIntervalSec)))
          .retry(Schedule.exponential(Duration.fromSeconds(5)))
          .debug.fork
        // checks local cache for processed records
        loop = for {
          id <- idsQueue.take
          _ <- ZIO.logInfo(s"fetching id: $id")
          _ <- cache.get(id)
          hits <- cache.cacheStats.map(_.hits)
          misses <- cache.cacheStats.map(_.misses)
          _ <- ZIO.log(s"Number of cache hits: $hits")
          _ <- ZIO.log(s"Number of cache misses: $misses")
        } yield ()
        _ <- loop.forever.fork
        _ <- producerKafka(adsQueue).runDrain
      } yield ()

    }.provide(
      HttpClientZioBackend.layer(),
      kafkaProducerLayer,
      actorSystem)
  }.catchAll(t => ZIO.logError(s"caught failure $t"))

  /* Fetches a record from API for a given ID and pushes it to internal Q */
  private def fetchAdToQ(id: String, pubSub: PubSub[AdWithId]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      response <- send(basicRequest.get(infoBase.addParam("auto_id", id)).response(asJson[Advertisement]))
//      _ <- ZIO.log("Got Ad: " + response.body)
      adv <- ZIO.fromEither(response.body).debug.tapError { e => ZIO.logError("AD fetch failed: " + e) }
      adWithId = AdWithId(Integer.valueOf(id), adv)
      _ <- pubSub.publish("ads", adWithId)
    } yield ()
  }

  /* Returns a list of IDs for a given API page */
  private def pageNext(pageNum: Int): ZIO[SttpClient, Throwable, Seq[String]] = {
    val searchWithPage = searchWithParameters.addParam("page", s"$pageNum")
    val req = basicRequest.get(searchWithPage).response(asJson[SearchRoot])
    for {
      response <- send(req)
      root <- ZIO.fromEither(response.body).tapError { e => ZIO.logError("Paging failed: " + e) }
      ids <- ZIO.succeed(root.result.search_result.ids)
    } yield ids
  }

  /* Publishes list of IDs to local Q */
  private def recProduceIds(pageNum: Int, destQ: PubSub[String]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      _ <- ZIO.ifZIO(ZIO.succeed(pageNum >= 0))(
        for {
          _ <- ZIO.logInfo(s"pages num: $pageNum")
          ids <- pageNext(pageNum)
          _ <- ZIO.foreachDiscard(ids)(id => destQ.publish("ids", id))
          _ <- recProduceIds(pageNum - 1, destQ)
        } yield (),
        ZIO.succeed())

    } yield ()
  }

  /*Serialises and sends Avro messages to Kafka*/
  private def producerKafka(records: Queue[AdWithId]): ZStream[Producer, Throwable, Nothing] =
    ZStream.fromQueue(records)
      .tap(a => ZIO.log("records for send to kafka: " + a))
      .mapZIO { adWithId =>

        Producer.produce[Any, Int, Advertisement](
          topic = topicName,
          key = adWithId.id,
          value = adWithId.advertisement,
          keySerializer = Serde.int,
          valueSerializer = advertisementSerializer
        )

      }
      .tapError(error => ZIO.logError(s"Error in stream: $error"))
      .tap(a => ZIO.log("records sent, current offset " + a.offset()))
      .drain


}

