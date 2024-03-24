import com.dyptan.gen.proto.{ExportRequest, ExportServiceGrpc, ExportStatus}
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.spotify.scio.{ContextAndArgs, ScioContext}
import io.grpc.{ServerBuilder, ServerServiceDefinition}
import io.grpc.stub.StreamObserver
import org.apache.avro.data.Json
import org.apache.beam.sdk.io.mongodb.{FindQuery, MongoDbIO}
import org.bson.Document

import java.util.stream.Collectors
import scala.collection.mutable.ListBuffer
import scala.language.existentials
import scala.concurrent.ExecutionContext

class ExportServiceImpl extends ExportServiceGrpc.ExportServiceImplBase {
  def toJson(messageOrBuilder: MessageOrBuilder): String = JsonFormat.printer.print(messageOrBuilder)
  override def doExport(request: ExportRequest, responseObserver: StreamObserver[ExportStatus]) = {
    val sc = ScioContext()

    val filterJson = toJson(request.getFilter.getFilter)
    val filter = Document.parse(filterJson)

    val projectionJson = toJson(request.getFilter.getProjection)
    val projectionSet = Document.parse(projectionJson).keySet()
    val projection: java.util.List[String] = java.util.List.copyOf(projectionSet)

    val data = sc.customInput("read from MongoDB", MongoDbIO.read()
      .withUri("mongodb://localhost:27017")
      .withDatabase("test")
      .withCollection("customers").withQueryFn(
        FindQuery.create().withFilters(filter).withProjection(projection)
      )
      )

    data.saveAsCustomOutput("write to MongoDB", MongoDbIO.write()
        .withUri("mongodb://localhost:27017")
        .withDatabase("test")
        .withCollection("customers_copy")
      )

    try {
      val result = sc.run().waitUntilFinish()
      val confirmationMessage = ExportStatus.newBuilder.setStatus("ok").build
      responseObserver.onNext(confirmationMessage)
      responseObserver.onCompleted()
    } catch {
      case e: Throwable => val confirmationMessage = ExportStatus.newBuilder.setStatus(e.toString).build
        responseObserver.onNext(confirmationMessage)
        responseObserver.onCompleted()
    }
  }
}

object Main {
  val ec = ExecutionContext.global
  def main(args: Array[String]): Unit = {
    ServerBuilder.forPort(50051)
      .addService(new ExportServiceImpl)
      .build
      .start
      .awaitTermination
  }
}