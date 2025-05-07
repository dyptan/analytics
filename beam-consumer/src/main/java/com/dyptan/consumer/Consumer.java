package com.dyptan.consumer;

import com.google.gson.Gson;
import com.ria.avro.Advertisement;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.extensions.avro.coders.AvroCoder;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.io.mongodb.MongoDbIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

public class Consumer {
    static Logger logger = LoggerFactory.getLogger("MessageProcessor");

    public static Pipeline createPipeline(PipelineOptions options, Properties properties) {
        Pipeline pipeline = Pipeline.create(options);
        logger.info("Pipeline has been initialised");

        @SuppressWarnings("unchecked") PTransform<PBegin, PCollection<KafkaRecord<Integer, Advertisement>>> kafkaConsumer =
                KafkaIO.<Integer, Advertisement>read()
                        .withBootstrapServers(properties.getProperty("kafka.bootstrap.servers"))
                        .withTopic(properties.getProperty("kafka.consumer.topic"))
                        .withConsumerConfigUpdates(Collections.singletonMap("specific.avro.reader", "true"))
                        .withConsumerConfigUpdates(Collections.singletonMap("fetch.max.wait.ms", "5000"))
                        .withConsumerConfigUpdates(Collections.singletonMap("auto.offset.reset", "earliest"))
                        .withConsumerConfigUpdates(Collections.singletonMap("schema.registry.url", properties.getProperty("schema.registry.url")))
                        .withConsumerConfigUpdates(Collections.singletonMap("group.id", properties.getProperty("kafka.consumer.group.id")))
                        .withKeyDeserializer(IntegerDeserializer.class)
                        .withValueDeserializerAndCoder((Class) KafkaAvroDeserializer.class, AvroCoder.of(Advertisement.class));

        PCollection<KV<Integer, Advertisement>> advertisementRecords = pipeline.apply(kafkaConsumer).apply("ExtractRecord", ParDo.of(new DoFn<KafkaRecord<Integer, Advertisement>, KV<Integer, Advertisement>>() {
            @ProcessElement
            public void processElement(ProcessContext c) {
                KafkaRecord<Integer, Advertisement> record = c.element();
                KV<Integer, Advertisement> log = record.getKV();
                logger.info("Key Obtained: " + log.getKey());
                logger.info("Value Obtained: " + log.getValue().toString());
                c.output(record.getKV());
            }
        }));

        PCollection<Document> mongoDocuments = advertisementRecords.apply("TransformToDocument", ParDo.of(new DoFn<KV<Integer, Advertisement>, Document>() {
            @ProcessElement
            public void processElement(ProcessContext c) {
                logger.info("Processing element started");
                try {
                    KV<Integer, Advertisement> input = c.element();
                    logger.info("Received key: " + input.getKey());
                    logger.info("Received value: " + input.getValue().toString());

                    Document document = Document.parse(new Gson().toJson(input.getValue(), Advertisement.class));
                    document.put("_id", new ObjectId(String.format("%024d", input.getKey())));
                    c.output(document);

                    logger.info("Document created and outputted: " + document.toJson());
                } catch (Exception e) {
                    logger.error("Error processing element", e);
                }
                logger.info("Processing element finished");
            }
        }));

        String mongoURI = properties.getProperty("mongo.uri");
        String databaseName = properties.getProperty("database.name");
        String collectionName = properties.getProperty("collection.name");

        mongoDocuments.apply(MongoDbIO.write().withUri(mongoURI).withDatabase(databaseName).withCollection(collectionName));

        mongoDocuments.apply(MongoDbIO.write().withUri(mongoURI).withDatabase(databaseName).withCollection(collectionName + "Archive"));

        return pipeline;
    }
}
