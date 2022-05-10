package com.dyptan;

import com.mongodb.spark.MongoSpark;
import org.apache.hadoop.mapred.InvalidInputException;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.VoidFunction2;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.StructType;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.from_json;
import static org.apache.spark.sql.types.DataTypes.*;

public class StreamTransformer implements  Runnable{
    final static Logger logger = Logger.getLogger(StreamTransformer.class.getName());

    private final StructType STREAMING_RSS_SCHEMA = new StructType()
            .add("link", StringType, true)
            .add("@timestamp", TimestampType, true)
            .add("model", StringType, true)
            .add("price_usd", StringType, true)
            .add("race_km", StringType, true)
            .add("year", StringType, true)
            .add("category", StringType, true)
            .add("engine_cubic_cm", StringType, true)
            .add("message", StringType, true)
            .add("title", StringType, true)
            .add("published", TimestampType, true);

    private String KAFKA_BOOTSTRAP_SERVER;
    private String KAFKA_TOPIC;
    String MODEL_PATH;
    String MODEL_NAME;
    private SparkSession spark;
    public StreamingQuery query = null;
    public int id;

    public StreamTransformer(int Id) {
        this.id = Id;

        logger.info("Loading Stream properties.");

        InputStream sparkDefaults = getClass().getClassLoader()
                .getResourceAsStream("conf/streamer.properties");

        Properties streamingConfig = new Properties();
        try {
            streamingConfig.load(sparkDefaults);
        } catch (IOException e) {
            e.printStackTrace();
        }

//      Setting up Stream properties

        KAFKA_BOOTSTRAP_SERVER = streamingConfig.getProperty("source.kafka.bootstrap.server", "kafka:9092");
        KAFKA_TOPIC = streamingConfig.getProperty("source.kafka.topic");
        MODEL_PATH = streamingConfig.getProperty("model.path", "model");
        MODEL_NAME = streamingConfig.getProperty("model.name", "dummy");


        if (KAFKA_TOPIC == null | MODEL_PATH == null) throw new RuntimeException("configuration not found");
//      Converting Java Properties to SparkConf

        Map<String, String> scalaProps = new HashMap<>();
        scalaProps.putAll((Map)streamingConfig);

        SparkConf sparkConf = new SparkConf();
        sparkConf.setAll(JavaConversions.mapAsScalaMap(scalaProps));

        spark = SparkSession
                .builder()
                .appName("StreamingApplication")
                .config(sparkConf)
                .getOrCreate();

    }

    @Override
    public void run() {
        logger.info("Start reading data from source");
        Dataset<Row> rawKafkaStream = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVER)
                .option("subscribe", KAFKA_TOPIC)
                .option("startingOffsets", "latest")
                .load();

        Dataset<Row> parsedStream = rawKafkaStream.select(
                from_json(
                        col("value").cast("string"), STREAMING_RSS_SCHEMA)
                        .alias("unbounded_table"))
                .select("unbounded_table.*");

//      Augmenting stream data with types
        Dataset<Row> structuredStream = parsedStream.select(
                col("year").cast(DoubleType),
                col("category"),
                col("price_usd").cast(DoubleType),
                col("model"),
                col("engine_cubic_cm").cast(DoubleType),
                col("race_km").cast(DoubleType),
                col("published")
        );

        PipelineModel pipelineModel = PipelineModel.load(MODEL_PATH.concat(MODEL_NAME));

        Dataset<Row> streamPredictionsDF = pipelineModel.transform(structuredStream);

        Dataset<Row> filteredDf = streamPredictionsDF.drop("features");


            query = filteredDf.writeStream()
    //                .outputMode("append").format("console")
                    .foreachBatch(
                    (VoidFunction2<Dataset<Row>, Long>) (records, batchId) -> {
                        logger.warn(records.showString(10, 15, false));
                        MongoSpark.save(records);
                    }
            ).start();


        try {
            query.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void applyNewModel(String name) {
        MODEL_NAME = name;
    }


}
