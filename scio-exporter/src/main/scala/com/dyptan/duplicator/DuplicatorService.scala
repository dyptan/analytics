package com.dyptan.duplicator

import com.dyptan.gen.proto.{ExportRequest, ExportServiceGrpc, ExportStatus}
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.spotify.scio.ScioContext
import io.grpc.stub.StreamObserver
import org.apache.beam.sdk.io.mongodb.{FindQuery, MongoDbIO}
import org.bson.Document
import org.slf4j.LoggerFactory

class DuplicatorService extends ExportServiceGrpc.ExportServiceImplBase {
  val logger = LoggerFactory.getLogger("DuplicatorService")

  def toJson(messageOrBuilder: MessageOrBuilder): String = JsonFormat.printer.print(messageOrBuilder)

  override def doExport(request: ExportRequest, responseObserver: StreamObserver[ExportStatus]): Unit = {
  val sc = ScioContext()
    logger.info("service has started")
    val filterJson = toJson(request.getQuery.getFilter)
    val filter = Document.parse(filterJson)

    val projectionJson = toJson(request.getQuery.getProjection)
    val projectionSet = Document.parse(projectionJson).keySet()
    val projection: java.util.List[String] = java.util.List.copyOf(projectionSet)
    val read = MongoDbIO.read()
      .withUri("mongodb://mongo:27017")
      .withDatabase("ria")
      .withCollection("advertisementArchive")
      .withQueryFn(
        FindQuery.create().withFilters(filter).withProjection(projection)
      )

    val data = sc.customInput("read from MongoDB", read)

    data.saveAsCustomOutput("write to MongoDB", MongoDbIO.write()
      .withUri("mongodb://mongo:27017")
      .withDatabase("ria")
      .withCollection("advertisementCopy")
    )
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

