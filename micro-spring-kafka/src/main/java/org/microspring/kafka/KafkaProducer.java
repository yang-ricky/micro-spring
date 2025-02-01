package org.microspring.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * A simple Kafka producer wrapper that provides basic message sending functionality.
 */
public class KafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    
    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;
    
    public KafkaProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }
    
    /**
     * Send a message to a topic without a key.
     *
     * @param topic   the topic to send the message to
     * @param message the message to send
     * @return a Future containing the RecordMetadata
     */
    public Future<RecordMetadata> send(String topic, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
        return producer.send(record);
    }
    
    /**
     * Send a message to a topic with a key.
     *
     * @param topic   the topic to send the message to
     * @param key     the key for the message (used for partitioning)
     * @param message the message to send
     * @return a Future containing the RecordMetadata
     */
    public Future<RecordMetadata> send(String topic, String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        return producer.send(record);
    }
    
    /**
     * Close the producer.
     */
    public void close() {
        producer.close();
    }
} 