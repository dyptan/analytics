/*
package com.dyptan.producer

import com.ria.avro.scala.Advertisement
import io.circe._
import io.circe.generic.auto._
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeaders
import zio.ZIO
import zio.test.{ZIOSpecDefault, assertTrue}

import java.util

// Generated with AI
object Tests extends ZIOSpecDefault {
  override def spec = suite("ParseJsonSpec2")(
    test("decode json, serialize, and deserialize back") {
      for {
        adv <- ZIO.readFile("zio-producer/src/test/resources/server_parsed_response.json")
        decoded <- ZIO.fromEither(parser.decode[Advertisement](adv))
        _ <- ZIO.debug(s"got value $decoded")
        _ <- assertTrue(decoded.autoData.year == 2005)
        
        serialiser <- ZIO.attempt(new AdvertisementSerializer)
        headers = new RecordHeaders(Array.empty[Header])
        serialized <- serialiser.serialize("topic", headers, decoded)
        
        deserializer <- ZIO.attempt {
          val avroDeserializer = new KafkaAvroDeserializer()
          val props = new util.HashMap[String, String]
          props.put("schema.registry.url", Conf.registryUrl)
          avroDeserializer.configure(props, false)
          avroDeserializer
        }
        
        deserializedGeneric <- ZIO.attempt(
          deserializer.deserialize("topic", headers, serialized).asInstanceOf[GenericRecord]
        )
        
        year = deserializedGeneric.get("autoData").asInstanceOf[GenericRecord].get("year")
        
        _ <- ZIO.debug(s"Deserialized year value: $year")
      } yield assertTrue(year == 2005 && decoded.autoData.year == 2005)
    }
  )
}
*/
