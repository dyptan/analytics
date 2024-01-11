package com.dyptan

case class L2(ids: Array[String], count: Int)

case class L1(search_result: L2)

case class searchRoot(result: L1)

case class Geo(stateId: Int, cityId: Int, regionNameEng: String)

case class Dtls(year: Int, autoId: Int, bodyId: Int, raceInt: Int, fuelId: Int,
                fuelNameEng: String, gearBoxId: Int, gearboxName: String, driveId: Int, driveName: String,
                categoryId: Int, categoryNameEng: String, subCategoryNameEng: String)

case class Ad(USD: Int, addDate: String, soldDate: String, autoData: Dtls,
              markId: Int, markNameEng: String, modelId: Int, modelNameEng: String, linkToView: String,
              stateData: Geo)
case class AdWithId(id: Int, advertisement: Ad)