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
import sttp.model._
import zio.akka.cluster.pubsub.PubSub
import zio.http.{Method, Request, Response, _}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{Console, Duration, Queue, Schedule, ZIO, ZIOApp, ZIOAppDefault, ZLayer}

object Main extends ZIOAppDefault {
  val authKey = model.QueryParams().param("api_key","KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl")
  val infoBase = uri"https://developers.ria.com/auto/info".addParams(authKey)
  val searchBase = uri"https://developers.ria.com/auto/search".addParams(authKey).addParam("countpage","100")
//  val searchDefault = uri"https://developers.ria.com/auto/search?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl&s_yers[0]=2010&po_yers[0]=2011&category_id=1"
  val searchDefault = uri"https://developers.ria.com/auto/search?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl&s_yers[0]=2010&po_yers[0]=2011&category_id=1&price_ot=30000&price_do=60000&countpage=100"
//  category_id=1&bodystyle[0]=3&bodystyle[4]=2&marka_id[0]=79&model_id[0]=0&s_yers[0]=2010&po_yers[0]=2017&marka_id[1]=84&model_id[1]=0&s_yers[1]=2012&po_yers[1]=2016&brandOrigin[0]=276&brandOrigin[1]=392&price_ot=1000&price_do=60000&currency=1&auctionPossible=1&state[0]=1&city[0]=0&state[1]=2&city[1]=0&state[2]=10&city[2]=0&abroad=2&custom=1&auto_options[477]=477&type[0]=1&type[1]=2&type[3]=4&type[7]=8&gearbox[0]=1&gearbox[1]=2&gearbox[2]=3&engineVolumeFrom=1.4&engineVolumeTo=3.2&powerFrom=90&powerTo=250&power_name=1&countpage=50&with_photo=1"
  var searchWithParameters = searchDefault

  private def searchRequest = basicRequest
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

  private val akkaConfig = ConfigFactory.parseString(configString)
  val actorSystem: ZLayer[Any, Throwable, ActorSystem] =
    ZLayer
      .scoped(
        ZIO.acquireRelease(ZIO.attempt(ActorSystem("Test", akkaConfig)))(sys => ZIO.fromFuture(_ => sys.terminate()).either)
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

  private def fetchAds(id: String) = {
    for {
      response <- send(basicRequest.get(infoBase.addParam("auto_id", id)).response(asJson[advRoot]))
      jsn <- ZIO.fromEither(response.body)
      adv <- ZIO.succeed(jsn.toString)
    } yield adv
  }
  def pageNext(id: Int) = {
    val searchWithPage = searchWithParameters.addParam("page", s"$id")
    val req = basicRequest.get(searchWithPage).response(asJson[searchRoot])
    for {
      response <- send(req)
      root <- ZIO.fromEither(response.body)
      ids <- ZIO.succeed(root.result.search_result.ids)
    } yield (ids)
  }

  def recProduce(pageNum: Int, pubSub: PubSub[String]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      _ <- ZIO.ifZIO(ZIO.succeed(pageNum>=0))(
        for {
        _ <- ZIO.debug(s"pages num: $pageNum")
        ids <- pageNext(pageNum)
        _ <- ZIO.foreach(ids.take(1)) { id => pubSub.publish("ids", id) }
        _ <- recProduce(pageNum - 1, pubSub)
      } yield (),
        ZIO.succeed())

    } yield()
  }
  override def run: ZIO[Any, Throwable, Unit] = {
    {
      for {
        pubSub <- PubSub.createPubSub[String]
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
          ad <- fetchAds(id)
          _ <- pubSub.publish("ads", ad)
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
      req.body.asString.map { input =>
        val inputMap: Map[String, String] = decode[Map[String, String]](input).getOrElse(Map.empty)
        searchWithParameters = searchBase.addParams(inputMap)
        Response.text(searchWithParameters.toString())
      }.mapError(_ => Response.status(Status.BadRequest))

    case req@Method.POST -> Root / "echo" =>
      req.body.asString.map(Response.text(_)).mapError(_ => Response.status(Status.BadRequest))
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