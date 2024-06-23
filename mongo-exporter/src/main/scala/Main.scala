import io.grpc.ServerBuilder

object ExporterMain extends App{
  ServerBuilder.forPort(50051)
    .addService(new ExportServiceImpl())
    .build
    .start
    .awaitTermination
}
