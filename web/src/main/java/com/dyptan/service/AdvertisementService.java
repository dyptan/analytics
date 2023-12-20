package com.dyptan.service;

import com.dyptan.avro.Advertisement;
import com.dyptan.model.AdvertisementMongo;
import com.dyptan.repository.AdvertisementRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdvertisementService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    public List<AdvertisementMongo> findAdvertisementsByQuery(Document query) {
        return advertisementRepository.findByQuery(query);
    }
}

