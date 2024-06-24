import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object Exporter extends App{
  val sparkConf    = new SparkConf()
  sparkConf.set("fs.s3a.endpoint", "http://localhost:9000/")
  sparkConf.set("fs.s3a.connection.establish.timeout", "3000")
  sparkConf.set("fs.s3a.path.style.access", "true")
  sparkConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider")
  sparkConf.set("fs.s3a.access.key", "ECgrJr21A3je4mEBZ2dq")
  sparkConf.set("fs.s3a.secret.key", "ffyUb3c2uhuDeqXP6NHU53uw9mXn6emG6BZ6DtSO")
  sparkConf.set("spark.mongodb.read.connection.uri", "mongodb://localhost:27017")

  val spark = SparkSession
    .builder()
    .master("local")
    .appName("S3toMongo")
    .config(sparkConf)
    .getOrCreate()

  val dataFrame = spark.read
    .format("mongodb")
    .option("database", "ria")
    .option("collection", "advertisementCopy")
    .load();

  spark.sparkContext.setLogLevel("DEBUG")
  dataFrame.write
    .format("csv")
    .option("header", true)
    .save(s"s3a://another-bucket/tenant/")
}
