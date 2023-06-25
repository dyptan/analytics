
package com.dyptan;

import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.Imputer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import scala.collection.immutable.Iterable;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;


public class Trainer {
    private static final Logger logger = Logger.getLogger(Trainer.class.getName());

    private PipelineModel pipelineModel = null;
    public SparkSession spark = null;
    private Dataset<Row>[] splitDF = null;
    Properties trainerConfig = new Properties();

    public Trainer() throws IOException {

        // We need this to find the config file from env var when running from IDE
        String configFile = Optional.ofNullable(System.getenv("CONF_FILE")).orElse("conf/trainer.properties");
        logger.info("Loading config file from "+configFile);

        try {
            trainerConfig.load(Files.newInputStream(Paths.get(configFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> scalaProps = new HashMap<>((Map) trainerConfig);

        SparkConf sparkConf = new SparkConf();
        scalaProps.forEach(sparkConf::set);

        spark = SparkSession
                .builder()
                .appName("ReadFromElasticAndTrainModel")
                .config(sparkConf)
                .getOrCreate();

        logger.info("Spark Started..");
    }

    // override default ES config
    public void setSource(URL source, int limit, int iterations) {
        Properties properties = new Properties();
        properties.setProperty("es.nodes", source.getHost());
        properties.setProperty("es.port", String.valueOf(source.getPort()));
        properties.setProperty("es.resource", source.getPath());
        //TODO duplicate properties in config file
        properties.setProperty("ml.source.limit", String.valueOf(limit));
        properties.setProperty("ml.iterations", String.valueOf(iterations));
        //Override properties from config file
    }

    public void train() {
        logger.info("Training Started..");

        Dataset<Row> cars = spark
                .read()
                .format("mongodb")
                .option("database", "<example-database>")
                .option("collection", "<example-collection>").load();

        logger.info("Source schema: " + cars.schema().treeString());

        Dataset<Row> selected = cars.select(
                "category",
                "price_usd",
                "engine_cubic_cm",
                "race_km",
                "model",
                "year",
                "published");

        logger.info("Pre-transformed data sample: \n"+selected.showString(10, 10, false));
        logger.info("Train dataset is limited to " + selected.count() + " rows");

        Dataset<Row> labelDF = selected.withColumnRenamed("price_usd", "label");

        Imputer imputer = new Imputer()
                // .setMissingValue(1.0d)
                .setInputCols(new String[] { "engine_cubic_cm", "race_km","year" })
                .setOutputCols(new String[] { "engine_cubic_cm", "race_km","year" });

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[] { "engine_cubic_cm","race_km","year" })
                .setOutputCol("features");

        // Choosing a Model
        LinearRegression linearRegression = new LinearRegression();

        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[] {
                        imputer, assembler, linearRegression
                });

        // Splitting train and evaluating data

        splitDF = labelDF.randomSplit(new double[] { 0.8, 0.2 });

        Dataset<Row> trainDF = splitDF[0];

        pipelineModel = pipeline.fit(trainDF);

    }

    public String evaluate(){

        Dataset<Row> evaluationDF = splitDF[1];

        Dataset<Row> predictionsDF = pipelineModel.transform(evaluationDF);

        logger.info("Transformed data with predictions: \n"+predictionsDF.showString(10, 10, false));

        Dataset<Row> forEvaluationDF = predictionsDF.select("label", "prediction");

        RegressionEvaluator evaluateR2 = new RegressionEvaluator().setMetricName("r2");
        RegressionEvaluator evaluateRMSE = new RegressionEvaluator().setMetricName("rmse");

        double r2 = evaluateR2.evaluate(forEvaluationDF);
        double rmse = evaluateRMSE.evaluate(forEvaluationDF);

        return String.format("R2 = %s \n RMSE = %s", r2, rmse);
    }

    public void save(String name) throws IOException {
       String MODEL_PATH = trainerConfig.getProperty("model.path");
        logger.info("Saving to "+MODEL_PATH);
            pipelineModel.write().overwrite().save(MODEL_PATH+name);

        logger.info("Model "+name+" successfully saved to "+MODEL_PATH);
    }
}
