package com.dyptan.component;

import com.dyptan.generated.exporter.Client;
import com.dyptan.generated.exporter.ClientException;
import com.dyptan.generated.exporter.DoExportResponse.*;
import com.dyptan.generated.exporter.Handler;
import com.dyptan.generated.exporter.definitions.*;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class Export2S3HandlerImpl implements Handler {
    private static String accept(com.dyptan.generated.exporter.DoExportResponse response) {
        return  switch (response) {
                        case Ok ok -> "Success: %s".formatted(ok.getValue());
                        case BadRequest bad -> "Bad Request: %s".formatted(bad.toString());
                        case InternalServerError error -> "Internal Server Error: %s".formatted(error.toString());
                        default -> "Unexpected response";
        };
    }

    @Override
    public CompletionStage<DoExportResponse> doExport(ProcessRequest body) {
        var client = new Client.Builder(URI.create("http://localhost:8082")).build();
        try {
            var exporterResponse = client.doExport(body).call();
            var result = exporterResponse.toCompletableFuture().join();
            var message = accept(result); // Pass the result to the accept method
            ProcessResponse response = new ProcessResponse.Builder()
                    .withStatus(ProcessResponse.Status.SUCCESS)
                    .withMessage(message)
                    .build();
            return CompletableFuture.completedFuture(DoExportResponse.Ok(response));
        } catch (ClientException e) {
            ProcessResponse response = new ProcessResponse.Builder()
                    .withStatus(ProcessResponse.Status.ERROR)
                    .withMessage("Connection error: " + e.getMessage())
                    .build();
            return CompletableFuture.completedFuture(DoExportResponse.Ok(response));
        } catch (Exception e) {
            ProcessResponse response = new ProcessResponse.Builder()
                    .withStatus(ProcessResponse.Status.ERROR)
                    .withMessage("Unexpected error: " + e.getMessage())
                    .build();
            return CompletableFuture.completedFuture(DoExportResponse.Ok(response));
        }
    }
}
