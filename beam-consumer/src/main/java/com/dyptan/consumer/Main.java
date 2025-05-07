package com.dyptan.consumer;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class Main {
    static Logger logger = LoggerFactory.getLogger("Consumer logger");
    static Properties properties = new Properties();

    public static void main(String[] args) throws IOException {
        var propertiesFile = Main.class.getResource("/application.properties");
        if (propertiesFile == null) {
            throw new IOException("Properties file not found in classpath");
        }
        try (var inputStream = propertiesFile.openStream()) {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Failed to load properties file", e);
            throw e;
        }

        PipelineOptions options = PipelineOptionsFactory.create();
        Pipeline pipeline = Consumer.createPipeline(options, properties);

        logger.info("Starting processing pipeline...");
        pipeline.run().waitUntilFinish();
    }
}
