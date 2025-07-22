package com.maroctelecom.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO représentant le résultat du traitement d'une partition
 * Contient les métriques, statistiques et erreurs du traitement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultDTO {
    
    /**
     * Identifiant de la tâche traitée
     */
    @NotBlank(message = "L'ID de la tâche ne peut pas être vide")
    private String taskId;
    
    /**
     * Identifiant de la partition
     */
    @NotNull(message = "L'ID de partition est requis")
    private Integer partitionId;
    
    /**
     * Identifiant du worker qui a traité la tâche
     */
    @NotBlank(message = "L'ID du worker ne peut pas être vide")
    private String workerId;
    
    /**
     * Statut du traitement
     */
    @NotNull(message = "Le statut est requis")
    private ProcessingStatus status;
    
    /**
     * Timestamp de début du traitement
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * Timestamp de fin du traitement
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * Durée du traitement en millisecondes
     */
    @Min(value = 0, message = "La durée doit être positive")
    private Long processingTimeMs;
    
    /**
     * Nombre total de lignes traitées
     */
    @Builder.Default
    private Long totalLinesProcessed = 0L;
    
    /**
     * Nombre de contrats traités avec succès
     */
    @Builder.Default
    private Long successfulContracts = 0L;
    
    /**
     * Nombre de contrats échoués
     */
    @Builder.Default
    private Long failedContracts = 0L;
    
    /**
     * Nombre de contrats ignorés (validation échouée)
     */
    @Builder.Default
    private Long skippedContracts = 0L;
    
    /**
     * Montant total des contrats traités (en DH)
     */
    @Builder.Default
    private Double totalAmount = 0.0;
    
    /**
     * Nombre de clients uniques traités
     */
    @Builder.Default
    private Long uniqueClientsCount = 0L;
    
    /**
     * Liste des erreurs rencontrées
     */
    private List<ErrorInfo> errors;
    
    /**
     * Métriques additionnelles personnalisées
     */
    private Map<String, Object> customMetrics;
    
    /**
     * Utilisation mémoire max pendant le traitement (en MB)
     */
    private Long maxMemoryUsageMB;
    
    /**
     * Utilisation CPU moyenne pendant le traitement (en %)
     */
    private Double avgCpuUsage;
    
    /**
     * Message de résumé du traitement
     */
    private String summary;
    
    /**
     * Statistiques par type de contrat
     */
    private Map<String, ContractTypeStats> contractTypeStats;
    
    /**
     * Calcule le taux de succès en pourcentage
     */
    public Double getSuccessRate() {
        if (totalLinesProcessed == null || totalLinesProcessed == 0) {
            return 0.0;
        }
        return (successfulContracts.doubleValue() / totalLinesProcessed.doubleValue()) * 100;
    }
    
    /**
     * Calcule le débit de traitement (lignes par seconde)
     */
    public Double getThroughput() {
        if (processingTimeMs == null || processingTimeMs == 0 || totalLinesProcessed == null) {
            return 0.0;
        }
        return (totalLinesProcessed.doubleValue() / processingTimeMs.doubleValue()) * 1000;
    }
    
    /**
     * Indique si le traitement a réussi
     */
    public boolean isSuccessful() {
        return ProcessingStatus.COMPLETED.equals(status);
    }
    
    /**
     * Indique si le traitement a échoué
     */
    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(status);
    }
    
    /**
     * Énumération des statuts de traitement
     */
    public enum ProcessingStatus {
        PENDING("En attente"),
        PROCESSING("En cours"),
        COMPLETED("Terminé"),
        FAILED("Échoué"),
        CANCELLED("Annulé"),
        TIMEOUT("Timeout");
        
        private final String description;
        
        ProcessingStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Statistiques par type de contrat
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractTypeStats {
        private String contractType;
        private Long count;
        private Double totalAmount;
        private Double averageAmount;
        private Long uniqueClients;
    }
}