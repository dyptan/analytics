package com.dyptan;

import com.dyptan.consumer.Consumer;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public class ProcessorSpec {

    static Logger logger = LoggerFactory.getLogger("ProcessorSpec");
    private KafkaConsumer<Integer, String> consumer;
    private File outputFile;

    @BeforeEach
    public void setUp() throws IOException {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("ria"));

        outputFile = File.createTempFile("kafka-output", ".txt");
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    @Test
    public void testPipelineWithAvroMessage() throws InterruptedException, IOException {
        // Start Kafka and MongoDB containers
//        logger.info("Starting Kafka and MongoDB containers...");
//        KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
//        kafkaContainer.start();

        MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"));
        mongoDBContainer.start();

        logger.info("Kafka and MongoDB containers started.");

        // Set up properties for the pipeline
        logger.info("Setting up properties for the pipeline...");
        Properties testProperties = new Properties();
        testProperties.setProperty("kafka.bootstrap.servers", "kafka:9092"); //kafkaContainer.getBootstrapServers());
        testProperties.setProperty("kafka.producer.topic", "ria");
        testProperties.setProperty("schema.registry.url", "http://localhost:8081"); // Adjust if needed
        testProperties.setProperty("mongo.uri", "mongodb://localhost:27017");//mongoDBContainer.getReplicaSetUrl());
        testProperties.setProperty("database.name", "test_db");
        testProperties.setProperty("collection.name", "advertisement");

        logger.info("Properties set: " + testProperties);

        // Create and run the pipeline asynchronously
        logger.info("Creating and running the pipeline asynchronously...");
        PipelineOptions options = PipelineOptionsFactory.create();
        Pipeline testPipeline = Consumer.createPipeline(options, testProperties);
        Thread pipelineThread = new Thread(() -> testPipeline.run().waitUntilFinish());
        pipelineThread.start();

        logger.info("Pipeline started.");

        // Wait for the pipeline to start
        Thread.sleep(3000); // Adjust the sleep time as needed

        // Send the Avro message to the Kafka topic
        logger.info("Sending the Avro message to the Kafka topic...");
        File avroFile = new File("src/test/resources/message.avro");
        assertTrue(avroFile.exists(), "Avro file should exist for testing");

        try (KafkaProducer<Integer, byte[]> producer = new KafkaProducer<>(createProducerProperties("kafka:9092"))) {
            byte[] avroData = java.nio.file.Files.readAllBytes(avroFile.toPath());
            logger.info("Read Avro file data: " + avroData.length + " bytes.");
            ProducerRecord<Integer, byte[]> record = new ProducerRecord<>("ria", 1, avroData);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Error sending message to Kafka", exception);
                } else {
                    logger.info("Message sent to Kafka topic: " + metadata.topic() + ", partition: " + metadata.partition() + ", offset: " + metadata.offset());
                }
            });
        } catch (Exception e) {
            logger.error("Error while sending Avro message to Kafka", e);
        }

        logger.info("Avro message sent to Kafka.");

        // Wait for the pipeline to process data
        Thread.sleep(2000); // Adjust the sleep time as needed

        // Verify MongoDB collection
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("test_db");
            MongoCollection<Document> collection = database.getCollection("advertisement");

            long count = collection.countDocuments();
            assertTrue(count > 0, "MongoDB collection should contain at least one document");
        }

        // Stop containers
//        kafkaContainer.stop();
        mongoDBContainer.stop();
    }

    private KafkaProducer<Integer, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    }

    private Properties createProducerProperties(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        return props;
    }
}


