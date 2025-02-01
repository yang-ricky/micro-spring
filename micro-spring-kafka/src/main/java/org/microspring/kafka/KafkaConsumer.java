package org.microspring.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * A simple Kafka consumer wrapper that provides basic message consumption functionality.
 */
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    
    private final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;
    private volatile boolean running = true;
    
    public KafkaConsumer(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }
    
    /**
     * Subscribe to a topic and start consuming messages.
     *
     * @param topic    the topic to subscribe to
     * @param callback the callback to handle received messages
     */
    public void subscribe(String topic, MessageCallback callback) {
        consumer.subscribe(Collections.singletonList(topic));
        
        Thread consumerThread = new Thread(() -> {
            try {
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            callback.onMessage(record.key(), record.value());
                        } catch (Exception e) {
                            logger.error("Error processing message: {}", e.getMessage(), e);
                        }
                    }
                }
            } finally {
                consumer.close();
            }
        });
        
        consumerThread.start();
    }
    
    /**
     * Stop consuming messages and close the consumer.
     */
    public void close() {
        running = false;
    }
    
    /**
     * Callback interface for message handling.
     */
    public interface MessageCallback {
        void onMessage(String key, String value);
    }
} 