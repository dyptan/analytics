package com.dyptan.crawler

import com.dyptan.avro.{Advertisement, AutoData, Geography}
import com.dyptan.crawler.Conf.Ad
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecord}
import org.apache.kafka.common.header.Headers
import zio.{Task, ZIO}
import zio.kafka.serde

import java.io.ByteArrayOutputStream
import java.util

object AvroConverter {
  def toAvro(record: Ad) = {
    val autoData = AutoData.newBuilder()
      .setYear(record.autoData.year)
      .setAutoId(record.autoData.autoId)
      .setBodyId(record.autoData.bodyId)
      .setRaceInt(record.autoData.raceInt)
      .setFuelId(record.autoData.fuelId)
      .setFuelNameEng(record.autoData.fuelNameEng)
      .setGearBoxId(record.autoData.gearBoxId)
      .setGearboxName(record.autoData.gearboxName)
      .setDriveId(record.autoData.driveId)
      .setDriveName(record.autoData.driveName)
      .setCategoryId(record.autoData.categoryId)
      .setCategoryNameEng(record.autoData.categoryNameEng)
      .setSubCategoryNameEng(record.autoData.subCategoryNameEng)
      .build();

    val geography = Geography.newBuilder()
      .setStateId(record.stateData.stateId)
      .setCityId(record.stateData.cityId)
      .setRegionNameEng(record.stateData.regionNameEng)
      .build();

    val advertisement = Advertisement.newBuilder()
      .setUSD(record.USD)
      .setAddDate(record.addDate)
      .setSoldDate(record.soldDate)
      .setAutoData(autoData)
      .setMarkId(record.markId)
      .setMarkNameEng(record.markNameEng)
      .setModelId(record.modelId)
      .setModelNameEng(record.modelNameEng)
      .setLinkToView(record.linkToView)
      .setStateData(geography)
      .build();
    advertisement
  }
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
  props.put("schema.registry.url","http://schema-registry:8081")
  avroSerializer.configure(props, false)
  override def serialize(topic: String, headers: Headers, data: Advertisement): Task[Array[Byte]] = {
    val genericRecord = AvroConverter.convertToGenericRecord(data)

    ZIO.attempt(avroSerializer.serialize(topic, headers, genericRecord))
  }
}