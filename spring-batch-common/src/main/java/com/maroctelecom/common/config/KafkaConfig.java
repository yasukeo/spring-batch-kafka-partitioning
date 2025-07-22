package com.maroctelecom.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration Kafka partagée pour tous les modules
 * Centralise tous les paramètres Kafka nécessaires au bon fonctionnement du système
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaConfig {
    
    /**
     * Configuration des brokers Kafka
     */
    private Broker broker = new Broker();
    
    /**
     * Configuration des topics
     */
    private Topics topics = new Topics();
    
    /**
     * Configuration des groupes de consommateurs
     */
    private ConsumerGroups consumerGroups = new ConsumerGroups();
    
    /**
     * Configuration du producer
     */
    private Producer producer = new Producer();
    
    /**
     * Configuration du consumer
     */
    private Consumer consumer = new Consumer();
    
    /**
     * Configuration des retry policies
     */
    private Retry retry = new Retry();
    
    @Data
    public static class Broker {
        private String servers = "localhost:9092";
        private Security security = new Security();
        
        @Data
        public static class Security {
            private String protocol = "PLAINTEXT";
            private String mechanism = "PLAIN";
            private String username;
            private String password;
            private String truststore;
            private String truststorePassword;
            private String keystore;
            private String keystorePassword;
        }
    }
    
    @Data
    public static class Topics {
        private String contractPartitions = "contract-partitions";
        private String contractResults = "contract-results";
        private String deadLetter = "contract-dead-letter";
        private String monitoring = "contract-monitoring";
        
        private TopicConfig contractPartitionsConfig = new TopicConfig(8, 3, 604800000L); // 8 partitions, 3 replicas, 7 days retention
        private TopicConfig contractResultsConfig = new TopicConfig(4, 3, 604800000L);   // 4 partitions, 3 replicas, 7 days retention
        private TopicConfig deadLetterConfig = new TopicConfig(1, 3, 2592000000L);        // 1 partition, 3 replicas, 30 days retention
        private TopicConfig monitoringConfig = new TopicConfig(1, 2, 86400000L);          // 1 partition, 2 replicas, 1 day retention
        
        @Data
        public static class TopicConfig {
            private int partitions;
            private int replicationFactor;
            private long retentionMs;
            private Map<String, String> configs;
            
            public TopicConfig() {}
            
            public TopicConfig(int partitions, int replicationFactor, long retentionMs) {
                this.partitions = partitions;
                this.replicationFactor = replicationFactor;
                this.retentionMs = retentionMs;
            }
        }
    }
    
    @Data
    public static class ConsumerGroups {
        private String taskMaster = "task-master-group";
        private String worker = "worker-group";
        private String monitoring = "monitoring-group";
        private String deadLetter = "dead-letter-group";
    }
    
    @Data
    public static class Producer {
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "org.springframework.kafka.support.serializer.JsonSerializer";
        private String acks = "all";
        private int retries = 3;
        private int batchSize = 16384;
        private int lingerMs = 5;
        private long bufferMemory = 33554432L;
        private String compressionType = "gzip";
        private int maxInFlightRequestsPerConnection = 1;
        private boolean enableIdempotence = true;
        private int requestTimeoutMs = 30000;
        private int deliveryTimeoutMs = 120000;
        private Map<String, Object> additionalProperties;
    }
    
    @Data
    public static class Consumer {
        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
        private String valueDeserializer = "org.springframework.kafka.support.serializer.JsonDeserializer";
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private int sessionTimeoutMs = 30000;
        private int heartbeatIntervalMs = 10000;
        private int maxPollRecords = 100;
        private long maxPollIntervalMs = 300000L;
        private int fetchMinBytes = 1;
        private int fetchMaxWaitMs = 500;
        private String isolationLevel = "read_committed";
        private boolean allowAutoCreateTopics = false;
        private Map<String, Object> additionalProperties;
        
        // Configuration spécifique pour la désérialisation JSON
        private JsonDeserializer jsonDeserializer = new JsonDeserializer();
        
        @Data
        public static class JsonDeserializer {
            private boolean trustedPackages = true;
            private String typeMapping = "partitionTask:com.maroctelecom.common.dto.PartitionTaskDTO," +
                                        "taskResult:com.maroctelecom.common.dto.TaskResultDTO";
            private boolean addTypeHeaders = true;
            private boolean removeTypeHeaders = false;
            private boolean useTypeMapperForKey = false;
        }
    }
    
    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long initialInterval = 1000L;
        private double multiplier = 2.0;
        private long maxInterval = 30000L;
        private boolean randomizationFactor = true;
        
        // Configuration spécifique pour les erreurs récupérables
        private boolean retryableExceptions = true;
        private String[] retryableExceptionClasses = {
            "org.springframework.kafka.KafkaException",
            "org.apache.kafka.common.errors.RetriableException",
            "java.net.SocketTimeoutException",
            "java.io.IOException"
        };
        
        // Configuration pour les dead letter queues
        private boolean enableDeadLetter = true;
        private String deadLetterSuffix = ".DLT";
        private boolean includeHeaders = true;
        private boolean includeException = true;
    }
    
    /**
     * Configuration pour la surveillance et le monitoring
     */
    @Data
    public static class Monitoring {
        private boolean enabled = true;
        private long metricsIntervalMs = 30000L;
        private boolean enableLagMonitoring = true;
        private boolean enableThroughputMonitoring = true;
        private long alertThresholdLag = 1000L;
        private double alertThresholdThroughput = 100.0;
    }
    
    /**
     * Configuration pour la santé des connexions
     */
    @Data
    public static class Health {
        private boolean enabled = true;
        private long timeoutMs = 5000L;
        private int retryAttempts = 3;
        private long retryDelayMs = 1000L;
    }
    
    /**
     * Validation de la configuration
     */
    public void validate() {
        if (broker.servers == null || broker.servers.trim().isEmpty()) {
            throw new IllegalArgumentException("La configuration des serveurs Kafka ne peut pas être vide");
        }
        
        if (topics.contractPartitions == null || topics.contractPartitions.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du topic contract-partitions ne peut pas être vide");
        }
        
        if (topics.contractResults == null || topics.contractResults.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du topic contract-results ne peut pas être vide");
        }
        
        if (consumerGroups.taskMaster == null || consumerGroups.taskMaster.trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe de consommateurs task-master ne peut pas être vide");
        }
        
        if (consumerGroups.worker == null || consumerGroups.worker.trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe de consommateurs worker ne peut pas être vide");
        }
    }
}