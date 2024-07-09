package com.dyptan.producer

import com.dyptan.producer.Conf.registryUrl
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecord}
import org.apache.kafka.common.header.Headers
import zio.kafka.serde
import zio.{Task, ZIO}
import java.io.ByteArrayOutputStream
import java.util
import com.ria.avro.scala.Advertisement

object AvroConverter {
  def convertToGenericRecord(record: SpecificRecord): GenericRecord = {
    val writer = new SpecificDatumWriter[SpecificRecord](record.getSchema)
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(out, null)
    writer.write(record, encoder)
    encoder.flush()
    out.close()

    val decoder = DecoderFactory.get.binaryDecoder(out.toByteArray, null)
    val reader = new SpecificDatumReader[GenericRecord](record.getSchema)
    reader.read(null, decoder)
  }
}

class AdvertisementSerializer extends serde.Serializer[Any, Advertisement] {
  val avroSerializer = new KafkaAvroSerializer()
  val props = new util.HashMap[String, String]
  props.put("schema.registry.url", registryUrl)
  avroSerializer.configure(props, false)
  override def serialize(topic: String, headers: Headers, data: Advertisement): Task[Array[Byte]] = {
    val genericRecord = AvroConverter.convertToGenericRecord(data)
    ZIO.attempt(avroSerializer.serialize(topic, headers, genericRecord))
  }
}