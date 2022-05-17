package com.dyptan

import java.io.{IOException, InputStream}
import java.util.{Optional, Properties}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import java.nio.file.{Files, Paths}
import scala.concurrent.duration._

object TrainerActor {
  case class TrainRequest(trainingDataSetPath: String, limit: Int, iterations: Int)
  case class SaveRequest(name: String)
}

class TrainerActor extends Actor with ActorLogging {

  import TrainerActor._

  val trainer = new Trainer

  override def receive: Receive = readyToTrain()

  def readyToTrain(): Receive = {
    case TrainRequest(path, limit, iterations) =>
      log.info(s"Train request received with path: $path, limit: $limit,iterations: $iterations")

      trainer.setSource(new java.net.URL(path), limit, iterations)
      trainer.train()

      log.info(s"Training completed")

      sender() ! trainer.evaluate()
      context.become(readyToApply())
  }

  def readyToApply(): Receive = {
    case SaveRequest(name) =>
      log.info("Applying model...")
      try {
        trainer.save(name)
      } catch {
        case e: Throwable => e.toString()
      }

      log.info(s"New model name is: " + name)

      sender() ! "true"
      context.unbecome()
  }

}

class TrainerGateway {
  import org.slf4j.LoggerFactory
  val log = LoggerFactory.getLogger(classOf[TrainerGateway])

  val trainerConfig = new Properties()
  val configFile: String = Option(System.getenv("CONF_FILE")).getOrElse("conf/trainer.properties")
  log.info("Loading config file from " + configFile)

  try trainerConfig.load(Files.newInputStream(Paths.get(configFile)))
  catch {
    case e: IOException =>
      e.printStackTrace()
  }

    implicit val system = ActorSystem("TrainerGateway")
    implicit val materializer = ActorMaterializer()
    import TrainerActor._
    import system.dispatcher
    implicit val defaultTimeout = Timeout(3000 seconds)

    val trainerActor = system.actorOf(Props[TrainerActor], "TRainerActor")

    log.info("Starting actorSystem "+this.getClass().getPackage().getImplementationVersion())

    val trainerServerRoute =
      path("api" / "train") {
        parameters(
          'path.as[String],
          'iterations.as[Int],
          'limit.as[Int])
          { (path: String, iterations: Int, limit: Int) =>
          post {
            val trainerResponseFuture = (trainerActor ? TrainRequest(path, limit, iterations))
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
      } ~
      path("api" / "save") {
        parameters('name.as[String])
        { (name: String) =>
          post {
            val trainerResponseFuture = (trainerActor ? SaveRequest(name))
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
      }

    val port = trainerConfig.getOrDefault("gateway.port", "8081").asInstanceOf[String].toInt

    Http().bindAndHandle(trainerServerRoute, "0.0.0.0", port)
    log.info("Trainer Gateway (AKKA HTTP) started to listen on port " + port)

}

trait TrainRequestJsonProtocol extends DefaultJsonProtocol {
  import TrainerActor._
  implicit val requestFormat = jsonFormat3(TrainRequest)
}

object TrainerLauncher {
  def main(args: Array[String]): Unit = {
   val trainer = new TrainerGateway
  }
}
