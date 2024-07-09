package com.dyptan.duplicator

import io.grpc.ServerBuilder

object Main extends App{
  ServerBuilder.forPort(50051)
    .addService(new DuplicatorService())
    .build
    .start
    .awaitTermination
}
