import com.spotify.scio.ScioContext
//import org.apache.beam.sdk.io.mongodb.{FindQuery, MongoDbIO}
//import org.bson.Document
import org.slf4j.LoggerFactory

import scala.language.existentials

object Archiver {
  val sc = ScioContext()

  val logger = LoggerFactory.getLogger("ExportServiceImpl")
//  val options = PipelineOptionsFactory.create().as(classOf[S3Options])
//  options.setEndpoint(new URI("http://minio:9000"))
//  options.setAwsCredentialsProvider(EnvironmentVariableCredentialsProvider.create())

  def exportScio(sc: ScioContext, request: String) = {
//    val filter = Document.parse(filterJson)

//    val projectionSet = Document.parse(projectionJson).keySet()
//    val projection: java.util.List[String] = java.util.List.copyOf(projectionSet)

//    val data = sc.customInput("read from MongoDB", MongoDbIO.read()
//      .withUri("mongodb://mongo:27017")
//      .withDatabase("ria")
//      .withCollection("advertisement")
//      .withQueryFn(
//        FindQuery.create().withFilters(filter).withProjection(projection)
//      )
//    )

//    data.map(_.toJson()).saveAsCustomOutput("write to Minio",
//      TextIO.write().to(s"s3://export-bucket/${request.getExportName}"))

    sc.run().waitUntilFinish()
  }

  def doExport(request: String
//                        , responseObserver: StreamObserver[ExportStatus]
                       ) = {
//    val format = request.getFormat.name()
    try {

      exportScio(sc, "")
//      val confirmationMessage = ExportStatus.newBuilder.setStatus("ok").build
//      responseObserver.onNext(confirmationMessage)
//      responseObserver.onCompleted()
    } catch {
      case e: Throwable => ()
//        val status = ExportStatus.newBuilder.setStatus(e.toString).build
//        logger.error("something bad happened", e)
//        responseObserver.onNext(status)
//        responseObserver.onCompleted()
    }
  }
}
