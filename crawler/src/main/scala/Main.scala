package com.dyptan.crawler

import io.circe.generic.auto._
import io.circe.parser.decode
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import sttp.client3.{basicRequest, _}
import zio.akka.cluster.pubsub.PubSub
import zio.cache.{Cache, Lookup}
import zio.http.{Request, Response, _}
import zio.{Duration, Schedule, ZIO, ZIOApp, ZIOAppDefault}

object Main extends ZIOAppDefault {
  import Conf._
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

        // Takes records from remote API and pushes to internal queue
        loop = for {
          _ <- ZIO.logInfo("searching with request:" + searchRequest.toString)
          response <- send(searchRequest)
          root <- ZIO.fromEither(response.body)
          pageCount <- ZIO.succeed(root.result.search_result.count / 100 )
          // iterates over paged records and publishes to local Q
          _ <- recProduce(pageCount, pubSub)
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
          _ <- ZIO.logDebug(s"Number of cache hits: $hits")
          _ <- ZIO.logDebug(s"Number of cache misses: $misses")
        } yield ()
        _ <- loop.forever.fork

        _ <- producerKafka(adsQueue).runDrain

      } yield ()

    }.provide(
      HttpClientZioBackend.layer(),
      kafkaProducerLayer,
      actorSystem)
  }.catchAll(t => ZIO.log(s"caught failure $t"))

  /* Fetches a record from API for a given ID and pushes it to internal Q
  * */
  def fetchAndPublish(id: String, pubSub: PubSub[String]) = {
    import io.circe.syntax._
    for {
      response <- send(basicRequest.get(infoBase.addParam("auto_id", id)).response(asJson[advRoot]))
      jsn <- ZIO.fromEither(response.body)
      adv <- ZIO.succeed(jsn.asJson.toString())
      _ <- pubSub.publish("ads", adv)
    } yield ()
  }

  /* Returns a list of IDs for a given API page
  * */
  private def pageNext(pageNum: Int): ZIO[SttpClient, Throwable, Array[String]] = {
    val searchWithPage = searchWithParameters.addParam("page", s"$pageNum")
    val req = basicRequest.get(searchWithPage).response(asJson[searchRoot])
    for {
      response <- send(req)
      root <- ZIO.fromEither(response.body)
      ids <- ZIO.succeed(root.result.search_result.ids)
    } yield ids
  }

  /* Publishes list of IDs to local Q
  */
  def recProduce(pageNum: Int, destQ: PubSub[String]): ZIO[SttpClient, Throwable, Unit] = {
    for {
      _ <- ZIO.ifZIO(ZIO.succeed(pageNum >= 0))(
        for {
          _ <- ZIO.logDebug(s"pages num: $pageNum")
          ids <- pageNext(pageNum)
          // TODO remove 1 record limit for every page
          _ <- ZIO.foreachDiscard(ids.take(1))(id => destQ.publish("ids", id))
          _ <- recProduce(pageNum - 1, destQ)
        } yield (),
        ZIO.succeed())

    } yield ()
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
        _ <- ZIO.logDebug(s"Http server started on port: $port")
      } yield ()
    }.provide(Server.defaultWithPort(port))
  }

}
object Combined extends ZIOApp.Proxy(Main <> HttpServer)