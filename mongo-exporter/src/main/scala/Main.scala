package com.dyptan

import com.dyptan.service.ExportServiceImpl
import io.grpc.ServerBuilder

object Main extends App{
  ServerBuilder.forPort(50051)
    .addService(new ExportServiceImpl())
    .build
    .start
    .awaitTermination
}
