package com.dyptan.crawler

import Conf._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import zio.ZIO
import zio.test.{ZIOSpecDefault, _}
object Tests extends ZIOSpecDefault{

  def spec =
    suite("ParseJsonSpec")(
      test("field matches expected value") {
        {
          for {
            adv <- ZIO.readFile("src/test/resources/search.json")
            decoded <- ZIO.fromEither(parser.decode[advRoot](adv))
            _ <- ZIO.debug(s"got value $decoded")
            json <- ZIO.succeed(decoded.asJson)
            _ <- ZIO.debug(s"$json")
          } yield assertTrue(decoded.autoData.year == 2013)
        }
      }
    )
}
