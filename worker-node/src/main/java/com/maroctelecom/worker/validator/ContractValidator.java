package com.maroctelecom.worker.validator;

import com.maroctelecom.common.dto.ErrorInfo;
import com.maroctelecom.common.model.Contract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validateur métier pour les contrats Maroc Telecom
 * Effectue toutes les validations spécifiques au domaine métier
 */
@Slf4j
@Component
public class ContractValidator {
    
    // Patterns de validation
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+212|0)[0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("\\d{5}");
    private static final Pattern CONTRACT_ID_PATTERN = Pattern.compile("^[A-Z0-9]{5,50}$");
    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("^[A-Z0-9]{3,30}$");
    
    // Constantes métier
    private static final BigDecimal MIN_AMOUNT = BigDecimal.ZERO;
    private static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(1000000); // 1M DH
    private static final int MIN_DURATION_MONTHS = 1;
    private static final int MAX_DURATION_MONTHS = 120; // 10 ans
    private static final int CONTRACT_ID_MIN_LENGTH = 5;
    private static final int CONTRACT_ID_MAX_LENGTH = 50;
    
    // Régions valides du Maroc
    private static final List<String> VALID_REGIONS = List.of(
        "CASABLANCA-SETTAT", "RABAT-SALE-KENITRA", "MARRAKECH-SAFI", 
        "FES-MEKNES", "TANGER-TETOUAN-AL HOCEIMA", "ORIENTAL",
        "SOUSS-MASSA", "DRAA-TAFILALET", "BENI MELLAL-KHENIFRA",
        "LAAYOUNE-SAKIA EL HAMRA", "DAKHLA-OUED ED-DAHAB", "GUELMIM-OUED NOUN"
    );
    
    /**
     * Valide un contrat et retourne la liste des erreurs
     */
    public List<ErrorInfo> validate(Contract contract, Long lineNumber) {
        List<ErrorInfo> errors = new ArrayList<>();
        
        if (contract == null) {
            errors.add(ErrorInfo.validationError("contract", "null", 
                "Le contrat ne peut pas être null", lineNumber));
            return errors;
        }
        
        // Validation des champs obligatoires
        validateMandatoryFields(contract, errors, lineNumber);
        
        // Validation du format des champs
        validateFieldFormats(contract, errors, lineNumber);
        
        // Validation de la logique métier
        validateBusinessRules(contract, errors, lineNumber);
        
        // Validation des contraintes de données
        validateDataConstraints(contract, errors, lineNumber);
        
        if (!errors.isEmpty()) {
            log.debug("Validation échouée pour le contrat {}: {} erreurs", 
                    contract.getContractId(), errors.size());
        }
        
        return errors;
    }
    
    /**
     * Valide les champs obligatoires
     */
    private void validateMandatoryFields(Contract contract, List<ErrorInfo> errors, Long lineNumber) {
        if (isBlank(contract.getContractId())) {
            errors.add(ErrorInfo.validationError("contractId", contract.getContractId(),
                "L'ID du contrat est obligatoire", lineNumber));
        }
        
        if (isBlank(contract.getClientId())) {
            errors.add(ErrorInfo.validationError("clientId", contract.getClientId(),
                "L'ID du client est obligatoire", lineNumber));
        }
        
        if (isBlank(contract.getClientName())) {
            errors.add(ErrorInfo.validationError("clientName", contract.getClientName(),
                "Le nom du client est obligatoire", lineNumber));
        }
        
        if (contract.getContractType() == null) {
            errors.add(ErrorInfo.validationError("contractType", "null",
                "Le type de contrat est obligatoire", lineNumber));
        }
        
        if (contract.getAmount() == null) {
            errors.add(ErrorInfo.validationError("amount", "null",
                "Le montant est obligatoire", lineNumber));
        }
        
        if (contract.getCreationDate() == null) {
            errors.add(ErrorInfo.validationError("creationDate", "null",
                "La date de création est obligatoire", lineNumber));
        }
        
        if (contract.getStatus() == null) {
            errors.add(ErrorInfo.validationError("status", "null",
                "Le statut est obligatoire", lineNumber));
        }
        
        if (isBlank(contract.getRegion())) {
            errors.add(ErrorInfo.validationError("region", contract.getRegion(),
                "La région est obligatoire", lineNumber));
        }
    }
    
    /**
     * Valide le format des champs
     */
    private void validateFieldFormats(Contract contract, List<ErrorInfo> errors, Long lineNumber) {
        // Validation de l'ID du contrat
        if (!isBlank(contract.getContractId()) && 
            !CONTRACT_ID_PATTERN.matcher(contract.getContractId()).matches()) {
            errors.add(ErrorInfo.validationError("contractId", contract.getContractId(),
                "Format d'ID de contrat invalide (5-50 caractères alphanumériques)", lineNumber));
        }
        
        // Validation de l'ID du client
        if (!isBlank(contract.getClientId()) && 
            !CLIENT_ID_PATTERN.matcher(contract.getClientId()).matches()) {
            errors.add(ErrorInfo.validationError("clientId", contract.getClientId(),
                "Format d'ID de client invalide (3-30 caractères alphanumériques)", lineNumber));
        }
        
        // Validation du téléphone
        if (!isBlank(contract.getPhoneNumber()) && 
            !PHONE_PATTERN.matcher(contract.getPhoneNumber()).matches()) {
            errors.add(ErrorInfo.validationError("phoneNumber", contract.getPhoneNumber(),
                "Format de téléphone marocain invalide (+212xxxxxxxxx ou 0xxxxxxxxx)", lineNumber));
        }
        
        // Validation de l'email
        if (!isBlank(contract.getEmail()) && 
            !EMAIL_PATTERN.matcher(contract.getEmail()).matches()) {
            errors.add(ErrorInfo.validationError("email", contract.getEmail(),
                "Format d'email invalide", lineNumber));
        }
        
        // Validation du code postal
        if (!isBlank(contract.getPostalCode()) && 
            !POSTAL_CODE_PATTERN.matcher(contract.getPostalCode()).matches()) {
            errors.add(ErrorInfo.validationError("postalCode", contract.getPostalCode(),
                "Le code postal doit contenir 5 chiffres", lineNumber));
        }
    }
    
    /**
     * Valide la logique métier
     */
    private void validateBusinessRules(Contract contract, List<ErrorInfo> errors, Long lineNumber) {
        // Validation des montants
        if (contract.getAmount() != null) {
            if (contract.getAmount().compareTo(MIN_AMOUNT) < 0) {
                errors.add(ErrorInfo.validationError("amount", contract.getAmount().toString(),
                    "Le montant doit être positif", lineNumber));
            }
            
            if (contract.getAmount().compareTo(MAX_AMOUNT) > 0) {
                errors.add(ErrorInfo.validationError("amount", contract.getAmount().toString(),
                    "Le montant ne peut pas dépasser " + MAX_AMOUNT + " DH", lineNumber));
            }
        }
        
        // Validation de la durée
        if (contract.getDurationMonths() != null) {
            if (contract.getDurationMonths() < MIN_DURATION_MONTHS) {
                errors.add(ErrorInfo.validationError("durationMonths", contract.getDurationMonths().toString(),
                    "La durée doit être d'au moins " + MIN_DURATION_MONTHS + " mois", lineNumber));
            }
            
            if (contract.getDurationMonths() > MAX_DURATION_MONTHS) {
                errors.add(ErrorInfo.validationError("durationMonths", contract.getDurationMonths().toString(),
                    "La durée ne peut pas dépasser " + MAX_DURATION_MONTHS + " mois", lineNumber));
            }
        }
        
        // Validation de la région
        if (!isBlank(contract.getRegion()) && 
            !VALID_REGIONS.contains(contract.getRegion().toUpperCase())) {
            errors.add(ErrorInfo.validationError("region", contract.getRegion(),
                "Région marocaine invalide", lineNumber));
        }
        
        // Validation de la remise
        if (contract.getDiscountPercentage() != null) {
            if (contract.getDiscountPercentage().compareTo(BigDecimal.ZERO) < 0) {
                errors.add(ErrorInfo.validationError("discountPercentage", contract.getDiscountPercentage().toString(),
                    "La remise doit être positive", lineNumber));
            }
            
            if (contract.getDiscountPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                errors.add(ErrorInfo.validationError("discountPercentage", contract.getDiscountPercentage().toString(),
                    "La remise ne peut pas dépasser 100%", lineNumber));
            }
        }
    }
    
    /**
     * Valide les contraintes de données (cohérence temporelle, etc.)
     */
    private void validateDataConstraints(Contract contract, List<ErrorInfo> errors, Long lineNumber) {
        LocalDate today = LocalDate.now();
        
        // Validation des dates
        if (contract.getCreationDate() != null && contract.getCreationDate().isAfter(today)) {
            errors.add(ErrorInfo.validationError("creationDate", contract.getCreationDate().toString(),
                "La date de création ne peut pas être dans le futur", lineNumber));
        }
        
        if (contract.getActivationDate() != null && contract.getCreationDate() != null &&
            contract.getActivationDate().isBefore(contract.getCreationDate())) {
            errors.add(ErrorInfo.validationError("activationDate", contract.getActivationDate().toString(),
                "La date d'activation ne peut pas être antérieure à la création", lineNumber));
        }
        
        if (contract.getExpirationDate() != null && contract.getActivationDate() != null &&
            contract.getExpirationDate().isBefore(contract.getActivationDate())) {
            errors.add(ErrorInfo.validationError("expirationDate", contract.getExpirationDate().toString(),
                "La date d'expiration ne peut pas être antérieure à l'activation", lineNumber));
        }
        
        // Validation de la cohérence statut/dates
        if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
            if (contract.getActivationDate() == null) {
                errors.add(ErrorInfo.validationError("activationDate", "null",
                    "Un contrat actif doit avoir une date d'activation", lineNumber));
            }
            
            if (contract.getExpirationDate() != null && contract.getExpirationDate().isBefore(today)) {
                errors.add(ErrorInfo.validationError("status", contract.getStatus().toString(),
                    "Un contrat expiré ne peut pas être actif", lineNumber));
            }
        }
        
        // Validation de la cohérence type/canal
        if (contract.getContractType() == Contract.ContractType.ENTERPRISE &&
            contract.getSalesChannel() == Contract.SalesChannel.ONLINE) {
            errors.add(ErrorInfo.validationError("salesChannel", contract.getSalesChannel().toString(),
                "Les contrats entreprise ne peuvent pas être vendus en ligne", lineNumber));
        }
    }
    
    /**
     * Vérifie si une chaîne est vide ou null
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Valide rapidement un contrat (validation de base seulement)
     */
    public boolean isValidBasic(Contract contract) {
        return contract != null &&
               !isBlank(contract.getContractId()) &&
               !isBlank(contract.getClientId()) &&
               contract.getAmount() != null &&
               contract.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               contract.getContractType() != null &&
               contract.getStatus() != null;
    }
    
    /**
     * Compte le nombre d'erreurs critiques (bloquantes)
     */
    public long countCriticalErrors(List<ErrorInfo> errors) {
        return errors.stream()
                .filter(error -> error.getErrorType() == ErrorInfo.ErrorType.VALIDATION_ERROR)
                .filter(error -> isCriticalField(error.getFieldName()))
                .count();
    }
    
    /**
     * Détermine si un champ est critique
     */
    private boolean isCriticalField(String fieldName) {
        return List.of("contractId", "clientId", "amount", "contractType", "status").contains(fieldName);
    }
}