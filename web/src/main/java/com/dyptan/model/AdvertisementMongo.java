package com.dyptan.model;

import com.dyptan.avro.Advertisement;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "autos")
public class AdvertisementMongo extends Advertisement {
    @Id
    private String id;
}
