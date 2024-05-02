import com.spotify.scio.ScioContext
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.io.aws2.options.S3Options
import org.apache.beam.sdk.io.mongodb.{FindQuery, MongoDbIO}
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.bson.Document
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider

import java.net.URI
import scala.language.existentials

class S3Archiver {
  val logger = LoggerFactory.getLogger("ExportServiceImpl")
  val options = PipelineOptionsFactory.create().as(classOf[S3Options])
  options.setEndpoint(new URI("http://minio:9000"))
  options.setAwsCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
  val sc = ScioContext(options)

  def exportScio(sc: ScioContext) = {
    val filter = Document.parse("filterJson")
    val projectionSet = Document.parse("projectionJson").keySet()
    val projection: java.util.List[String] = java.util.List.copyOf(projectionSet)
    val data = sc.customInput("read from MongoDB", MongoDbIO.read()
      .withUri("mongodb://mongo:27017")
      .withDatabase("ria")
      .withCollection("advertisement")
      .withQueryFn(
        FindQuery.create().withFilters(filter).withProjection(projection)
      )
    )

    data.map(_.toJson()).saveAsCustomOutput("write to Minio",
      TextIO.write().to(s"s3://export-bucket/name"))

    sc.run().waitUntilFinish()
  }

}
