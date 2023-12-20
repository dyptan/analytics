package com.dyptan.repository;

import com.dyptan.avro.Advertisement;
import com.dyptan.model.AdvertisementMongo;
import org.bson.Document;
import java.util.List;

public interface AdvertisementRepository {

    List<AdvertisementMongo> findByQuery(Document query);
}

