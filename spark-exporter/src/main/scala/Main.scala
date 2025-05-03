package com.dyptan.exporter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  implicit val logger: Logger = LoggerFactory.getLogger("export-logger")

  implicit def actorSystem: ActorSystem = ActorSystem()

  logger.info("Export service is starting up")

  val conf = ConfigFactory.load()

  val service = new ExportService
  val controller = new ExportController(service)
  Await.result(
    Http().newServerAt("0.0.0.0", 8082).bind(Directives.concat(controller.route)),
    Duration.Inf
  )
}
