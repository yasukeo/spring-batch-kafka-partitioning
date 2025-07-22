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

/**
 * DTO représentant une tâche de partition pour le traitement distribué
 * Contient les informations nécessaires pour qu'un worker traite une portion de fichier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartitionTaskDTO {
    
    /**
     * Identifiant unique de la tâche
     */
    @NotBlank(message = "L'ID de la tâche ne peut pas être vide")
    private String taskId;
    
    /**
     * Nom du fichier à traiter
     */
    @NotBlank(message = "Le nom du fichier ne peut pas être vide")
    private String fileName;
    
    /**
     * Chemin complet vers le fichier
     */
    @NotBlank(message = "Le chemin du fichier ne peut pas être vide")
    private String filePath;
    
    /**
     * Ligne de début pour cette partition (1-based)
     */
    @NotNull(message = "L'offset de début est requis")
    @Min(value = 1, message = "L'offset doit être supérieur à 0")
    private Long startLine;
    
    /**
     * Ligne de fin pour cette partition (1-based, inclusive)
     */
    @NotNull(message = "L'offset de fin est requis")
    @Min(value = 1, message = "La limite doit être supérieure à 0")
    private Long endLine;
    
    /**
     * Identifiant de la partition Kafka (pour le parallélisme)
     */
    @NotNull(message = "L'ID de partition est requis")
    @Min(value = 0, message = "L'ID de partition doit être positif ou nul")
    private Integer partitionId;
    
    /**
     * Identifiant du job Spring Batch
     */
    @NotBlank(message = "L'ID du job ne peut pas être vide")
    private String jobId;
    
    /**
     * Identifiant de l'exécution du job
     */
    @NotNull(message = "L'ID d'exécution du job est requis")
    private Long jobExecutionId;
    
    /**
     * Timestamp de création de la tâche
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * Nombre estimé de lignes à traiter
     */
    private Long estimatedLineCount;
    
    /**
     * Priorité de la tâche (1-10, 1 = haute priorité)
     */
    @Builder.Default
    private Integer priority = 5;
    
    /**
     * Métadonnées additionnelles pour le traitement
     */
    private String metadata;
    
    /**
     * Timeout en secondes pour le traitement de cette partition
     */
    @Builder.Default
    private Integer timeoutSeconds = 300;
    
    /**
     * Retourne la taille de la partition en nombre de lignes
     */
    public Long getPartitionSize() {
        if (startLine != null && endLine != null) {
            return endLine - startLine + 1;
        }
        return estimatedLineCount;
    }
    
    /**
     * Indique si cette partition est considérée comme grande
     */
    public boolean isLargePartition() {
        Long size = getPartitionSize();
        return size != null && size > 10000; // Plus de 10K lignes
    }
}