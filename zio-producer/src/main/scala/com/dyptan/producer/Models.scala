package com.dyptan.producer

import com.ria.avro.scala.Advertisement
case class AdWithId(id: Int, advertisement: Advertisement)
object CrawlerCommands {
  case object StartProcessing
  case object StopProcessing
}