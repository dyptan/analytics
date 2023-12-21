package com.dyptan;

import com.dyptan.avro.Advertisement;
import com.dyptan.avro.AutoData;
import com.dyptan.avro.Geography;

public class Stubs {
    Advertisement advertisement = Advertisement.newBuilder()
            .setUSD(50000)
            .setAddDate("2023-01-01 12:00:00")
            .setSoldDate("2023-01-10 15:30:00")
            .setAutoData(
                    AutoData.newBuilder()
                            .setYear(2022)
                            .setAutoId(123456)
                            .setBodyId(1)
                            .setRaceInt(10000)
                            .setFuelId(2)
                            .setFuelNameEng("Gasoline")
                            .setGearBoxId(1)
                            .setGearboxName("Manual")
                            .setDriveId(2)
                            .setDriveName("Front-wheel drive")
                            .setCategoryId(1)
                            .setCategoryNameEng("sedan")
                            .setSubCategoryNameEng("compact")
                            .build()
            )
            .setMarkId(1)
            .setMarkNameEng("Toyota")
            .setModelId(101)
            .setModelNameEng("Corolla")
            .setLinkToView("/auto/toyota-corolla-123456")
            .setStateData(
                    Geography.newBuilder()
                            .setStateId(5)
                            .setCityId(10)
                            .setRegionNameEng("California")
                            .build()
            )
            .build();
}
