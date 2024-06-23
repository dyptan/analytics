package com.dyptan

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ria.avro.scala.Advertisement
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.apache.kafka.common.header.internals.{RecordHeader, RecordHeaders}
import org.apache.kafka.common.header.{Header, Headers}
import sttp.client3.testing.SttpBackendStub
import zio.ZIO
import zio.kafka.serde.Serializer
import zio.test.{ZIOSpecDefault, assertTrue}

//import scala.jdk.CollectionConverters._

//object ExampleSpec extends ZIOSpecDefault {
////  "insert" should {
////    "pass" in {
////      assert(true == true)
////    }
////  }
//  def returnString(str: String): ZIO[Any, Nothing, String] =
//    ZIO.succeed(str)
//  override def spec = suite("TestingApplicationsExamplesSpec")(
//    test("returnString correctly returns string") {
//      val testString = "Hello World!"
//      for {
//        output <- returnString(testString)
//      } yield assertTrue(output == testString)
//    }
//  )
//}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._

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
