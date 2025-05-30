package com.dyptan.exporter

import com.dyptan.generated.exporter.definitions.{ProcessRequest, ProcessResponse}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

class ExportService {
  val sparkConf    = new SparkConf()
  sparkConf.set("fs.s3a.endpoint", "http://minio:9000/")
  sparkConf.set("fs.s3a.connection.establish.timeout", "3000")
  sparkConf.set("fs.s3a.path.style.access", "true")
  sparkConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider")
  sparkConf.set("fs.s3a.access.key", "myuserserviceaccount")
  sparkConf.set("fs.s3a.secret.key", "myuserserviceaccountpassword")
  sparkConf.set("spark.mongodb.read.connection.uri", "mongodb://mongo:27017")

  val spark = SparkSession
    .builder()
    .master("local")
    .appName("S3toMongo")
    .config(sparkConf)
    .getOrCreate()


  def exportToS3(body: ProcessRequest) = {
    try {
      val dataFrame = spark.read
        .format("mongodb")
        .option("database", "ria")
        .option("collection", body.mongoCollection)
        .load();

      dataFrame.write
        .format(body.outputFormat.value)
        .option("header", true)
        .save(s"s3a://export-bucket/${body.s3Bucket}/")

      ProcessResponse(Some(ProcessResponse.Status.Success), Some("congrats!"), None)
    } catch {
      case e: Throwable =>  ProcessResponse(Some(ProcessResponse.Status.Error), Some("somethign went wrong!"), Some(e.getMessage))
    }

  }
}
