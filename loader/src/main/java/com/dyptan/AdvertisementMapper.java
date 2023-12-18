package com.dyptan;

import com.dyptan.avro.Advertisement;
import com.dyptan.avro.AutoData;
import com.dyptan.avro.Geography;
import com.dyptan.gen.proto.AdvertisementMessage;
import com.dyptan.gen.proto.Details;
import com.dyptan.gen.proto.Geo;
import com.google.protobuf.Timestamp;
import org.bson.Document;

import java.time.Instant;

public class AdvertisementMapper {

    public static AdvertisementMessage mapToAdvertisementMessage(Document advertisement) {
        Document autoData = (Document) advertisement.get("autoData");
        Details details = Details.newBuilder()
                .setYear(autoData.getInteger("year"))
                .setAutoId(autoData.getInteger("autoId"))
                .setBodyId(autoData.getInteger("bodyId"))
                .setRaceInt(autoData.getInteger("raceInt"))
                .setFuelId(autoData.getInteger("fuelId"))
                .setGearBoxId(autoData.getInteger("gearBoxId"))
                .setGearboxName(autoData.getString("gearboxName"))
                .setDriveId(autoData.getInteger("driveId"))
                .setDriveName(autoData.getString("driveName"))
                .setCategoryId(autoData.getInteger("categoryId"))
                .setCategoryName(autoData.getString("categoryNameEng"))
                .build();

        // Map the Geography field
        Document geo = (Document) advertisement.get("stateData");
        Geo stateData = Geo.newBuilder()
                .setStateId(geo.getInteger("stateId"))
                .setCityId(geo.getInteger("cityId"))
                .setRegionName(geo.getString("regionNameEng"))
                .build();

        // Map the AdvertisementMessage
        AdvertisementMessage advertisementMessage = AdvertisementMessage.newBuilder()
                .setPriceUSD(advertisement.getInteger("USD"))
                .setAddDate(advertisement.getString("addDate"))
                .setSoldDate(advertisement.getString("soldDate"))
                .setAutoData(details)
                .setBrandId(advertisement.getInteger("markId"))
                .setBrandName(advertisement.getString("markNameEng"))
                .setModelId(advertisement.getInteger("modelId"))
                .setModelName(advertisement.getString("modelNameEng"))
                .setLinkToView(advertisement.getString("linkToView"))
                .setStateData(stateData)
                .build();


        return advertisementMessage;
    }
}

