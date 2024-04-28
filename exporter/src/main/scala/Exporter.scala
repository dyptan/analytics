import io.grpc.ServerBuilder

object Exporter {
  def main(args: Array[String]) = {
    ServerBuilder.forPort(50051)
      .addService(new ExportServiceImpl)
      .build
      .start
      .awaitTermination
  }

}
