import org.apache.spark.sql.SparkSession

object Exporter extends App{
    val spark = SparkSession.builder()
      .appName("exporter")
      .master("local[*]")
      //    .config("spark.ui.enabled", "false")
      //    .config("spark.mongodb.read.connection.uri", "mongodb://localhost:27017")
      .getOrCreate()
}
