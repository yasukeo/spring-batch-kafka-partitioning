package com.maroctelecom.worker.service;

import com.maroctelecom.common.config.KafkaConfig;
import com.maroctelecom.common.dto.TaskResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service d'envoi des résultats de traitement vers Kafka
 * Gère l'envoi asynchrone des résultats vers le task master
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultSender {
    
    private final KafkaTemplate<String, TaskResultDTO> kafkaTemplate;
    private final KafkaConfig kafkaConfig;
    
    /**
     * Envoie un résultat de traitement vers Kafka
     * 
     * @param taskResult Le résultat à envoyer
     * @return CompletableFuture pour le suivi asynchrone
     */
    public CompletableFuture<SendResult<String, TaskResultDTO>> sendResult(TaskResultDTO taskResult) {
        String topic = kafkaConfig.getTopics().getContractResults();
        String key = generateResultKey(taskResult);
        
        log.debug("Envoi du résultat de la tâche {} vers le topic {} avec la clé {}", 
                taskResult.getTaskId(), topic, key);
        
        CompletableFuture<SendResult<String, TaskResultDTO>> future = 
            kafkaTemplate.send(topic, key, taskResult);
            
        // Ajout de callbacks pour le logging
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Échec de l'envoi du résultat de la tâche {} vers Kafka: {}", 
                        taskResult.getTaskId(), throwable.getMessage());
            } else {
                log.info("Résultat de la tâche {} envoyé avec succès vers la partition {} du topic {}", 
                        taskResult.getTaskId(), 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().topic());
            }
        });
        
        return future;
    }
    
    /**
     * Envoie un résultat de traitement de manière synchrone
     * 
     * @param taskResult Le résultat à envoyer
     * @throws RuntimeException si l'envoi échoue
     */
    public void sendResultSync(TaskResultDTO taskResult) {
        try {
            SendResult<String, TaskResultDTO> result = sendResult(taskResult).get();
            log.info("Résultat de la tâche {} envoyé de manière synchrone vers la partition {}", 
                    taskResult.getTaskId(), 
                    result.getRecordMetadata().partition());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi synchrone du résultat de la tâche {}: {}", 
                    taskResult.getTaskId(), e.getMessage());
            throw new RuntimeException("Échec de l'envoi du résultat vers Kafka", e);
        }
    }
    
    /**
     * Génère une clé pour le résultat basée sur l'ID de la tâche
     * Assure une distribution équilibrée des résultats
     */
    private String generateResultKey(TaskResultDTO taskResult) {
        // Utilisation de l'ID de la tâche comme clé pour garantir l'ordre
        // et assurer que les résultats d'une même tâche arrivent sur la même partition
        return String.format("result-%s", taskResult.getTaskId());
    }
    
    /**
     * Vérifie la santé de la connexion Kafka pour l'envoi de résultats
     * 
     * @return true si la connexion est opérationnelle
     */
    public boolean isKafkaHealthy() {
        try {
            // Tentative d'accès aux métadonnées du topic
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor(
                kafkaConfig.getTopics().getContractResults());
            return true;
        } catch (Exception e) {
            log.warn("Problème de connectivité Kafka détecté pour l'envoi de résultats: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Envoie un résultat d'erreur critique
     * Utilisé pour signaler des erreurs système importantes
     */
    public void sendCriticalError(String taskId, String workerId, String errorMessage) {
        TaskResultDTO errorResult = TaskResultDTO.builder()
            .taskId(taskId)
            .workerId(workerId)
            .status(TaskResultDTO.ProcessingStatus.FAILED)
            .summary("Erreur critique: " + errorMessage)
            .totalLinesProcessed(0L)
            .successfulContracts(0L)
            .failedContracts(0L)
            .skippedContracts(0L)
            .build();
        
        try {
            sendResultSync(errorResult);
            log.info("Résultat d'erreur critique envoyé pour la tâche {}", taskId);
        } catch (Exception e) {
            log.error("Impossible d'envoyer le résultat d'erreur critique pour la tâche {}: {}", 
                    taskId, e.getMessage());
        }
    }
}