package com.dyptan.crawler

import com.dyptan.crawler.ZIOAkka.actorSystem
import io.circe.generic.auto._
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import zio.ZIOAppDefault
//import zio._
import io.circe._
import io.circe.parser._
import com.typesafe.config.ConfigFactory
import zio.{ZIO, ZLayer}
import zio.akka.cluster.pubsub.PubSub
import akka.actor.ActorSystem

object GetAndParseJsonZioCirce extends ZIOAppDefault {
  val url = uri"https://developers.ria.com/auto/search?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl&category_id=1&bodystyle%5B0%5D=3&bodystyle%5B4%5D=2&marka_id%5B0%5D=79&model_id%5B0%5D=0&s_yers%5B0%5D=2010&po_yers%5B0%5D=2017&marka_id%5B1%5D=84&model_id%5B1%5D=0&s_yers%5B1%5D=2012&po_yers%5B1%5D=2016&brandOrigin%5B0%5D=276&brandOrigin%5B1%5D=392&price_ot=1000&price_do=60000&currency=1&auctionPossible=1&state%5B0%5D=1&city%5B0%5D=0&state%5B1%5D=2&city%5B1%5D=0&state%5B2%5D=10&city%5B2%5D=0&abroad=2&custom=1&auto_options%5B477%5D=477&type%5B0%5D=1&type%5B1%5D=2&type%5B3%5D=4&type%5B7%5D=8&gearbox%5B0%5D=1&gearbox%5B1%5D=2&gearbox%5B2%5D=3&engineVolumeFrom=1.4&engineVolumeTo=3.2&powerFrom=90&powerTo=250&power_name=1&countpage=50&with_photo=1"
  case class L2(ids: Array[String], count: Int)
  case class L1(search_result: L2)
  case class L0(result: L1)


  val configString =
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
  override def run = {
    val request = basicRequest
      .get(url)
      .response(asJson[L0])

   for {
      response <- send(request)
      root <- ZIO.fromEither(response.body)
      ids <- ZIO.succeed(root.result.search_result.ids)
      _ <- process(ids)
   } yield ()

  }.provideLayer(HttpClientZioBackend.layer())

  def process(ids: Array[String]) = {
    for {
      pubSub <- PubSub.createPubSub[String]
      queue <- pubSub.listen("my-topic")
      _ <- ZIO.foreach(ids){
        id => pubSub.publish("my-topic", id)
      }

      loop = for {
        id <- queue.take
        _ <- zio.Console.printLine(s"worker: Starting analyzing task $id")
      } yield ()
      fiber <- loop.forever.fork

    } yield queue
  }.provideLayer(actorSystem)

}


