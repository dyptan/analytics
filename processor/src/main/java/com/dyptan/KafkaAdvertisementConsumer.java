package com.dyptan;

import com.dyptan.avro.Advertisement;
import com.google.gson.Gson;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.extensions.avro.coders.AvroCoder;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.io.mongodb.MongoDbIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
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

import java.io.Serializable;
import java.util.Collections;

public class KafkaAdvertisementConsumer implements Serializable {
    static Logger logger = LoggerFactory.getLogger("KafkaAdvertisementConsumer");

    public void run() {
        PipelineOptions options = PipelineOptionsFactory.create();
        Pipeline pipeline = Pipeline.create(options);

        PTransform<PBegin, PCollection<KafkaRecord<Integer, Advertisement>>> read = KafkaIO.<Integer, Advertisement>read()
                .withBootstrapServers("http://kafka:9092")
                .withTopic("ria")
                .withConsumerConfigUpdates(Collections.singletonMap("specific.avro.reader", (Object) "true"))
                .withConsumerConfigUpdates(Collections.singletonMap("auto.offset.reset", (Object) "latest"))
                .withConsumerConfigUpdates(Collections.singletonMap("schema.registry.url", (Object) "http://localhost:8081"))
                .withKeyDeserializer(IntegerDeserializer.class)
                .withValueDeserializerAndCoder((Class) KafkaAvroDeserializer.class, AvroCoder.of(Advertisement.class));


        PCollection<KV<Integer, Advertisement>> advertisementRecords =
                pipeline.apply(read)
                        .apply("ExtractRecord", ParDo.of(
                                new DoFn<KafkaRecord<Integer, Advertisement>, KV<Integer, Advertisement>>() {
                                    @ProcessElement
                                    public void processElement(ProcessContext c) {
                                        KafkaRecord<Integer, Advertisement> record = c.element();
                                        KV<Integer, Advertisement> log = record.getKV();
                                        logger.debug("Key Obtained: " + log.getKey());
                                        logger.debug("Value Obtained: " + log.getValue().toString());
                                        c.output(record.getKV());

                                    }
                                }));

        PCollection<Document> mongoDocumentCollection = advertisementRecords
                .apply("TransformToDocument", ParDo.of(new KVToMongoDocumentFn()));

        String mongoURI = "mongodb://localhost:27017";
        String databaseName = "ria";
        String collectionName = "advertisement";

        mongoDocumentCollection.apply(
                MongoDbIO.write()
                        .withUri(mongoURI)
                        .withDatabase(databaseName)
                        .withCollection(collectionName)

        );
        pipeline.run().waitUntilFinish();
        System.out.println("Pipeline finished");
    }

    public static class KVToMongoDocumentFn extends DoFn<KV<Integer, Advertisement>, Document> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            KV<Integer, Advertisement> input = c.element();
            Document document = Document.parse(new Gson().toJson(input.getValue(), Advertisement.class).toString());
            document.put("_id", new ObjectId(String.format("%024d", input.getKey())));
            c.output(document);
        }
    }
}
