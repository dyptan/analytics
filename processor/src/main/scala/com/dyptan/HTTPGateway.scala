package com.dyptan

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.dyptan.Commands.{Init, StartProcessing}
import org.slf4j.LoggerFactory

import java.io.IOException
import java.nio.file.{Files, Paths}
import java.util.Properties
import scala.concurrent.duration._

object Commands {
  case object Init
  case class StartProcessing(name: String)
}

class ProcessorActor extends Actor with ActorLogging {

  var processor: Processor = null
  override def receive: Receive = initProcessor()
  def initProcessor(): Receive = {
    case Init =>
      log.info(s"Initiating Processor...")
      processor = new Processor()
      sender() ! "Connected to topic witn N messages"
      context.become(readyToProcess())
  }

  def readyToProcess(): Receive = {
    case StartProcessing(name) =>
      log.info("Starting processing...")
      try {
        processor.consume(new Array[String](1))
      } catch {
        case e: Throwable => e.toString()
      }

      log.info(s"New model name is: " + name)

      sender() ! "true"
      context.unbecome()
  }

}

//trait TrainRequestJsonProtocol extends DefaultJsonProtocol {
//  implicit val requestFormat = jsonFormat[Commands.StartProcessing]
//}

object HTTPGateway {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("root")
    import system.dispatcher
    implicit val defaultTimeout = Timeout(3000.seconds)
    val log = LoggerFactory.getLogger(getClass.getName)

    val trainerActor = system.actorOf(Props[ProcessorActor], "ProcessorActor")

    val processorServerRoute =
      path("api" / "control")(
        parameters(Symbol("command").as[String]) { (command: String) =>
          post {
            command match {
              case "init" =>
                val resp = (trainerActor ? Init)
                  .mapTo[String]

                val entityFuture = resp.map { responseOption =>
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    responseOption
                  )
                }
                complete(entityFuture)

              case "start" =>
                val trainerResponseFuture = (trainerActor ? StartProcessing("name"))
                  .mapTo[String]
                val entityFuture = trainerResponseFuture.map { responseOption =>
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    responseOption
                  )
                }
                complete(entityFuture)
            }
          }
        })

    Http().newServerAt("0.0.0.0", 8081).bindFlow(processorServerRoute)
    log.info("Gateway started to listen on port " + 8081)

  }
}
