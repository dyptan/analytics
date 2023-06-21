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
  import Conf._
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