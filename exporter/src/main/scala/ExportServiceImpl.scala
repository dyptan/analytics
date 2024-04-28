import com.dyptan.gen.proto.{ExportRequest, ExportServiceGrpc, ExportStatus}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.spotify.scio.ScioContext
import io.grpc.stub.StreamObserver
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.io.aws2.options.S3Options
import org.apache.beam.sdk.io.mongodb.{FindQuery, MongoDbIO}
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.spark.sql.SparkSession
import org.bson.Document
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider

import java.net.URI
import scala.language.existentials

class ExportServiceImpl extends ExportServiceGrpc.ExportServiceImplBase {
  val logger = LoggerFactory.getLogger("ExportServiceImpl")
  def toJson(messageOrBuilder: MessageOrBuilder): String = JsonFormat.printer.print(messageOrBuilder)
  val options = PipelineOptionsFactory.create().as(classOf[S3Options])
  options.setEndpoint(new URI("http://minio:9000"))
  options.setAwsCredentialsProvider(EnvironmentVariableCredentialsProvider.create())

  val spark = SparkSession.builder().appName("exporter").master("local[*]").getOrCreate()
  spark.read.format("mongodb").load()

  val sc = ScioContext(options)

  def exportScio(sc: ScioContext, request: ExportRequest) = {
    val filterJson = toJson(request.getQuery.getFilter)
    val filter = Document.parse(filterJson)

    val projectionJson = toJson(request.getQuery.getProjection)
    val projectionSet = Document.parse(projectionJson).keySet()
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
      TextIO.write().to(s"s3://export-bucket/${request.getExportName}"))

    sc.run().waitUntilFinish()
  }

  override def doExport(request: ExportRequest, responseObserver: StreamObserver[ExportStatus]) = {
    val format = request.getFormat.getDescriptorForType.getFullName
    try {

      val confirmationMessage = ExportStatus.newBuilder.setStatus("ok").build
      responseObserver.onNext(confirmationMessage)
      responseObserver.onCompleted()
    } catch {
      case e: Throwable => val status = ExportStatus.newBuilder.setStatus(e.toString).build
        logger.error("something bad happened", e)
        responseObserver.onNext(status)
        responseObserver.onCompleted()
    }
  }
}
