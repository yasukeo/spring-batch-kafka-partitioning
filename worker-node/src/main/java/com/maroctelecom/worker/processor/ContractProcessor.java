package com.maroctelecom.worker.processor;

import com.maroctelecom.common.dto.ErrorInfo;
import com.maroctelecom.common.dto.PartitionTaskDTO;
import com.maroctelecom.common.dto.TaskResultDTO;
import com.maroctelecom.common.model.Contract;
import com.maroctelecom.worker.validator.ContractValidator;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Processeur principal pour le traitement des contrats Maroc Telecom
 * Gère le parsing, la validation et le traitement métier des contrats
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractProcessor {
    
    private final ContractValidator contractValidator;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] EXPECTED_HEADERS = {
        "contractId", "clientId", "clientName", "contractType", "amount", 
        "creationDate", "activationDate", "expirationDate", "status", 
        "region", "city", "postalCode", "phoneNumber", "email", 
        "durationMonths", "salesChannel", "salesAgentId"
    };
    
    /**
     * Traite une partition de contrats
     */
    public TaskResultDTO processPartition(PartitionTaskDTO partitionTask, String workerId) {
        LocalDateTime startTime = LocalDateTime.now();
        
        TaskResultDTO.TaskResultDTOBuilder resultBuilder = TaskResultDTO.builder()
            .taskId(partitionTask.getTaskId())
            .partitionId(partitionTask.getPartitionId())
            .workerId(workerId)
            .startTime(startTime)
            .status(TaskResultDTO.ProcessingStatus.PROCESSING);
        
        List<ErrorInfo> errors = new ArrayList<>();
        Map<String, TaskResultDTO.ContractTypeStats> contractTypeStats = new HashMap<>();
        Set<String> uniqueClients = new HashSet<>();
        
        AtomicLong successfulContracts = new AtomicLong(0);
        AtomicLong failedContracts = new AtomicLong(0);
        AtomicLong skippedContracts = new AtomicLong(0);
        AtomicLong totalLinesProcessed = new AtomicLong(0);
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        try (CSVReader csvReader = new CSVReader(new FileReader(partitionTask.getFilePath()))) {
            
            // Lecture et validation des en-têtes
            String[] headers;
            try {
                headers = csvReader.readNext();
            } catch (com.opencsv.exceptions.CsvValidationException e) {
                throw new IllegalArgumentException("Erreur de validation lors de la lecture des en-têtes", e);
            }
            
            if (headers == null || !validateHeaders(headers)) {
                throw new IllegalArgumentException("En-têtes CSV invalides ou manquants");
            }
            
            // Positionnement à la ligne de début
            skipToStartLine(csvReader, partitionTask.getStartLine() - 1); // -1 car on a déjà lu les headers
            
            String[] line;
            long currentLine = partitionTask.getStartLine();
            
            try {
                while ((line = csvReader.readNext()) != null && currentLine <= partitionTask.getEndLine()) {
                totalLinesProcessed.incrementAndGet();
                
                try {
                    // Parsing du contrat
                    Contract contract = parseContract(line, headers, currentLine);
                    
                    // Validation métier
                    List<ErrorInfo> validationErrors = contractValidator.validate(contract, currentLine);
                    
                    if (validationErrors.isEmpty()) {
                        // Traitement métier
                        processContract(contract, contractTypeStats, uniqueClients);
                        
                        totalAmount = totalAmount.add(contract.getAmount());
                        successfulContracts.incrementAndGet();
                        
                        log.debug("Contrat {} traité avec succès", contract.getContractId());
                        
                    } else {
                        // Contrat invalide
                        errors.addAll(validationErrors);
                        skippedContracts.incrementAndGet();
                        
                        log.debug("Contrat ligne {} ignoré: {} erreurs de validation", 
                                currentLine, validationErrors.size());
                    }
                    
                } catch (Exception e) {
                    // Erreur de parsing ou de traitement
                    ErrorInfo error = ErrorInfo.technicalError(
                        String.format("Erreur ligne %d: %s", currentLine, e.getMessage()), e);
                    error.setLineNumber(currentLine);
                    error.setLineData(String.join(",", line));
                    
                    errors.add(error);
                    failedContracts.incrementAndGet();
                    
                    log.debug("Erreur ligne {}: {}", currentLine, e.getMessage());
                }
                
                currentLine++;
            }
            } catch (com.opencsv.exceptions.CsvValidationException e) {
                throw new RuntimeException("Erreur de validation CSV ligne " + currentLine, e);
            }
            
            // Construction du résultat
            TaskResultDTO result = resultBuilder
                .status(TaskResultDTO.ProcessingStatus.COMPLETED)
                .endTime(LocalDateTime.now())
                .totalLinesProcessed(totalLinesProcessed.get())
                .successfulContracts(successfulContracts.get())
                .failedContracts(failedContracts.get())
                .skippedContracts(skippedContracts.get())
                .totalAmount(totalAmount.doubleValue())
                .uniqueClientsCount((long) uniqueClients.size())
                .errors(errors.isEmpty() ? null : errors)
                .contractTypeStats(contractTypeStats)
                .summary(generateSummary(totalLinesProcessed.get(), successfulContracts.get(), 
                       failedContracts.get(), skippedContracts.get()))
                .build();
            
            log.info("Partition {} traitée: {} lignes, {} succès, {} échecs, {} ignorés",
                    partitionTask.getPartitionId(), totalLinesProcessed.get(), 
                    successfulContracts.get(), failedContracts.get(), skippedContracts.get());
            
            return result;
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la partition {}: {}", 
                    partitionTask.getPartitionId(), e.getMessage(), e);
            
            return resultBuilder
                .status(TaskResultDTO.ProcessingStatus.FAILED)
                .endTime(LocalDateTime.now())
                .totalLinesProcessed(totalLinesProcessed.get())
                .successfulContracts(successfulContracts.get())
                .failedContracts(failedContracts.get())
                .skippedContracts(skippedContracts.get())
                .summary("Échec du traitement: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Valide les en-têtes du fichier CSV
     */
    private boolean validateHeaders(String[] headers) {
        if (headers == null || headers.length < EXPECTED_HEADERS.length) {
            return false;
        }
        
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                log.warn("En-tête inattendu à la position {}: attendu '{}', trouvé '{}'", 
                        i, EXPECTED_HEADERS[i], headers[i]);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Se positionne à la ligne de début
     */
    private void skipToStartLine(CSVReader csvReader, long linesToSkip) throws IOException {
        try {
            for (long i = 0; i < linesToSkip; i++) {
                if (csvReader.readNext() == null) {
                    throw new IOException("Impossible de se positionner à la ligne " + (linesToSkip + 1));
                }
            }
        } catch (com.opencsv.exceptions.CsvValidationException e) {
            throw new IOException("Erreur de validation CSV lors du positionnement", e);
        }
    }
    
    /**
     * Parse une ligne CSV en objet Contract
     */
    private Contract parseContract(String[] line, String[] headers, long lineNumber) {
        if (line.length < EXPECTED_HEADERS.length) {
            throw new IllegalArgumentException("Ligne incomplète: " + line.length + " colonnes, " + 
                                             EXPECTED_HEADERS.length + " attendues");
        }
        
        try {
            return Contract.builder()
                .contractId(getValue(line, 0))
                .clientId(getValue(line, 1))
                .clientName(getValue(line, 2))
                .contractType(parseContractType(getValue(line, 3)))
                .amount(parseAmount(getValue(line, 4)))
                .creationDate(parseDate(getValue(line, 5)))
                .activationDate(parseDate(getValue(line, 6)))
                .expirationDate(parseDate(getValue(line, 7)))
                .status(parseContractStatus(getValue(line, 8)))
                .region(getValue(line, 9))
                .city(getValue(line, 10))
                .postalCode(getValue(line, 11))
                .phoneNumber(getValue(line, 12))
                .email(getValue(line, 13))
                .durationMonths(parseInteger(getValue(line, 14)))
                .salesChannel(parseSalesChannel(getValue(line, 15)))
                .salesAgentId(getValue(line, 16))
                .lastModified(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            throw new RuntimeException("Erreur de parsing ligne " + lineNumber + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Traite un contrat valide (logique métier)
     */
    private void processContract(Contract contract, 
                               Map<String, TaskResultDTO.ContractTypeStats> contractTypeStats,
                               Set<String> uniqueClients) {
        
        // Ajout du client unique
        uniqueClients.add(contract.getClientId());
        
        // Mise à jour des statistiques par type de contrat
        String typeKey = contract.getContractType().name();
        TaskResultDTO.ContractTypeStats stats = contractTypeStats.computeIfAbsent(typeKey, 
            k -> TaskResultDTO.ContractTypeStats.builder()
                .contractType(typeKey)
                .count(0L)
                .totalAmount(0.0)
                .uniqueClients(0L)
                .build());
        
        stats.setCount(stats.getCount() + 1);
        stats.setTotalAmount(stats.getTotalAmount() + contract.getAmount().doubleValue());
        
        // Calcul de la moyenne
        stats.setAverageAmount(stats.getTotalAmount() / stats.getCount());
        
        // TODO: Ajouter d'autres traitements métier spécifiques à Maroc Telecom
        // - Calculs de commissions
        // - Vérifications de crédit
        // - Intégrations systèmes externes
        // - Notifications
    }
    
    /**
     * Génère un résumé du traitement
     */
    private String generateSummary(long total, long success, long failed, long skipped) {
        double successRate = total > 0 ? (success * 100.0) / total : 0.0;
        return String.format("Traitement terminé: %d lignes (%.2f%% succès, %d échecs, %d ignorés)", 
                           total, successRate, failed, skipped);
    }
    
    // Méthodes utilitaires de parsing
    
    private String getValue(String[] line, int index) {
        return line[index] != null ? line[index].trim() : "";
    }
    
    private Contract.ContractType parseContractType(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Type de contrat requis");
        }
        try {
            return Contract.ContractType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de contrat invalide: " + value);
        }
    }
    
    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Montant requis");
        }
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant invalide: " + value);
        }
    }
    
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide: " + value + " (format attendu: yyyy-MM-dd)");
        }
    }
    
    private Contract.ContractStatus parseContractStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Statut requis");
        }
        try {
            return Contract.ContractStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide: " + value);
        }
    }
    
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nombre invalide: " + value);
        }
    }
    
    private Contract.SalesChannel parseSalesChannel(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Contract.SalesChannel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Canal de vente invalide: " + value);
        }
    }
}