package com.maroctelecom.worker.application;

import com.maroctelecom.common.config.KafkaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application principale du Worker Node
 * Responsable du traitement des tâches de partition reçues via Kafka
 */
@SpringBootApplication(scanBasePackages = {
    "com.maroctelecom.worker", 
    "com.maroctelecom.common"
})
@EnableKafka
@EnableAsync
@EnableConfigurationProperties(KafkaConfig.class)
public class WorkerNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerNodeApplication.class, args);
    }
}