package com.dyptan.repository;
import com.dyptan.avro.Advertisement;
import com.dyptan.model.AdvertisementMongo;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdvertisementRepositoryImpl implements AdvertisementRepository {

    private final MongoTemplate mongoTemplate;

    public AdvertisementRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<AdvertisementMongo> findByQuery(Document query) {
        Query mongoQuery = new BasicQuery(query);
        List<AdvertisementMongo> advertisementMongos = mongoTemplate.find(mongoQuery, AdvertisementMongo.class);
        return advertisementMongos;
    }
}
