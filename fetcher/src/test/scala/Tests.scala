package com.dyptan

import Conf._
import com.dyptan.avro.Advertisement
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import zio.ZIO
import zio.test.{ZIOSpecDefault, _}
object Tests extends ZIOSpecDefault{

  def spec =
    suite("ParseJsonSpec")(

     /* test("field matches expected value") {
        {
          for {
            adv <- ZIO.readFile("src/test/resources/info.json")
            decoded <- ZIO.fromEither(parser.decode[Advertisement](adv))
            _ <- ZIO.debug(s"got value $decoded")
            json <- ZIO.succeed(decoded.asJson)
            _ <- ZIO.debug(s"$json")
          } yield assertTrue(decoded.autoData.year == 2013)
        }
      }*/

//        test("field matches expected value") {
//        {
//          import io.circe.parser.decode
//
//          val jsonString = """{"USD": 100, "addDate": "2023-01-01"}"""
//
//          val result: Either[io.circe.Error, Advertisement] = decode[Advertisement](jsonString)
//
//          result match {
//            case Right(advertisement) => println(s"Decoded Advertisement: $advertisement")
//            case Left(error) => println(s"Failed to decode JSON: $error")
//          }
//          for {
//            adv <- ZIO.readFile("src/test/resources/info.json")
//            decoded <- ZIO.fromEither(parser.decode[Advertisement](adv))
//            _ <- ZIO.debug(s"got value $decoded")
//            json <- ZIO.succeed(decoded.asJson)
//            _ <- ZIO.debug(s"$json")
//          } yield assertTrue(decoded.autoData.year == 2013)
//        }
//      }


    )
}
