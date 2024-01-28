package com.dyptan;

import com.ria.avro.Advertisement;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ProcessorSpec {

    static Logger logger = LoggerFactory.getLogger("ProcessorSpec");

    public static void main(String[] args) {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "your-consumer-group-id");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<Integer, Advertisement> consumer = new KafkaConsumer(props);

        String topic = "ria";
        consumer.subscribe(Arrays.asList(topic));

        while (true) {
            consumer.seekToBeginning(consumer.assignment());
            ConsumerRecords<Integer, Advertisement> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<Integer, Advertisement> record : records) {
                logger.error(String.valueOf(record.value()));
                logger.error(String.valueOf(record.key()));
            }
            consumer.commitSync();
        }
    }
}


