package com.dyptan;


class Processor {

    public static void main(String[] args) {

        KafkaAdvertisementConsumer kafkaConsumer = new KafkaAdvertisementConsumer();
        kafkaConsumer.run();
    }

    public void consume(String[] args) {
        KafkaAdvertisementConsumer kafkaConsumer = new KafkaAdvertisementConsumer();
        kafkaConsumer.run();
    }


}


