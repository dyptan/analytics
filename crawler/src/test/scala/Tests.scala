package com.dyptan.crawler

import Main._

import io.circe._
import io.circe.generic.auto._
import sttp.client3.httpclient.zio._
import sttp.client3.testing._
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
            _ <- ZIO.debug(s"got value ${decoded}")

          } yield assertTrue(decoded.autoData.year == 2013)

        }
      }
    )
}
