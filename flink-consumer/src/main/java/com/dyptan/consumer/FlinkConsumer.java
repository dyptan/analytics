package com.dyptan.consumer;

import com.google.gson.Gson;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.ria.avro.Advertisement;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class FlinkConsumer {
    private static final Logger logger = LoggerFactory.getLogger(FlinkConsumer.class);

    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (InputStream input = FlinkConsumer.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalArgumentException("config.properties file not found in classpath");
            }
            config.load(input);
        }

        String kafkaBootstrapServers = config.getProperty("kafka.bootstrap.servers");
        String kafkaTopic = config.getProperty("kafka.consumer.topic");
        String mongoUri = config.getProperty("mongo.uri");
        String databaseName = config.getProperty("database.name");
        String collectionName = config.getProperty("collection.name");
        String schemaRegistryUrl = config.getProperty("schema.registry.url");

        // Print out configuration for debugging
        logger.info("Using Kafka Bootstrap Servers: {}", kafkaBootstrapServers);
        logger.info("Using Kafka Topic: {}", kafkaTopic);
        logger.info("Using MongoDB URI: {}", mongoUri);
        logger.info("Using Schema Registry URL: {}", schemaRegistryUrl);

        if (kafkaBootstrapServers == null || kafkaTopic == null || mongoUri == null || 
            databaseName == null || collectionName == null || schemaRegistryUrl == null) {
            throw new IllegalArgumentException("Missing required configuration parameters.");
        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        KafkaSource<Advertisement> kafkaSource = KafkaSource.<Advertisement>builder()
            .setBootstrapServers(kafkaBootstrapServers)
            .setTopics(kafkaTopic)
            .setGroupId("flink-consumer-group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new AdvertisementDeserializationSchema(schemaRegistryUrl))
            .build();

        DataStream<Advertisement> kafkaStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source");

        kafkaStream.map(advertisement -> {
            logger.info("Received message: {}", advertisement);
            try {
                Gson gson = new Gson();
                String jsonString = gson.toJson(advertisement);
                Document document = Document.parse(jsonString);
                document.put("_id", new ObjectId());

                try (var mongoClient = MongoClients.create(mongoUri)) {
                    MongoDatabase database = mongoClient.getDatabase(databaseName);
                    MongoCollection<Document> advertisementCollection = database.getCollection(collectionName);
                    MongoCollection<Document> archiveCollection = database.getCollection("advertisementArchive");
                    advertisementCollection.insertOne(document);
                    archiveCollection.insertOne(new Document(document)); // Insert a copy into archive
                    logger.info("Successfully inserted document into MongoDB collections");
                } catch (Exception e) {
                    logger.error("Error inserting into MongoDB: {}", e.getMessage(), e);
                }

                return document.toJson();
            } catch (Exception e) {
                logger.error("Error processing message: {}", e.getMessage(), e);
                return "Error: " + e.getMessage();
            }
        });

        env.execute("Flink Kafka to MongoDB Consumer");
    }
}

class AdvertisementDeserializationSchema implements DeserializationSchema<Advertisement> {

    private final String schemaRegistryUrl;

    public AdvertisementDeserializationSchema(String schemaRegistryUrl) {
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    @Override
    public Advertisement deserialize(byte[] message) throws IOException {
        try (KafkaAvroDeserializer avroDeserializer = new KafkaAvroDeserializer()) {
            Map<String, Object> props = new java.util.HashMap<>();
            props.put("schema.registry.url", schemaRegistryUrl);
            props.put("specific.avro.reader", true); // Ensure specific record is returned
            avroDeserializer.configure(props, false);
            Object result = avroDeserializer.deserialize(null, message);
            if (!(result instanceof Advertisement)) {
                LoggerFactory.getLogger(AdvertisementDeserializationSchema.class)
                    .error("Deserialized object is not of type Advertisement: {}", result);
                return null;
            }
            return (Advertisement) result;
        } catch (Exception e) {
            LoggerFactory.getLogger(AdvertisementDeserializationSchema.class)
                .error("Error during Avro deserialization: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean isEndOfStream(Advertisement nextElement) {
        return false;
    }

    @Override
    public TypeInformation<Advertisement> getProducedType() {
        return TypeInformation.of(Advertisement.class);
    }
}