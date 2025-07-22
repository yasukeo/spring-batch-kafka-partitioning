package com.maroctelecom.worker.listener;

import com.maroctelecom.common.config.KafkaConfig;
import com.maroctelecom.common.dto.PartitionTaskDTO;
import com.maroctelecom.common.dto.TaskResultDTO;
import com.maroctelecom.worker.processor.ContractProcessor;
import com.maroctelecom.worker.service.ResultSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Listener pour l'écoute des tâches de partition depuis Kafka
 * Traite les tâches de manière asynchrone et envoie les résultats
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartitionTaskListener {
    
    private final ContractProcessor contractProcessor;
    private final ResultSender resultSender;
    private final KafkaConfig kafkaConfig;
    
    @Value("${spring.application.name:worker-node}")
    private String workerId;
    
    @Value("${server.port:8081}")
    private String workerPort;
    
    /**
     * Écoute les tâches de partition depuis Kafka
     */
    @KafkaListener(
        topics = "#{@kafkaConfig.topics.contractPartitions}",
        groupId = "#{@kafkaConfig.consumerGroups.worker}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePartitionTask(
            @Payload PartitionTaskDTO partitionTask,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        String fullWorkerId = generateWorkerId();
        LocalDateTime startTime = LocalDateTime.now();
        
        log.info("Réception de la tâche {} sur worker {} (topic: {}, partition: {}, offset: {})",
                partitionTask.getTaskId(), fullWorkerId, topic, partition, offset);
        
        // Traitement asynchrone de la tâche
        CompletableFuture.supplyAsync(() -> processTask(partitionTask, fullWorkerId, startTime))
            .whenComplete((result, throwable) -> {
                try {
                    if (throwable != null) {
                        log.error("Erreur lors du traitement de la tâche {}: {}", 
                                partitionTask.getTaskId(), throwable.getMessage(), throwable);
                        
                        // Création d'un résultat d'erreur
                        TaskResultDTO errorResult = createErrorResult(partitionTask, fullWorkerId, startTime, throwable);
                        sendResult(errorResult);
                    } else {
                        log.info("Tâche {} traitée avec succès par {}: {} contrats traités",
                                partitionTask.getTaskId(), fullWorkerId, result.getTotalLinesProcessed());
                        sendResult(result);
                    }
                    
                    // Acknowledgment du message Kafka
                    acknowledgment.acknowledge();
                    
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi du résultat pour la tâche {}: {}", 
                            partitionTask.getTaskId(), e.getMessage(), e);
                    // On acknowledge quand même pour éviter la relivraison
                    acknowledgment.acknowledge();
                }
            });
    }
    
    /**
     * Traite une tâche de partition
     */
    private TaskResultDTO processTask(PartitionTaskDTO partitionTask, String workerId, LocalDateTime startTime) {
        try {
            log.debug("Début du traitement de la tâche {} par {}", partitionTask.getTaskId(), workerId);
            
            // Validation de la tâche
            validateTask(partitionTask);
            
            // Traitement de la partition
            TaskResultDTO result = contractProcessor.processPartition(partitionTask, workerId);
            
            // Mise à jour des timestamps
            result.setStartTime(startTime);
            result.setEndTime(LocalDateTime.now());
            
            // Calcul du temps de traitement
            if (result.getStartTime() != null && result.getEndTime() != null) {
                long processingTimeMs = java.time.Duration.between(result.getStartTime(), result.getEndTime()).toMillis();
                result.setProcessingTimeMs(processingTimeMs);
            }
            
            log.debug("Traitement de la tâche {} terminé avec succès", partitionTask.getTaskId());
            return result;
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la tâche {}: {}", partitionTask.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("Échec du traitement de la tâche: " + partitionTask.getTaskId(), e);
        }
    }
    
    /**
     * Valide une tâche avant traitement
     */
    private void validateTask(PartitionTaskDTO partitionTask) {
        if (partitionTask == null) {
            throw new IllegalArgumentException("La tâche ne peut pas être nulle");
        }
        
        if (partitionTask.getTaskId() == null || partitionTask.getTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la tâche ne peut pas être vide");
        }
        
        if (partitionTask.getFilePath() == null || partitionTask.getFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin du fichier ne peut pas être vide");
        }
        
        if (partitionTask.getStartLine() == null || partitionTask.getStartLine() <= 0) {
            throw new IllegalArgumentException("La ligne de début doit être positive");
        }
        
        if (partitionTask.getEndLine() == null || partitionTask.getEndLine() < partitionTask.getStartLine()) {
            throw new IllegalArgumentException("La ligne de fin doit être >= ligne de début");
        }
        
        // Vérification du timeout
        if (partitionTask.getTimeoutSeconds() != null && partitionTask.getTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Le timeout doit être positif");
        }
        
        log.debug("Validation de la tâche {} réussie", partitionTask.getTaskId());
    }
    
    /**
     * Crée un résultat d'erreur
     */
    private TaskResultDTO createErrorResult(PartitionTaskDTO partitionTask, String workerId, 
                                          LocalDateTime startTime, Throwable throwable) {
        return TaskResultDTO.builder()
            .taskId(partitionTask.getTaskId())
            .partitionId(partitionTask.getPartitionId())
            .workerId(workerId)
            .status(TaskResultDTO.ProcessingStatus.FAILED)
            .startTime(startTime)
            .endTime(LocalDateTime.now())
            .totalLinesProcessed(0L)
            .successfulContracts(0L)
            .failedContracts(0L)
            .skippedContracts(0L)
            .summary("Erreur lors du traitement: " + throwable.getMessage())
            .build();
    }
    
    /**
     * Envoie le résultat via Kafka
     */
    private void sendResult(TaskResultDTO result) {
        try {
            resultSender.sendResult(result);
            log.debug("Résultat de la tâche {} envoyé avec succès", result.getTaskId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du résultat pour la tâche {}: {}", 
                    result.getTaskId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Génère un ID unique pour ce worker
     */
    private String generateWorkerId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.trim().isEmpty()) {
            hostname = "localhost";
        }
        return String.format("%s-%s-%s", workerId, hostname, workerPort);
    }
}