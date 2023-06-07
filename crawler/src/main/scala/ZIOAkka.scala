package com.dyptan.crawler
import zio.{Random, Schedule, ZIOAppDefault}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.stream.ZStream
object ZIOAkka extends ZIOAppDefault {

  import com.typesafe.config.ConfigFactory
  import akka.actor.ActorSystem
  import zio.{ZIO, ZLayer}
  import zio.akka.cluster.pubsub.PubSub

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

  override def run = (for {
    pubSub <- PubSub.createPubSub[String]
    queue <- pubSub.listen("my-topic")
    _ <- pubSub.publish("my-topic", "yo")
    firstMsg <- queue.take
    _ <- zio.Console.print(firstMsg)

  } yield firstMsg).provideLayer(actorSystem)
}
