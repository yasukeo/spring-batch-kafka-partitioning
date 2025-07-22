package com.maroctelecom.taskmaster.application;

import com.maroctelecom.common.config.KafkaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale du Task Master
 * Responsable de la coordination et du partitioning des tâches de traitement
 */
@SpringBootApplication(scanBasePackages = {
    "com.maroctelecom.taskmaster", 
    "com.maroctelecom.common"
})
@EnableBatchProcessing
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(KafkaConfig.class)
public class TaskMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskMasterApplication.class, args);
    }
}