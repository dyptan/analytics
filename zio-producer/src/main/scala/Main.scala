package com.dyptan.producer

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.dyptan.producer.Conf._
import com.dyptan.producer.CrawlerCommands.{StartProcessing, StopProcessing}
import io.circe.parser.decode
import zio._
import zio.http._

import scala.concurrent.Await

object Main extends ZIOAppDefault {
  implicit val system: ActorSystem = ActorSystem("CrawlerGateway")
  // Use Scala duration explicitly
  implicit val defaultTimeout: Timeout = Timeout(scala.concurrent.duration.Duration(3000, scala.concurrent.duration.SECONDS))
  private val controller = system.actorOf(Props[CrawlerController], "CrawlerController")
  
  // Define HTTP handlers
  def searchHandler(req: Request): ZIO[Any, Throwable, Response] = 
    req.body.asString.map { bodyString =>
      val inputMap = decode[Map[String, String]](bodyString).getOrElse(Map.empty)
      searchWithParameters = searchBase.addParams(inputMap)
      Response.text(searchWithParameters.toString())
    }.orElse(ZIO.succeed(Response.status(Status.BadRequest)))
  
  def echoHandler(req: Request): ZIO[Any, Throwable, Response] =
    req.body.asString
      .map(Response.text)
      .orElse(ZIO.succeed(Response.status(Status.BadRequest)))
  
  def controlHandler(req: Request): ZIO[Any, Throwable, Response] =
    req.body.asURLEncodedForm.map { form =>
      val resp = form.get("command").flatMap(_.stringValue)
        .map {
          case "start" =>
            Await.result(controller ? StartProcessing, scala.concurrent.duration.Duration.Inf)
          case "stop" =>
            Await.result(controller ? StopProcessing, scala.concurrent.duration.Duration.Inf)
          case _ => Some("no such command")
        }
      Response.text(resp.getOrElse("no command").toString)
    }.orElse(ZIO.succeed(Response.status(Status.BadRequest)))
  
  // Simple HTTP app using Routes API
  val routes = Routes(
    Method.POST / "search" -> handler(request => searchHandler(request)),
    Method.POST / "echo" -> handler(request => echoHandler(request)),
    Method.POST / "control" -> handler(request => controlHandler(request))
  )
  
  override def run: ZIO[Any, Throwable, Unit] = {
    // Create server config with port
    val config = Server.Config.default.port(httpServerPort)
    
    // Start server with error handling
    Server.serve(routes.handleError(error => {
      Console.printLine("Error occurred: " + error.getMessage).orDie
      Response.status(Status.InternalServerError)
    })).provide(
      ZLayer.succeed(config),
      Server.live
    )
  }
}