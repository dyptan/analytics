import com.spotify.scio.ScioContext
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.io.mongodb.MongoDbIO
import org.apache.kafka.common.serialization.StringDeserializer
import org.bson.Document
import org.slf4j.LoggerFactory

import scala.language.existentials

class MongoArchiver {
  val logger = LoggerFactory.getLogger("MongoArchiver")
  val sc = ScioContext()

  def exportScio(sc: ScioContext) = {
    val filter = Document.parse("filterJson")
    val projectionSet = Document.parse("projectionJson").keySet()
    val projection: java.util.List[String] = java.util.List.copyOf(projectionSet)
    val data = sc.customInput("ReadFromKafka",
      KafkaIO.read[String, String]
      .withBootstrapServers("kafka:9092")
      .withTopic("ria")
      .withKeyDeserializer(classOf[StringDeserializer])
      .withValueDeserializer(classOf[StringDeserializer])
      .withoutMetadata
    )

//    data.saveAsCustomOutput("write to Mongo",
//      MongoDbIO.write()
//        .withUri("mongodb://mongo:27017")
//        .withDatabase("ria")
//        .withCollection("advertisement"))

    sc.run().waitUntilFinish()
  }

}
