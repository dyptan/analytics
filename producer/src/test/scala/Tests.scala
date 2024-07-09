package com.dyptan.producer

import com.ria.avro.scala.Advertisement
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeaders
import sttp.client3.testing.SttpBackendStub
import zio.ZIO
import zio.test.{ZIOSpecDefault, assertTrue}

import io.circe._
import io.circe.generic.auto._

object Tests extends ZIOSpecDefault {
  val testingBackend = SttpBackendStub.synchronous
    .whenRequestMatches(_ => true)
    .thenRespond(""" {"username": "john", "age": 65 } """)

  override def spec = suite("ParseJsonSpec2")(
    test("decode json") {
      for {
        adv <- ZIO.readFile("src/test/resources/infoV3.json")
        decoded <- ZIO.fromEither(parser.decode[Advertisement](adv))
        _ <- ZIO.debug(s"got value $decoded")
        _ <- assertTrue(decoded.autoData.year == 2002)
        serialiser <- ZIO.attempt(new AdvertisementSerializer)
        header = new RecordHeaders(Array.empty[Header])
        serialised <- serialiser.serialize("",header,decoded)
      } yield assertTrue(serialised == "2002".getBytes)
    } +
      test("Tes") {
        for {
          adv <- ZIO.readFile("src/test/resources/infoV3.json")
        } yield assertTrue(2002 == 2002)
      }
  )
}
