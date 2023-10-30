package com.dyptan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

public class StreamTransformer implements  Runnable{
    final static Logger logger = LoggerFactory.getLogger(StreamTransformer.class.getName());

    private String KAFKA_BOOTSTRAP_SERVER;
    private String KAFKA_TOPIC;
    String MODEL_PATH;
    String MODEL_NAME;
    public int id;

    public StreamTransformer(int Id) {
        this.id = Id;

        // We need this to find the config file from env var when running from IDE
        String configFile = Optional.ofNullable(System.getenv("CONF_FILE")).orElse("conf/streamer.properties");
        logger.info("Loading config file from "+configFile);

        Properties streamingConfig = new Properties();
        try {
            streamingConfig.load(Files.newInputStream(Paths.get(configFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

//      Setting up Stream properties
        KAFKA_BOOTSTRAP_SERVER = streamingConfig.getProperty("source.kafka.bootstrap.server");
        KAFKA_TOPIC = streamingConfig.getProperty("source.kafka.topic");
        MODEL_PATH = streamingConfig.getProperty("model.path");
        MODEL_NAME = streamingConfig.getProperty("model.name");


        if (Files.notExists(Paths.get(MODEL_PATH+"/"+MODEL_NAME))) {
            logger.error("Model does not exist: "+MODEL_PATH+MODEL_NAME);
            logger.error("Falling back to default model: model/dummy");
            MODEL_PATH="model/";
            MODEL_NAME="dummy";
        } else if (Files.notExists(Paths.get(MODEL_PATH+MODEL_NAME))) {
            logger.error("Default model does not exist. Exiting");
            System.exit(1);
        }

        if (KAFKA_TOPIC == null | KAFKA_BOOTSTRAP_SERVER == null) {
            throw new RuntimeException("Kafka configuration not found");
        }


    }

    @Override
    public void run() {
        logger.info("Start reading data from source");

    }

    public void applyNewModel(String name) {
        MODEL_NAME = name;
    }


}
