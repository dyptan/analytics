package com.dyptan.producer

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.dyptan.producer.Conf._
import com.dyptan.producer.CrawlerCommands.{StartProcessing, StopProcessing}
import io.circe.parser.decode
import zio.http._
import zio.{ZIO, ZIOAppDefault}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main extends ZIOAppDefault {
  implicit val system: ActorSystem = ActorSystem("CrawlerGateway")
  implicit val defaultTimeout: Timeout = Timeout(3000.seconds)
  private val controller = system.actorOf(Props[CrawlerController], "CrawlerController")
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

    case req@Method.POST -> Root / "control" =>
      req.body.asURLEncodedForm.mapBoth(_ => Response.status(Status.BadRequest), {
        input =>
          val resp: Option[Any] = input.get("command").map(_.stringValue)
            .map {
              case Some("start") =>
                Await.result(controller ? StartProcessing, scala.concurrent.duration.Duration.Inf)
              case Some("stop") =>
                Await.result(controller ? StopProcessing, scala.concurrent.duration.Duration.Inf)
              case _ => Some("no such command")
            }
        Response.text(resp.get.toString)
      })
  }

  override def run: ZIO[Any, Throwable, Unit] = {
    {
      for {
        _ <- Server.serve(http)
      } yield ()
    }.provide(Server.defaultWithPort(httpServerPort))
  }

}

//object Combined extends ZIOApp.Proxy(HttpServer)