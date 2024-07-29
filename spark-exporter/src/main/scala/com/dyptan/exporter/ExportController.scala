package com.dyptan.exporter

import akka.actor.ActorSystem
import com.dyptan.generated.exporter.definitions.{ProcessRequest, ProcessResponse}
import com.dyptan.generated.exporter.{Handler, Resource}
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExportController(exportService: ExportService)(implicit
                                                     val logger: Logger,
                                                     ec: ActorSystem
) {
  val exportHandler = new Handler {
    override def doExport(respond: Resource.DoExportResponse.type)(body: ProcessRequest): Future[Resource.DoExportResponse] = {
      exportService.exportToS3()
      Future {
        respond.OK(
          ProcessResponse(Some(ProcessResponse.Status.Success), Some("congrats!"), None)
        )
      }
    }
  }
  val route = Resource.routes(exportHandler)
}
