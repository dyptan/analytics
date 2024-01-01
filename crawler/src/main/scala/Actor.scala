package com.dyptan.crawler

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object CrawlerCommands {
  case object StartProcessing
  case object StopProcessing
}

class CrawlerController extends Actor with ActorLogging {
  implicit val system = ActorSystem("CrawlerGateway")
  implicit val defaultTimeout = Timeout(3000.seconds)

  import CrawlerCommands._
  import context.dispatcher

  override def receive: Receive = startable()


  def startable(): Receive = {
    case StartProcessing =>
      log.info("Starting...")
      val crawler = new Crawler
      Future(crawler.main(Array.empty))
      sender() ! "started"
      context.become(stoppable())
    case _ => sender() ! "Invalid state"
  }

  def stoppable(): Receive = {
    case StartProcessing => sender() ! "Already running.."
    case _ => sender() ! "Invalid state"
  }

}
