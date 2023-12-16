package com.dyptan.crawler

import com.dyptan.avro.{Advertisement, AutoData, Geography}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecord}
import org.apache.kafka.common.Uuid
import org.apache.kafka.common.header.Headers
import sttp.client3.basicRequest
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import zio.akka.cluster.pubsub.PubSub
import zio.cache.{Cache, Lookup}
import zio.http._
import zio.kafka.producer.Producer
import zio.kafka.serde
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Duration, Queue, Schedule, Task, ZIO, ZIOApp, ZIOAppDefault}

import java.io.ByteArrayOutputStream
import java.util

object Main extends ZIOAppDefault {

  import Conf._

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
          .repeat(Schedule.spaced(Duration.fromSeconds(config.getConfig("crawler").getInt("searchIntervalSec"))))
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

  /* Fetches a record from API for a given ID and pushes it to internal Q
  * */
  def fetchAdToQ(id: String, pubSub: PubSub[AdWithId]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      response <- send(basicRequest.get(infoBase.addParam("auto_id", id)).response(asJson[Ad]))
      _ <- ZIO.log("Got Ad: " + response.body)
      adv <- ZIO.fromEither(response.body).tapError { e => ZIO.logError("AD fetch failed: " + e) }
      adWithId = AdWithId(Integer.valueOf(id), adv)
      _ <- pubSub.publish("ads", adWithId)
    } yield ()
  }

  /* Returns a list of IDs for a given API page
  * */
  private def pageNext(pageNum: Int): ZIO[SttpClient, Throwable, Array[String]] = {
    val searchWithPage = searchWithParameters.addParam("page", s"$pageNum")
    val req = basicRequest.get(searchWithPage).response(asJson[searchRoot])
    for {
      response <- send(req)
      root <- ZIO.fromEither(response.body).tapError { e => ZIO.logError("Paging failed: " + e) }
      ids <- ZIO.succeed(root.result.search_result.ids)
    } yield ids
  }

  /* Publishes list of IDs to local Q
  */
  def recProduceIds(pageNum: Int, destQ: PubSub[String]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      _ <- ZIO.ifZIO(ZIO.succeed(pageNum >= 0))(
        for {
          _ <- ZIO.logInfo(s"pages num: $pageNum")
          ids <- pageNext(pageNum)
          // TODO remove 1 record limit for every page
          _ <- ZIO.foreachDiscard(ids.take(1))(id => destQ.publish("ids", id))
          _ <- recProduceIds(pageNum - 1, destQ)
        } yield (),
        ZIO.succeed())

    } yield ()
  }

  def producerKafka(records: Queue[AdWithId]): ZStream[Producer, Throwable, Nothing] =
    ZStream.fromQueue(records)
      .tap(a => ZIO.log("records for send to kafka: " + a))
      .mapZIO { adWithId =>

        val record = adWithId.advertisement
        val autoData = AutoData.newBuilder()
          .setYear(record.autoData.year)
          .setAutoId(record.autoData.autoId)
          .setBodyId(record.autoData.bodyId)
          .setRaceInt(record.autoData.raceInt)
          .setFuelId(record.autoData.fuelId)
          .setFuelNameEng(record.autoData.fuelNameEng)
          .setGearBoxId(record.autoData.gearBoxId)
          .setGearboxName(record.autoData.gearboxName)
          .setDriveId(record.autoData.driveId)
          .setDriveName(record.autoData.driveName)
          .setCategoryId(record.autoData.categoryId)
          .setCategoryNameEng(record.autoData.categoryNameEng)
          .setSubCategoryNameEng(record.autoData.subCategoryNameEng)
          .build();

        val geography = Geography.newBuilder()
          .setStateId(record.stateData.stateId)
          .setCityId(record.stateData.cityId)
          .setRegionNameEng(record.stateData.regionNameEng)
          .build();

        val advertisement = Advertisement.newBuilder()
          .setUSD(record.USD)
          .setAddDate(record.addDate)
          .setSoldDate(record.soldDate)
          .setAutoData(autoData)
          .setMarkId(record.markId)
          .setMarkNameEng(record.markNameEng)
          .setModelId(record.modelId)
          .setModelNameEng(record.modelNameEng)
          .setLinkToView(record.linkToView)
          .setStateData(geography)
          .build();

        Producer.produce[Any, Int, Advertisement](
          topic = config.getConfig("producer").getString("kafkaTopic"),
          key = adWithId.id,
          value = advertisement,
          keySerializer = Serde.int,
          valueSerializer = new AdvertisementSerializer
        )

      }
      .tapError(error => ZIO.logError(s"Error in stream: $error"))
      .tap(a => ZIO.log("records sent offset" + a.offset()))
      .drain
}

object AvroConverter {
  def convertToGenericRecord(record: SpecificRecord): GenericRecord = {
    val writer = new SpecificDatumWriter[SpecificRecord](record.getSchema)
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(out, null)
    writer.write(record, encoder)
    encoder.flush()
    out.close()

    val decoder = DecoderFactory.get.binaryDecoder(out.toByteArray, null)
    val reader = new SpecificDatumReader[GenericRecord](record.getSchema)
    reader.read(null, decoder)
  }
}

class AdvertisementSerializer extends serde.Serializer[Any, Advertisement] {
  val avroSerializer = new KafkaAvroSerializer()
  val props = new util.HashMap[String, String]
  props.put("schema.registry.url","http://localhost:8081")
  avroSerializer.configure(props, false)
  override def serialize(topic: String, headers: Headers, data: Advertisement): Task[Array[Byte]] = {
    val genericRecord = AvroConverter.convertToGenericRecord(data)

    ZIO.attempt(avroSerializer.serialize(topic, headers, genericRecord))
  }
}

object HttpServer extends ZIOAppDefault {

  import Conf._

  val port = config.getConfig("httpConf").getInt("serverPort")
  val http: Http[Any, Response, Request, Response] = Http.collectZIO[Request] {
    case req@Method.POST -> Root / "search" =>
      req.body.asString.mapBoth(_ => Response.status(Status.BadRequest), {
        input =>
          val inputMap: Map[String, String] = decode[Map[String, String]](input).getOrElse(Map.empty)
          searchWithParameters = searchBase.addParams(inputMap)
          Response.text(searchWithParameters.toString())
      })
    case req@Method.POST -> Root / "echo" =>
      req.body.asString.mapBoth(_ => Response.status(Status.BadRequest), Response.text(_))
  }

  override def run: ZIO[Any, Throwable, Unit] = {
    {
      for {
        _ <- Server.serve(http).fork
        _ <- ZIO.log(s"Http server started on port: $port")
      } yield ()
    }.provide(Server.defaultWithPort(port))
  }

}

object Combined extends ZIOApp.Proxy(Main <> HttpServer)