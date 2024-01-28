package com.dyptan

import com.dyptan.avro.Advertisement

case class L2(ids: Array[String], count: Int)

case class L1(search_result: L2)

case class searchRoot(result: L1)
//
//case class Geo(stateId: Int, cityId: Int, regionNameEng: String)
//
//case class Dtls(year: Int, autoId: Int, bodyId: Int, raceInt: Int, fuelId: Int,
//                fuelNameEng: Option[String], gearBoxId: Option[Int], gearboxName: Option[String], driveId: Option[Int], driveName: String,
//                categoryId: Int, categoryNameEng: Option[String], subCategoryNameEng: Option[String])
//
//case class Ad(USD: Int, addDate: String, soldDate: String, autoData: Dtls,
//              markId: Int, markNameEng: String, modelId: Int, modelNameEng: String, linkToView: String,
//              stateData: Geo)
case class AdWithId(id: Int, advertisement: Advertisement)