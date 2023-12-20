package com.dyptan;

import com.dyptan.gen.proto.AdvertisementMessage;
import com.dyptan.gen.proto.AutoData;
import com.dyptan.gen.proto.StateData;
import org.bson.Document;

public class AdvertisementMapper {

    public static AdvertisementMessage mapToAdvertisementMessage(Document advertisement) {
        Document autoData = (Document) advertisement.get("autoData");
        AutoData autoDataNew = AutoData.newBuilder()
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

        // Map the StateDatagraphy field
        Document stateData = (Document) advertisement.get("stateData");
        StateData stateDataNew = StateData.newBuilder()
                .setStateId(stateData.getInteger("stateId"))
                .setCityId(stateData.getInteger("cityId"))
                .setRegionName(stateData.getString("regionNameEng"))
                .build();

        // Map the AdvertisementMessage
        AdvertisementMessage advertisementMessage = AdvertisementMessage.newBuilder()
                .setUSD(advertisement.getInteger("USD"))
                .setAddDate(advertisement.getString("addDate"))
                .setSoldDate(advertisement.getString("soldDate"))
                .setAutoData(autoDataNew)
                .setBrandId(advertisement.getInteger("markId"))
                .setBrandName(advertisement.getString("markNameEng"))
                .setModelId(advertisement.getInteger("modelId"))
                .setModelName(advertisement.getString("modelNameEng"))
                .setLinkToView(advertisement.getString("linkToView"))
                .setStateData(stateDataNew)
                .build();


        return advertisementMessage;
    }
}

