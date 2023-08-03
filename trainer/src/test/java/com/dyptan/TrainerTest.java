package com.dyptan;
import com.dyptan.Trainer;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.spark.sql.SparkSession;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.spark.sql.types.StringType;
public class TrainerTest {

    @Ignore
    @Test
    public void runModelTrainer() throws IOException {
        Trainer modelTrainer = new Trainer();
        modelTrainer.train();
        modelTrainer.save("/mnt/model/trainedModel/");
        modelTrainer.spark.stop();
    }

    @Test
    public void connectPubSubLite() throws TimeoutException, StreamingQueryException {

        Long project_number = 952438425179L;
        String location = "europe-west9-a";
        String subscription_id = "crawler";

        SparkSession spark = SparkSession.builder().appName("read-app").master("local[*]").getOrCreate();

                Dataset<Row> sdf = spark.readStream().format("pubsublite")
                        .option(
                                "pubsublite.subscription",
                                "projects/952438425179/locations/europe-west9-a/subscriptions/crawler"
                                )
                        .option(
                                "gcp.credentials.key",
                                "ewogICJjbGllbnRfaWQiOiAiNzY0MDg2MDUxODUwLTZxcjRwNmdwaTZobjUwNnB0OGVqdXE4M2Rp" +
                                "MzQxaHVyLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwKICAiY2xpZW50X3NlY3JldCI6ICJk" +
                                "LUZMOTVRMTlxN01RbUZwZDdoSEQwVHkiLAogICJxdW90YV9wcm9qZWN0X2lkIjogInN5bnRoZXRp" +
                                "Yy12ZXJ2ZS0zOTA5MTMiLAogICJyZWZyZXNoX3Rva2VuIjogIjEvLzBjYllEaHY1N0RzelRDZ1lJ" +
                                "QVJBQUdBd1NOd0YtTDlJckQ4X3ozZEtERGFCQW5pUVNQaG9lZU0zYnlkaVhrVWlkZ1NKMnhGT2VJ" +
                                "cW1ROEtjNkswemphSlVCUk5yOXpQZEtBRWMiLAogICJ0eXBlIjogImF1dGhvcml6ZWRfdXNlciIK" +
                                "fQ=="
                                )
                        .load();


//        sdf = sdf.withColumn("data", sdf.col("data").cast("String"));

                sdf.writeStream().format("console")
                        .outputMode("append")
                        .trigger(Trigger.ProcessingTime(1, TimeUnit.SECONDS))
                        .start().awaitTermination(120);

        spark.stop();
    }

}
