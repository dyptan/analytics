import com.dyptan.gen.proto.{ExportRequest, ExportServiceGrpc, ExportStatus}
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.spotify.scio.ScioContext
import io.grpc.stub.StreamObserver
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.io.aws2.options.S3Options
import org.apache.beam.sdk.io.mongodb.{FindQuery, MongoDbIO}
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.bson.Document
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider

import java.net.URI
import scala.language.existentials

class ExportServiceImpl extends ExportServiceGrpc.ExportServiceImplBase {
  val logger = LoggerFactory.getLogger("ExportServiceImpl")
  def toJson(messageOrBuilder: MessageOrBuilder): String = JsonFormat.printer.print(messageOrBuilder)
  override def doExport(request: ExportRequest, responseObserver: StreamObserver[ExportStatus]) = {

    val filterJson = toJson(request.getQuery.getFilter)
    val filter = Document.parse(filterJson)

    val projectionJson = toJson(request.getQuery.getProjection)
    val projectionSet = Document.parse(projectionJson).keySet()
    val projection: java.util.List[String] = java.util.List.copyOf(projectionSet)

    val s3Opt = PipelineOptionsFactory.create().as(classOf[S3Options])
    s3Opt.setEndpoint(new URI("http://127.0.0.1:9000"))
    s3Opt.setAwsCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
    val sc = ScioContext(s3Opt)

    val data = sc.customInput("read from MongoDB", MongoDbIO.read()
      .withUri("mongodb://localhost:27017")
      .withDatabase("vlp")
      .withCollection("leads").withQueryFn(
        FindQuery.create().withFilters(filter).withProjection(projection)
      )
    )

    data.map(_.toJson()).saveAsCustomOutput("write to MongoDB",
      TextIO.write().to("s3://export-bucket/test"))

    try {
      sc.run().waitUntilFinish()
      val confirmationMessage = ExportStatus.newBuilder.setStatus("ok").build
      responseObserver.onNext(confirmationMessage)
      responseObserver.onCompleted()
    } catch {
      case e: Throwable => val confirmationMessage = ExportStatus.newBuilder.setStatus(e.toString).build
        logger.error("something bad happened", e)
        responseObserver.onNext(confirmationMessage)
        responseObserver.onCompleted()
    }
  }
}
