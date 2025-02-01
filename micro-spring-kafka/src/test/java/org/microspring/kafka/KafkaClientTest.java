package org.microspring.kafka;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import org.apache.curator.test.TestingServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.utils.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class KafkaClientTest {
    private static final String TOPIC = "test-topic";
    private static final String GROUP_ID = "test-group";
    
    private KafkaProducer producer;
    private KafkaConsumer consumer;
    private KafkaServer kafkaServer;
    private TestingServer zkServer;
    private String bootstrapServers;
    private TemporaryFolder tempFolder;
    
    @BeforeClass
    public static void setupClass() {
        // 设置Kafka相关日志级别为WARN
        ((Logger) LoggerFactory.getLogger("kafka")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.apache.kafka")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.apache.zookeeper")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("state.change.logger")).setLevel(Level.WARN);
    }
    
    private int getRandomPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
    
    @Before
    public void setUp() throws Exception {
        // Start ZooKeeper
        zkServer = new TestingServer(true);  // true means start immediately
        String zkConnect = zkServer.getConnectString();
        
        // Create temp directory for Kafka logs
        tempFolder = new TemporaryFolder();
        tempFolder.create();
        File logDir = tempFolder.newFolder("kafka-logs");
        
        // Get random port for Kafka
        int kafkaPort = getRandomPort();
        bootstrapServers = "localhost:" + kafkaPort;
        
        // Configure and start Kafka
        Properties props = new Properties();
        props.put("listeners", "PLAINTEXT://" + bootstrapServers);
        props.put("log.dirs", logDir.getAbsolutePath());
        props.put("offsets.topic.replication.factor", "1");
        props.put("transaction.state.log.replication.factor", "1");
        props.put("transaction.state.log.min.isr", "1");
        props.put("zookeeper.connect", zkConnect);
        props.put("num.partitions", "1");
        props.put("auto.create.topics.enable", "true");
        props.put("zookeeper.session.timeout.ms", "30000");
        props.put("zookeeper.connection.timeout.ms", "30000");
        
        KafkaConfig config = new KafkaConfig(props);
        kafkaServer = new KafkaServer(config, Time.SYSTEM, scala.Option.empty(), true);
        kafkaServer.startup();
        
        // Wait for Kafka to be ready
        Thread.sleep(1000);
        
        // Create topic
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);
        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            adminClient.createTopics(Collections.singleton(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        }
        
        // Initialize producer and consumer
        producer = new KafkaProducer(bootstrapServers);
        consumer = new KafkaConsumer(bootstrapServers, GROUP_ID);
    }
    
    @After
    public void tearDown() {
        try {
            // First close the clients
            if (producer != null) {
                producer.close();
            }
            if (consumer != null) {
                consumer.close();
            }
            
            // Wait a bit for clients to close
            Thread.sleep(1000);
            
            // Then stop Kafka
            if (kafkaServer != null) {
                kafkaServer.shutdown();
                kafkaServer.awaitShutdown();
            }
            
            // Wait a bit for Kafka to shut down
            Thread.sleep(1000);
            
            // Finally stop ZooKeeper
            if (zkServer != null) {
                zkServer.stop();
            }
            
            // Clean up temp files
            if (tempFolder != null) {
                tempFolder.delete();
            }
        } catch (Exception e) {
            // Log but don't throw as this is cleanup code
            System.err.println("Error during teardown: " + e.getMessage());
        }
    }
    
    @Test
    public void testSendAndReceiveMessage() throws Exception {
        String testMessage = "Hello, Kafka!";
        CountDownLatch latch = new CountDownLatch(1);
        final String[] receivedMessage = {null};
        
        // Start consumer
        consumer.subscribe(TOPIC, (key, value) -> {
            receivedMessage[0] = value;
            latch.countDown();
        });
        
        // Wait a bit for consumer to be ready
        Thread.sleep(1000);
        
        // Send message
        Future<RecordMetadata> future = producer.send(TOPIC, testMessage);
        future.get(5, TimeUnit.SECONDS);
        
        // Wait for message to be received
        assertTrue("Message not received within timeout", latch.await(10, TimeUnit.SECONDS));
        assertEquals("Received message doesn't match sent message", 
        testMessage, receivedMessage[0]);
    }
    
    @Test
    public void testSendAndReceiveMessageWithKey() throws Exception {
        String testKey = "test-key";
        String testMessage = "Hello, Kafka with key!";
        CountDownLatch latch = new CountDownLatch(1);
        final String[] receivedKey = {null};
        final String[] receivedMessage = {null};
        
        // Start consumer
        consumer.subscribe(TOPIC, (key, value) -> {
            receivedKey[0] = key;
            receivedMessage[0] = value;
            latch.countDown();
        });
        
        // Wait a bit for consumer to be ready
        Thread.sleep(1000);
        
        // Send message with key
        Future<RecordMetadata> future = producer.send(TOPIC, testKey, testMessage);
        future.get(5, TimeUnit.SECONDS);
        
        // Wait for message to be received
        assertTrue("Message not received within timeout", latch.await(10, TimeUnit.SECONDS));
        assertEquals("Received key doesn't match sent key", testKey, receivedKey[0]);
        assertEquals("Received message doesn't match sent message", 
                    testMessage, receivedMessage[0]);
    }
} 