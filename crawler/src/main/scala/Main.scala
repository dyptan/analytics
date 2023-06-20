package com.dyptan.crawler

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.parser.decode
import org.apache.kafka.common.Uuid
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import sttp.client3.{basicRequest, _}
import sttp.model
import sttp.model.Uri
import zio.akka.cluster.pubsub.PubSub
import zio.cache.{Cache, Lookup}
import zio.http.{Request, Response, _}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Console, Duration, Queue, Schedule, ZIO, ZIOApp, ZIOAppDefault, ZLayer}

object Main extends ZIOAppDefault {
  private val authKey = model.QueryParams().param("api_key","KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl")
  val infoBase = uri"https://developers.ria.com/auto/info".addParams(authKey)
  private val searchDefault = uri"https://developers.ria.com/auto/search?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl&s_yers[0]=2010&po_yers[0]=2011&category_id=1&price_ot=30000&price_do=60000&countpage=100"
  val searchBase: Uri = uri"https://developers.ria.com/auto/search".addParams(authKey).addParam("countpage","100")
  var searchWithParameters: Uri = searchDefault

  def searchRequest = basicRequest
    .get(searchWithParameters)
    .response(asJson[searchRoot])
  case class L2(ids: Array[String], count: Int)
  case class L1(search_result: L2)
  case class searchRoot(result: L1)
  case class Geography(stateId: Int, cityId: Int)
  case class Details(year: Int, autoId: Int,
                             raceInt: Int, fuelId: Int, gearBoxId: Int, driveId: Int)
  case class advRoot(USD: Int, addDate: String, autoData: Details,
                             markId: Int, modelId: Int, stateData: Geography)

  private val configString: String =
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

  private val akkaConfig = ConfigFactory.parseString(configString)
  val actorSystem: ZLayer[Any, Throwable, ActorSystem] =
    ZLayer
      .scoped(
        ZIO.acquireRelease(ZIO.attempt(ActorSystem("Test", akkaConfig)))(sys => ZIO.fromFuture(_ => sys.terminate()).either)
      )
  def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List("localhost:9092"))
      )
    )
  def consumerLayer =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:9092")).withGroupId("group")
      )
    )
  def producer(records: Queue[String]): ZStream[Producer, Throwable, Nothing] =
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
  val consumer: ZStream[Consumer, Throwable, Nothing] =
    Consumer
      .plainStream(Subscription.topics("random"), Serde.long, Serde.string)
      .tap(r => Console.printLine("Received on kafka: "+r.value))
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapZIO(_.commit)
      .drain

  def fetchAndPublish(id: String, pubSub: PubSub[String]) = {
    for {
      response <- send(basicRequest.get(infoBase.addParam("auto_id", id)).response(asJson[advRoot]))
      jsn <- ZIO.fromEither(response.body)
      adv <- ZIO.succeed(jsn.toString)
      _ <- pubSub.publish("ads", adv)
    } yield ()
  }
  private def pageNext(id: Int): ZIO[SttpClient, Throwable, Array[String]] = {
    val searchWithPage = searchWithParameters.addParam("page", s"$id")
    val req = basicRequest.get(searchWithPage).response(asJson[searchRoot])
    for {
      response <- send(req)
      root <- ZIO.fromEither(response.body)
      ids <- ZIO.succeed(root.result.search_result.ids)
    } yield ids
  }

  def recProduce(pageNum: Int, pubSub: PubSub[String]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      _ <- ZIO.ifZIO(ZIO.succeed(pageNum>=0))(
        for {
        _ <- ZIO.debug(s"pages num: $pageNum")
        ids <- pageNext(pageNum)
        _ <- ZIO.foreachDiscard(ids.take(1))(id => pubSub.publish("ids", id))
        _ <- recProduce(pageNum - 1, pubSub)
      } yield (),
        ZIO.succeed())

    } yield()
  }
  override def run: ZIO[Any, Throwable, Unit] = {
    {
      for {
        pubSub <- PubSub.createPubSub[String]
        cache <- Cache.make(
          capacity = 100000,
          timeToLive = Duration.fromSeconds(3600),
          lookup = Lookup(fetchAndPublish(_, pubSub))
        )
        idsQueue <- pubSub.listen("ids")
        adsQueue <- pubSub.listen("ads")

        loop = for {
          _ <- ZIO.debug("searching with request:" + searchRequest.toString)
          response <- send(searchRequest)
          root <- ZIO.fromEither(response.body)
          pageCount <- ZIO.succeed(root.result.search_result.count / 100 )
          _ <- recProduce(pageCount, pubSub)
        } yield ()
        _ <- loop.repeat(Schedule.spaced(Duration.fromSeconds(5))).fork

        loop = for {
          id <- idsQueue.take
          _ <- ZIO.debug(s"fetching id: $id")
          _ <- cache.get(id)
          hits <- cache.cacheStats.map(_.hits)
          misses <- cache.cacheStats.map(_.misses)
          _ <- ZIO.debug(s"Number of cache hits: $hits")
          _ <- ZIO.debug(s"Number of cache misses: $misses")
        } yield ()
        _ <- loop.forever.fork
        _ <- producer(adsQueue).merge(consumer).runDrain
      } yield ()

    }.provide(
      HttpClientZioBackend.layer(),
      producerLayer,
      consumerLayer,
      actorSystem)
  }
}
object HttpServer extends ZIOAppDefault {
  import Main._
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
        _ <- Server.serve(http).debug("http server started")
      } yield ()
    }.provide(Server.defaultWithPort(8092))
  }

}
object Combined extends ZIOApp.Proxy(Main <> HttpServer)