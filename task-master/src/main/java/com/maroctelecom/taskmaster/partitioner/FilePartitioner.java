package com.maroctelecom.taskmaster.partitioner;

import com.maroctelecom.common.dto.PartitionTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Partitionneur intelligent pour les fichiers CSV
 * Découpe les gros fichiers en partitions optimales pour le traitement distribué
 */
@Slf4j
@Component
public class FilePartitioner implements Partitioner {
    
    private static final String PARTITION_KEY = "partition";
    private static final long DEFAULT_LINES_PER_PARTITION = 10000L;
    private static final int MAX_PARTITIONS = 50;
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        // Paramètres récupérés du job context
        String filePath = getFilePath();
        String fileName = getFileName(filePath);
        String jobId = getJobId();
        Long jobExecutionId = getJobExecutionId();
        
        try {
            long totalLines = countLines(filePath);
            log.info("Fichier {} contient {} lignes", fileName, totalLines);
            
            // Calcul du nombre optimal de partitions
            int optimalPartitions = calculateOptimalPartitions(totalLines, gridSize);
            long linesPerPartition = totalLines / optimalPartitions;
            
            log.info("Création de {} partitions avec ~{} lignes par partition", 
                    optimalPartitions, linesPerPartition);
            
            for (int i = 0; i < optimalPartitions; i++) {
                ExecutionContext context = new ExecutionContext();
                
                long startLine = (i * linesPerPartition) + 1; // 1-based
                long endLine = (i == optimalPartitions - 1) ? 
                    totalLines : ((i + 1) * linesPerPartition);
                
                // Création du DTO de tâche
                PartitionTaskDTO task = PartitionTaskDTO.builder()
                    .taskId(UUID.randomUUID().toString())
                    .fileName(fileName)
                    .filePath(filePath)
                    .startLine(startLine)
                    .endLine(endLine)
                    .partitionId(i)
                    .jobId(jobId)
                    .jobExecutionId(jobExecutionId)
                    .createdAt(LocalDateTime.now())
                    .estimatedLineCount(endLine - startLine + 1)
                    .timeoutSeconds(calculateTimeout(endLine - startLine + 1))
                    .build();
                
                // Ajout des paramètres au contexte Spring Batch
                context.putString("taskId", task.getTaskId());
                context.putString("fileName", task.getFileName());
                context.putString("filePath", task.getFilePath());
                context.putLong("startLine", task.getStartLine());
                context.putLong("endLine", task.getEndLine());
                context.putInt("partitionId", task.getPartitionId());
                context.putString("jobId", task.getJobId());
                context.putLong("jobExecutionId", task.getJobExecutionId());
                
                // Sérialisation de l'objet complet pour Kafka
                context.put("partitionTask", task);
                
                String partitionName = PARTITION_KEY + i;
                partitions.put(partitionName, context);
                
                log.debug("Partition {} créée: lignes {}-{} ({} lignes)", 
                        i, startLine, endLine, endLine - startLine + 1);
            }
            
        } catch (IOException e) {
            log.error("Erreur lors du partitioning du fichier {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Impossible de partitionner le fichier: " + filePath, e);
        }
        
        log.info("Partitioning terminé: {} partitions créées", partitions.size());
        return partitions;
    }
    
    /**
     * Compte le nombre de lignes dans un fichier
     */
    private long countLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new IOException("Le fichier n'existe pas: " + filePath);
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            long lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            // Exclure l'en-tête si présent
            return Math.max(0, lines - 1);
        }
    }
    
    /**
     * Calcule le nombre optimal de partitions
     */
    private int calculateOptimalPartitions(long totalLines, int gridSize) {
        if (totalLines <= DEFAULT_LINES_PER_PARTITION) {
            return 1;
        }
        
        // Calcul basé sur le nombre de lignes et la taille de la grille
        int calculatedPartitions = (int) Math.ceil((double) totalLines / DEFAULT_LINES_PER_PARTITION);
        
        // Limitation par la taille de la grille et le maximum configuré
        int optimalPartitions = Math.min(calculatedPartitions, gridSize);
        optimalPartitions = Math.min(optimalPartitions, MAX_PARTITIONS);
        
        return Math.max(1, optimalPartitions);
    }
    
    /**
     * Calcule le timeout en fonction du nombre de lignes
     */
    private int calculateTimeout(long lineCount) {
        // Timeout de base + temps supplémentaire basé sur le nombre de lignes
        // Estimé à 100 lignes par seconde en traitement moyen
        int baseTimeout = 60; // 1 minute de base
        int additionalTimeout = (int) (lineCount / 100); // 1 seconde pour 100 lignes
        
        return Math.max(baseTimeout, baseTimeout + additionalTimeout);
    }
    
    /**
     * Récupère le chemin du fichier depuis les paramètres du job
     * TODO: Implémenter la récupération depuis JobParameters
     */
    private String getFilePath() {
        // Pour l'instant, valeur par défaut - sera récupérée depuis JobParameters
        return System.getProperty("batch.input.file", "/tmp/contracts.csv");
    }
    
    /**
     * Extrait le nom du fichier depuis le chemin complet
     */
    private String getFileName(String filePath) {
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }
    
    /**
     * Récupère l'ID du job
     * TODO: Implémenter la récupération depuis JobExecution
     */
    private String getJobId() {
        return "contract-processing-job";
    }
    
    /**
     * Récupère l'ID d'exécution du job
     * TODO: Implémenter la récupération depuis JobExecution
     */
    private Long getJobExecutionId() {
        return System.currentTimeMillis();
    }
}