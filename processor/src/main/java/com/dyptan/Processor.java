package com.dyptan;


import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.io.mongodb.MongoDbIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class Processor {
    private static final String TOPIC_NAME = "ria";

    public interface KafkaStreamingOptions extends PipelineOptions {
        @Description("Kafka server host")
        @Default.String("localhost:9092")
        String getKafkaHost();

        void setKafkaHost(String value);
    }

    public static void main(String[] args) {
        KafkaStreamingOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(KafkaStreamingOptions.class);

        options.setKafkaHost("localhost:9092");

        KafkaConsumer kafkaConsumer = new KafkaConsumer(options);
        kafkaConsumer.run();
    }

    public void consume(String[] args) {
        KafkaStreamingOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(KafkaStreamingOptions.class);
        KafkaConsumer kafkaConsumer = new KafkaConsumer(options);
        kafkaConsumer.run();
    }

    public static class KafkaConsumer {
        private final KafkaStreamingOptions options;

        public KafkaConsumer(KafkaStreamingOptions options) {
            this.options = options;
        }

        public void run() {
            Pipeline pipeline = Pipeline.create(options);

            Map<String, Object> consumerConfig = new HashMap<>();

            // Start reading form Kafka with the latest offset
            consumerConfig.put("auto.offset.reset", "earliest");

            PCollection<KV<String, String>> keyValueCollection =
                    pipeline.apply(
                            KafkaIO.<String, String>read()
                                    .withBootstrapServers(options.getKafkaHost())
                                    .withTopic(TOPIC_NAME)
                                    .withKeyDeserializer(StringDeserializer.class)
                                    .withValueDeserializer(StringDeserializer.class)
                                    .withConsumerConfigUpdates(consumerConfig)
                                    .withoutMetadata());

            PCollection<Document> mongoDocumentCollection = keyValueCollection.apply(
                    ParDo.of(new KVToMongoDocumentFn())
            );

            String mongoURI = "mongodb://localhost:27017";
            String databaseName = "ria";
            String collectionName = "autos";

            mongoDocumentCollection.apply(
                    MongoDbIO.write()
                            .withUri(mongoURI)
                            .withDatabase(databaseName)
                            .withCollection(collectionName)
            );
            pipeline.run().waitUntilFinish();
            System.out.println("Pipeline finished");
        }

        public static class KVToMongoDocumentFn extends DoFn<KV<String, String>, Document> {
            @ProcessElement
            public void processElement(@Element KV<String, String> input, OutputReceiver<Document> output) {
                // Transforming the KV<String, String> to a MongoDB Document
                Document document = new Document()
                        .append("key", input.getKey())
                        .append("value", input.getValue());
                output.output(document);
            }
        }
    }

}


