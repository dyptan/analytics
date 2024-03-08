package com.dyptan

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ria.avro.scala.Advertisement
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import zio.ZIO
import zio.test.{ZIOSpecDefault, assertTrue}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object Tests extends ZIOSpecDefault {

  def spec =
    suite("ParseJsonSpec")(

      test("Tes") {
        def extractFieldInfo(schema: Schema): String = {
          val mapper = new ObjectMapper()
          val rootNode = mapper.createObjectNode()
          schema.getFields.asScala.toList.foreach(f => recHelper(f, mapper, rootNode))
          mapper.writeValueAsString(rootNode)
        }

        def recHelper(field: Field, mapper: ObjectMapper, output: ObjectNode): ObjectNode = {
          field.schema().getType.getName match {
            case "record" =>
              val node = mapper.createObjectNode()
              field.schema().getFields.asScala.toList.foreach(recHelper(_, mapper, node))
              output.set(field.name(), node)
            case "union" => output.put(field.name, field.schema().getTypes.get(1).getType.getName)
            case _ => output.put(field.name, field.schema().getName)
          }
        }

        val schema = Advertisement.SCHEMA$
        val allFieldInfo = extractFieldInfo(schema)

        for {
          json <- ZIO.succeed(allFieldInfo)
          _ <- ZIO.debug(s"$json")
        } yield assertTrue(true)

      }

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
