package com.maroctelecom.taskmaster.service;

import com.maroctelecom.common.config.KafkaConfig;
import com.maroctelecom.common.dto.PartitionTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service d'envoi des tâches de partition via Kafka
 * Gère l'envoi asynchrone des tâches vers les workers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPartitionSender {
    
    private final KafkaTemplate<String, PartitionTaskDTO> kafkaTemplate;
    private final KafkaConfig kafkaConfig;
    
    /**
     * Envoie une tâche de partition vers Kafka
     * 
     * @param partitionTask La tâche à envoyer
     * @return CompletableFuture pour le suivi asynchrone
     */
    public CompletableFuture<SendResult<String, PartitionTaskDTO>> sendPartitionTask(PartitionTaskDTO partitionTask) {
        String topic = kafkaConfig.getTopics().getContractPartitions();
        String key = generatePartitionKey(partitionTask);
        
        log.debug("Envoi de la tâche {} vers le topic {} avec la clé {}", 
                partitionTask.getTaskId(), topic, key);
        
        CompletableFuture<SendResult<String, PartitionTaskDTO>> future = 
            kafkaTemplate.send(topic, key, partitionTask);
            
        // Ajout de callbacks pour le logging
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Échec de l'envoi de la tâche {} vers Kafka: {}", 
                        partitionTask.getTaskId(), throwable.getMessage());
            } else {
                log.info("Tâche {} envoyée avec succès vers la partition {} du topic {}", 
                        partitionTask.getTaskId(), 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().topic());
            }
        });
        
        return future;
    }
    
    /**
     * Envoie une tâche de partition de manière synchrone
     * 
     * @param partitionTask La tâche à envoyer
     * @throws RuntimeException si l'envoi échoue
     */
    public void sendPartitionTaskSync(PartitionTaskDTO partitionTask) {
        try {
            SendResult<String, PartitionTaskDTO> result = sendPartitionTask(partitionTask).get();
            log.info("Tâche {} envoyée de manière synchrone vers la partition {}", 
                    partitionTask.getTaskId(), 
                    result.getRecordMetadata().partition());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi synchrone de la tâche {}: {}", 
                    partitionTask.getTaskId(), e.getMessage());
            throw new RuntimeException("Échec de l'envoi de la tâche vers Kafka", e);
        }
    }
    
    /**
     * Génère une clé de partition pour assurer une distribution équilibrée
     * La clé est basée sur l'ID de partition pour garantir l'ordre
     */
    private String generatePartitionKey(PartitionTaskDTO partitionTask) {
        // Utilisation de l'ID de partition comme base pour la clé
        // Cela garantit que les tâches sont distribuées de manière équilibrée
        return String.format("%s-p%d", partitionTask.getJobId(), partitionTask.getPartitionId());
    }
    
    /**
     * Vérifie la santé de la connexion Kafka
     * 
     * @return true si la connexion est opérationnelle
     */
    public boolean isKafkaHealthy() {
        try {
            // Tentative d'envoi d'un message de test
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor(
                kafkaConfig.getTopics().getContractPartitions());
            return true;
        } catch (Exception e) {
            log.warn("Problème de connectivité Kafka détecté: {}", e.getMessage());
            return false;
        }
    }
}