package com.dyptan;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.io.mongodb.MongoDbIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.Document;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Processor {

    // Kafka topic name
    private static final String TOPIC_NAME = "ria";

    // The deadline for processing late data
    private static final int ALLOWED_LATENESS_TIME = 1;

    // Delay time after the first element in window
    private static final int TIME_OUTPUT_AFTER_FIRST_ELEMENT = 10;

    // The time of the window in which the elements will be processed
    private static final int WINDOW_TIME = 30;

    // Number of game events to send during game window
    private static final int MESSAGES_COUNT = 100;

    // List usernames
    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "David"};

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss");

    public interface KafkaStreamingOptions extends PipelineOptions {
        @Description("Kafka server host")
        @Default.String("localhost:9092")
        String getKafkaHost();

        void setKafkaHost(String value);
    }

    public static void main(String[] args) {
        // FixedWindows will always start at an integer multiple of the window size counting from epoch
        // start.
        // To get nicer looking results we will start producing results right after the next window
        // starts.
        Duration windowSize = Duration.standardSeconds(WINDOW_TIME);
        Instant nextWindowStart =
                new Instant(
                        Instant.now().getMillis()
                                + windowSize.getMillis()
                                - Instant.now().plus(windowSize).getMillis() % windowSize.getMillis());

        KafkaStreamingOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(KafkaStreamingOptions.class);

        Timer timer = new Timer();

        options.setKafkaHost("localhost:9092");

        /*
         * Kafka producer which sends messages (works in background thread)
         */
//        KafkaProducer producer = new KafkaProducer(options);
//        timer.schedule(producer, nextWindowStart.toDate());

        /*
         * Kafka consumer which reads messages
         */
        KafkaConsumer kafkaConsumer = new KafkaConsumer(options);
        kafkaConsumer.run();
    }

    public void consume(String[] args) {
        KafkaStreamingOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(KafkaStreamingOptions.class);
        KafkaConsumer kafkaConsumer = new KafkaConsumer(options);
        kafkaConsumer.run();
    }

    // Kafka producer
    public static class KafkaProducer extends TimerTask {
        private final KafkaStreamingOptions options;

        public KafkaProducer(KafkaStreamingOptions options) {
            this.options = options;
        }

        @Override
        public void run() {
            Pipeline pipeline = Pipeline.create(options);

            // Generating scores with a randomly selected names and random amount of points
            PCollection<KV<String, Integer>> input =
                    pipeline
                            .apply(
                                    GenerateSequence.from(0)
                                            .withRate(MESSAGES_COUNT, Duration.standardSeconds(WINDOW_TIME))
                                            .withTimestampFn((Long n) -> new Instant(System.currentTimeMillis())))
                            .apply(ParDo.of(new RandomUserScoreGeneratorFn()));
            input.apply(
                    KafkaIO.<String, Integer>write()
                            .withBootstrapServers(options.getKafkaHost())
                            .withTopic(TOPIC_NAME)
                            .withKeySerializer(StringSerializer.class)
                            .withValueSerializer(IntegerSerializer.class)
                            .withProducerConfigUpdates(new HashMap<>()));

            pipeline.run().waitUntilFinish();
        }

        // A class that randomly selects a name with random amount of points
        static class RandomUserScoreGeneratorFn extends DoFn<Object, KV<String, Integer>> {
            private static final int MAX_SCORE = 100;

            @ProcessElement
            public void processElement(ProcessContext c) {
                c.output(generate());
            }

            public KV<String, Integer> generate() {
                Random random = new Random();
                String randomName = NAMES[random.nextInt(NAMES.length)];
                int randomScore = random.nextInt(MAX_SCORE) + 1;
                return KV.of(randomName, randomScore);
            }
        }
    }

    // Kafka consumer
    public static class KafkaConsumer {
        private final KafkaStreamingOptions options;

        public KafkaConsumer(KafkaStreamingOptions options) {
            this.options = options;
        }

        public void run() {
            Pipeline pipeline = Pipeline.create(options);

            // Create fixed window for the length of the game round
            Window<KV<String, String>> window =
                    Window.into(FixedWindows.of(Duration.standardSeconds(WINDOW_TIME)));

            // After the first element, the trigger waits for a [TIME_OUTPUT_AFTER_FIRST_ELEMENT], after
            // which the output of elements begins
            Trigger trigger =
                    AfterProcessingTime.pastFirstElementInPane()
                            .plusDelayOf(Duration.standardSeconds(TIME_OUTPUT_AFTER_FIRST_ELEMENT));

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

            // Write data to a MongoDB collection
            String mongoURI = "mongodb://localhost:27017"; // Replace with your MongoDB connection URI
            String databaseName = "your_database";
            String collectionName = "your_collection";

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

    static class WindowCombineFn
            extends Combine.CombineFn<KV<String, Integer>, Map<String, Integer>, Map<String, Integer>> {
        @Override
        public Map<String, Integer> createAccumulator() {
            return new HashMap<>();
        }

        @Override
        public Map<String, Integer> addInput(
                Map<String, Integer> mutableAccumulator, KV<String, Integer> input) {
            assert input != null;
            assert mutableAccumulator != null;
            mutableAccumulator.put(input.getKey(), input.getValue());
            return mutableAccumulator;
        }

        @Override
        public Map<String, Integer> mergeAccumulators(Iterable<Map<String, Integer>> accumulators) {
            Map<String, Integer> result = new HashMap<>();
            for (Map<String, Integer> acc : accumulators) {
                for (Map.Entry<String, Integer> kv : acc.entrySet()) {
                    if (result.containsKey(kv.getKey())) {
                        result.put(kv.getKey(), result.get(kv.getKey()) + kv.getValue());
                    } else {
                        result.put(kv.getKey(), kv.getValue());
                    }
                }
            }
            return result;
        }

        @Override
        public Map<String, Integer> extractOutput(Map<String, Integer> accumulator) {
            return accumulator;
        }
    }

    static class LogResults extends DoFn<Map<String, Integer>, Map<String, Integer>> {
        @ProcessElement
        public void processElement(ProcessContext c, IntervalWindow w) throws Exception {
            Map<String, Integer> map = c.element();
            if (map == null) {
                c.output(c.element());
                return;
            }

            String startTime = w.start().toString(dateTimeFormatter);
            String endTime = w.end().toString(dateTimeFormatter);

            PaneInfo.Timing timing = c.pane().getTiming();

            switch (timing) {
                case EARLY:
                    System.out.println("Live score (running sum) for the current round:");
                    break;
                case ON_TIME:
                    System.out.println("Final score for the current round:");
                    break;
                case LATE:
                    System.out.printf("Late score for the round from %s to %s:%n", startTime, endTime);
                    break;
                default:
                    throw new RuntimeException("Unknown timing value");
            }

            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                System.out.printf("%10s: %-10s%n", entry.getKey(), entry.getValue());
            }

            if (timing == PaneInfo.Timing.ON_TIME) {
                System.out.printf("======= End of round from %s to %s =======%n%n", startTime, endTime);
            } else {
                System.out.println();
            }

            c.output(c.element());
        }
    }
}


