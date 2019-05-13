package com.dyptan.streaming.spark;

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
import scala.collection.JavaConversions;
import scala.collection.JavaConversions.*;
import scala.collection.Traversable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

enum TRAINER_SETTINGS {
    ELASTIC_TYPE,
    MODEL_PATH
}

public class ModelTrainer {
    private static final Logger logger = Logger.getLogger(ModelTrainer.class.getName());
    public SparkConf sparkConf = null;


    public static Map<String, String> SPARK_CONFIG = new HashMap<String, String>();
    public static Map<TRAINER_SETTINGS, String> TRAINER_CONFIG = new HashMap<TRAINER_SETTINGS, String>();

    {
        SPARK_CONFIG.put("es.read.field.as.array.include","tags");
        SPARK_CONFIG.put("es.read.field.as.array.include","com.dyptan.web.model");
        SPARK_CONFIG.put("es.nodes","localhost");
        SPARK_CONFIG.put("es.port","9200");
    }

    public final String ELASTIC_TYPE = "cars3/cars";
    public String MODEL_PATH = "trainedModel";

    public PipelineModel pipelineModel = null;

    public ModelTrainer set(TRAINER_SETTINGS setting, String value) {
        TRAINER_CONFIG.put(setting, value);
        return this;
    }

    public ModelTrainer() throws FileNotFoundException, IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("spark-defaults.properties");
        Properties prop = new Properties();
        prop.load(is);

        Map<String, String> scalaProps = new HashMap<>();
        scalaProps.putAll((Map)prop);

        sparkConf = new SparkConf();
        sparkConf.setAll(JavaConversions.mapAsScalaMap(scalaProps));

//        logger.warning("SparkConf content: "+Arrays.stream(sparkConf.getAll()).collect( Collectors.toList() ).toString());
//        logger.warning("Scala Prop cont: "+Arrays.asList(scalaProps));
//        logger.warning("Java prop content: "+Arrays.asList(prop).toString());

    }

    public void train() {

                SparkSession spark = SparkSession
                .builder()
                .master("local[*]")
                .appName("ReadFromElasticAndTrainModel")
                .config(sparkConf)
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        Dataset<Row> cars = spark.read().format("org.elasticsearch.spark.sql").options(SPARK_CONFIG)
                .option("inferSchema", true)
                .load(ELASTIC_TYPE);

        Dataset<Row> selected = cars.select("category", "price_usd","engine_cubic_cm","race_km", "model", "year","published");
        logger.info("Pre-transformed data sample: \n"+selected.showString(10, 10, false));
        logger.info("Rows in train data set: "+selected.count());

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
        linearRegression.setMaxIter(1000);

        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[] {
                        imputer, assembler, linearRegression
                });

        // Splitting train and evaluating data
        Dataset<Row>[] splitDF = labelDF.randomSplit(new double[] { 0.8, 0.2 });

        Dataset<Row> trainDF = splitDF[0];
        Dataset<Row> evaluationDF = splitDF[1];

        pipelineModel = pipeline.fit(trainDF);

        // Evaluation itself
        Dataset<Row> predictionsDF = pipelineModel.transform(evaluationDF);

//        predictionsDF.show(false);

        Dataset<Row> forEvaluationDF = predictionsDF.select("label",
                "prediction");

        RegressionEvaluator evaluteR2 = new RegressionEvaluator().setMetricName("r2");
        RegressionEvaluator evaluteRMSE = new RegressionEvaluator().setMetricName("rmse");

        double r2 = evaluteR2.evaluate(forEvaluationDF);
        double rmse = evaluteRMSE.evaluate(forEvaluationDF);

        logger.warning("---------------------------");
        logger.warning("R2 =" + r2);
        logger.warning("RMSE =" + rmse);
        logger.warning("---------------------------");

        spark.cloneSession();
    }

    public void save(){
        //Saving com.dyptan.web.model to disk
        try {
            pipelineModel.write().overwrite().save(MODEL_PATH);
            logger.warning("Model successfully saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
