package com.maroctelecom.taskmaster.listener;

import com.maroctelecom.common.config.KafkaConfig;
import com.maroctelecom.common.dto.TaskResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listener pour l'écoute et l'agrégation des résultats des workers
 * Reçoit les résultats de traitement via Kafka et les agrège
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultListener {
    
    private final KafkaConfig kafkaConfig;
    
    // Cache des résultats par job d'exécution
    private final Map<Long, JobExecutionResults> jobResults = new ConcurrentHashMap<>();
    
    /**
     * Écoute les résultats de traitement des workers
     */
    @KafkaListener(
        topics = "#{@kafkaConfig.topics.contractResults}",
        groupId = "#{@kafkaConfig.consumerGroups.taskMaster}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTaskResult(
            @Payload TaskResultDTO taskResult,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Réception du résultat de la tâche {} du worker {} (partition {}, offset {})",
                    taskResult.getTaskId(), taskResult.getWorkerId(), partition, offset);
            
            // Agrégation des résultats
            aggregateResult(taskResult);
            
            // Log des métriques importantes
            logTaskMetrics(taskResult);
            
            // Acknowledgment manuel du message
            acknowledgment.acknowledge();
            
            log.debug("Résultat de la tâche {} traité avec succès", taskResult.getTaskId());
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du résultat de la tâche {}: {}", 
                    taskResult.getTaskId(), e.getMessage(), e);
            
            // En cas d'erreur, on acknowledge quand même pour éviter la relivraison
            // L'erreur sera trackée dans les métriques
            acknowledgment.acknowledge();
        }
    }
    
    /**
     * Agrège les résultats d'une tâche dans le résultat global du job
     */
    private void aggregateResult(TaskResultDTO taskResult) {
        // Extraction de l'ID d'exécution du job depuis le taskId ou utilisation d'un mapping
        Long jobExecutionId = extractJobExecutionId(taskResult);
        
        JobExecutionResults jobResult = jobResults.computeIfAbsent(jobExecutionId, 
                id -> new JobExecutionResults(id));
        
        synchronized (jobResult) {
            jobResult.addTaskResult(taskResult);
            
            log.info("Agrégation mise à jour pour le job {}: {} tâches complétées, {} succès, {} échecs",
                    jobExecutionId,
                    jobResult.getCompletedTasks(),
                    jobResult.getTotalSuccessfulContracts(),
                    jobResult.getTotalFailedContracts());
        }
    }
    
    /**
     * Log des métriques importantes d'une tâche
     */
    private void logTaskMetrics(TaskResultDTO taskResult) {
        if (taskResult.isSuccessful()) {
            log.info("✓ Tâche {} réussie: {} contrats traités en {}ms (taux: {:.2f}%, débit: {:.2f} contrats/sec)",
                    taskResult.getTaskId(),
                    taskResult.getTotalLinesProcessed(),
                    taskResult.getProcessingTimeMs(),
                    taskResult.getSuccessRate(),
                    taskResult.getThroughput());
        } else {
            log.warn("✗ Tâche {} échouée: statut={}, erreurs={}",
                    taskResult.getTaskId(),
                    taskResult.getStatus(),
                    taskResult.getErrors() != null ? taskResult.getErrors().size() : 0);
        }
        
        // Log des statistiques de performance
        if (taskResult.getMaxMemoryUsageMB() != null) {
            log.debug("Utilisation mémoire max: {}MB, CPU moyen: {}%",
                    taskResult.getMaxMemoryUsageMB(),
                    taskResult.getAvgCpuUsage());
        }
    }
    
    /**
     * Extrait l'ID d'exécution du job depuis le résultat de la tâche
     * TODO: Implémenter un mapping plus robuste
     */
    private Long extractJobExecutionId(TaskResultDTO taskResult) {
        // Pour l'instant, utilisation d'un hash du taskId
        // Dans une implémentation réelle, ceci devrait être fourni explicitement
        return (long) taskResult.getTaskId().hashCode();
    }
    
    /**
     * Retourne les résultats agrégés pour un job d'exécution
     */
    public JobExecutionResults getJobResults(Long jobExecutionId) {
        return jobResults.get(jobExecutionId);
    }
    
    /**
     * Nettoie les résultats anciens pour éviter les fuites mémoire
     */
    public void cleanupOldResults(LocalDateTime before) {
        jobResults.entrySet().removeIf(entry -> 
                entry.getValue().getStartTime().isBefore(before));
        
        log.info("Nettoyage des anciens résultats effectué");
    }
    
    /**
     * Classe interne pour agréger les résultats d'un job
     */
    public static class JobExecutionResults {
        private final Long jobExecutionId;
        private final LocalDateTime startTime;
        private final AtomicLong completedTasks = new AtomicLong(0);
        private final AtomicLong totalSuccessfulContracts = new AtomicLong(0);
        private final AtomicLong totalFailedContracts = new AtomicLong(0);
        private final AtomicLong totalSkippedContracts = new AtomicLong(0);
        private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
        private volatile LocalDateTime lastUpdateTime;
        
        public JobExecutionResults(Long jobExecutionId) {
            this.jobExecutionId = jobExecutionId;
            this.startTime = LocalDateTime.now();
            this.lastUpdateTime = LocalDateTime.now();
        }
        
        public void addTaskResult(TaskResultDTO taskResult) {
            completedTasks.incrementAndGet();
            totalSuccessfulContracts.addAndGet(taskResult.getSuccessfulContracts());
            totalFailedContracts.addAndGet(taskResult.getFailedContracts());
            totalSkippedContracts.addAndGet(taskResult.getSkippedContracts());
            
            if (taskResult.getProcessingTimeMs() != null) {
                totalProcessingTimeMs.addAndGet(taskResult.getProcessingTimeMs());
            }
            
            lastUpdateTime = LocalDateTime.now();
        }
        
        // Getters
        public Long getJobExecutionId() { return jobExecutionId; }
        public LocalDateTime getStartTime() { return startTime; }
        public long getCompletedTasks() { return completedTasks.get(); }
        public long getTotalSuccessfulContracts() { return totalSuccessfulContracts.get(); }
        public long getTotalFailedContracts() { return totalFailedContracts.get(); }
        public long getTotalSkippedContracts() { return totalSkippedContracts.get(); }
        public long getTotalProcessingTimeMs() { return totalProcessingTimeMs.get(); }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        
        public double getOverallSuccessRate() {
            long total = totalSuccessfulContracts.get() + totalFailedContracts.get() + totalSkippedContracts.get();
            return total > 0 ? (totalSuccessfulContracts.get() * 100.0) / total : 0.0;
        }
        
        public double getAverageProcessingTimePerTask() {
            long tasks = completedTasks.get();
            return tasks > 0 ? (double) totalProcessingTimeMs.get() / tasks : 0.0;
        }
    }
}