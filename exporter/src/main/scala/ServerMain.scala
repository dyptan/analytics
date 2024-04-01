import io.grpc.ServerBuilder

object ServerMain {
  def main(args: Array[String]) = {
    ServerBuilder.forPort(50051)
      .addService(new ExportServiceImpl)
      .build
      .start
      .awaitTermination
  }

}
