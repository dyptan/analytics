import com.dyptan.gen.proto.{ExportFormat, ExportRequest, ExportServiceGrpc, ExportStatus}
import io.grpc.stub.StreamObserver
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

import scala.language.existentials

class ExportServiceImpl extends ExportServiceGrpc.ExportServiceImplBase {
  val logger = LoggerFactory.getLogger("ExportServiceImpl")

  val spark = SparkSession.builder()
    .appName("exporter")
    .master("local[*]")
    .config("spark.mongodb.read.connection.uri", "mongodb://localhost:27017")
    .getOrCreate()

  val advertisements = spark.read
    .format("mongodb")
    .option("database", "ria")
    .option("collection", "advertisement")
    .load

  def exportCsv(dataFrame: DataFrame, request: ExportRequest) = {
    dataFrame.write.csv(request.getExportName)
  }

  override def doExport(request: ExportRequest, responseObserver: StreamObserver[ExportStatus]) = {
    try {
      request.getFormat match {
        case ExportFormat.CSV => exportCsv(advertisements, request)
        case _ =>
      }

      val confirmationMessage = ExportStatus.newBuilder.setStatus("ok").build
      responseObserver.onNext(confirmationMessage)
      responseObserver.onCompleted()
    } catch {
      case e: Throwable =>
        val status = ExportStatus.newBuilder.setStatus(e.toString).build
        logger.error("something bad happened", e)
        responseObserver.onNext(status)
        responseObserver.onCompleted()
    }
  }
}
