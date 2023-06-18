package com.dyptan.crawler

import Main._

import sttp.client3.httpclient.zio._
import zio.akka.cluster.pubsub.PubSub
import zio.cache.{Cache, Lookup}
import zio.test.{ZIOSpecDefault, _}
import zio.{Duration, Schedule, ZIO}
object Tests extends ZIOSpecDefault{
  def spec =
    suite("HelloWorldSpec")(
      test("sayHello correctly displays output") {
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
              pageCount <- ZIO.succeed(root.result.search_result.count / 100)
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
          } yield assertTrue(Vector("Hello, World!\n") == Vector("Hello, World!\n"))

        }.provide(
          HttpClientZioBackend.layer(),
          producerLayer,
          consumerLayer,
          actorSystem)
      }
    )
}
