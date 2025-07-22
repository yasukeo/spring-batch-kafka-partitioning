package com.maroctelecom.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Classe représentant les informations détaillées d'une erreur
 * Utilisée pour le reporting et le debugging des problèmes de traitement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorInfo {
    
    /**
     * Type d'erreur
     */
    private ErrorType errorType;
    
    /**
     * Code d'erreur spécifique
     */
    private String errorCode;
    
    /**
     * Message d'erreur principal
     */
    private String message;
    
    /**
     * Message d'erreur détaillé
     */
    private String detailedMessage;
    
    /**
     * Numéro de ligne où l'erreur s'est produite
     */
    private Long lineNumber;
    
    /**
     * Données de la ligne qui a causé l'erreur
     */
    private String lineData;
    
    /**
     * Nom du champ qui a causé l'erreur (pour les erreurs de validation)
     */
    private String fieldName;
    
    /**
     * Valeur du champ qui a causé l'erreur
     */
    private String fieldValue;
    
    /**
     * Stack trace de l'exception (pour le debugging)
     */
    private String stackTrace;
    
    /**
     * Timestamp de l'erreur
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Identifiant du thread qui a rencontré l'erreur
     */
    private String threadId;
    
    /**
     * Contexte additionnel de l'erreur
     */
    private String context;
    
    /**
     * Indique si l'erreur est récupérable
     */
    @Builder.Default
    private Boolean recoverable = false;
    
    /**
     * Nombre de tentatives de retry pour cette erreur
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Action recommandée pour résoudre l'erreur
     */
    private String recommendedAction;
    
    /**
     * Énumération des types d'erreurs
     */
    public enum ErrorType {
        VALIDATION_ERROR("Erreur de validation"),
        PARSING_ERROR("Erreur de parsing"),
        BUSINESS_LOGIC_ERROR("Erreur de logique métier"),
        TECHNICAL_ERROR("Erreur technique"),
        TIMEOUT_ERROR("Erreur de timeout"),
        NETWORK_ERROR("Erreur réseau"),
        DATABASE_ERROR("Erreur base de données"),
        KAFKA_ERROR("Erreur Kafka"),
        UNKNOWN_ERROR("Erreur inconnue");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Crée un ErrorInfo pour une erreur de validation
     */
    public static ErrorInfo validationError(String fieldName, String fieldValue, String message, Long lineNumber) {
        return ErrorInfo.builder()
                .errorType(ErrorType.VALIDATION_ERROR)
                .errorCode("VALIDATION_FAILED")
                .message(message)
                .fieldName(fieldName)
                .fieldValue(fieldValue)
                .lineNumber(lineNumber)
                .timestamp(LocalDateTime.now())
                .recoverable(false)
                .recommendedAction("Corriger les données d'entrée")
                .build();
    }
    
    /**
     * Crée un ErrorInfo pour une erreur de parsing
     */
    public static ErrorInfo parsingError(String lineData, String message, Long lineNumber) {
        return ErrorInfo.builder()
                .errorType(ErrorType.PARSING_ERROR)
                .errorCode("PARSING_FAILED")
                .message(message)
                .lineData(lineData)
                .lineNumber(lineNumber)
                .timestamp(LocalDateTime.now())
                .recoverable(false)
                .recommendedAction("Vérifier le format des données")
                .build();
    }
    
    /**
     * Crée un ErrorInfo pour une erreur technique
     */
    public static ErrorInfo technicalError(String message, Exception exception) {
        return ErrorInfo.builder()
                .errorType(ErrorType.TECHNICAL_ERROR)
                .errorCode("TECHNICAL_ERROR")
                .message(message)
                .detailedMessage(exception.getMessage())
                .stackTrace(getStackTrace(exception))
                .timestamp(LocalDateTime.now())
                .recoverable(true)
                .recommendedAction("Retry ou investigation technique")
                .build();
    }
    
    /**
     * Convertit une exception en string
     */
    private static String getStackTrace(Exception exception) {
        if (exception == null) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getSimpleName()).append(": ").append(exception.getMessage()).append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            // Limite la stack trace pour éviter des messages trop longs
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)\n");
                break;
            }
        }
        
        return sb.toString();
    }
}