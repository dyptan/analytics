package com.dyptan.crawler

import zio.akka.cluster.Cluster
import sttp.client3._
import zio._
object Main extends ZIOAppDefault{
  def test(args: Array[String]): Unit = {
    val backend = HttpClientSyncBackend()
    val url = uri"https://developers.ria.com/auto/search?api_key=KoPvKSBRd5YTGGrePubrcokuqONxzHgFrBW8KHrl&category_id=1&bodystyle%5B0%5D=3&bodystyle%5B4%5D=2&marka_id%5B0%5D=79&model_id%5B0%5D=0&s_yers%5B0%5D=2010&po_yers%5B0%5D=2017&marka_id%5B1%5D=84&model_id%5B1%5D=0&s_yers%5B1%5D=2012&po_yers%5B1%5D=2016&brandOrigin%5B0%5D=276&brandOrigin%5B1%5D=392&price_ot=1000&price_do=60000&currency=1&auctionPossible=1&state%5B0%5D=1&city%5B0%5D=0&state%5B1%5D=2&city%5B1%5D=0&state%5B2%5D=10&city%5B2%5D=0&abroad=2&custom=1&auto_options%5B477%5D=477&type%5B0%5D=1&type%5B1%5D=2&type%5B3%5D=4&type%5B7%5D=8&gearbox%5B0%5D=1&gearbox%5B1%5D=2&gearbox%5B2%5D=3&engineVolumeFrom=1.4&engineVolumeTo=3.2&powerFrom=90&powerTo=250&power_name=1&countpage=50&with_photo=1"
    val response = basicRequest
      .get(url).send(backend)
    println(response.body)
  }

  def run =
  for {
          _ <- Console.print("Please enter your name: ")
            name <- Console.readLine
          _ <- Console.printLine(s"Hello, $name!")
        } yield ()
}
