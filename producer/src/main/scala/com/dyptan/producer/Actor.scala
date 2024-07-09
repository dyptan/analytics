package com.dyptan.producer

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import CrawlerCommands._

class CrawlerController extends Actor with ActorLogging {
  import context.dispatcher
  implicit val system: ActorSystem = ActorSystem("root")
  implicit val defaultTimeout: Timeout = Timeout(3000.seconds)

  override def receive: Receive = startable()

  def startable(): Receive = {
    case StartProcessing =>
      log.info("Starting...")
      val crawler = new Service
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
