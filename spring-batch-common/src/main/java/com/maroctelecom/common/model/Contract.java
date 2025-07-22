package com.maroctelecom.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modèle métier représentant un contrat Maroc Telecom
 * Contient toutes les informations nécessaires pour le traitement des contrats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    
    /**
     * Identifiant unique du contrat
     */
    @NotBlank(message = "L'ID du contrat ne peut pas être vide")
    @Size(min = 5, max = 50, message = "L'ID du contrat doit faire entre 5 et 50 caractères")
    private String contractId;
    
    /**
     * Identifiant du client
     */
    @NotBlank(message = "L'ID du client ne peut pas être vide")
    @Size(min = 3, max = 30, message = "L'ID du client doit faire entre 3 et 30 caractères")
    private String clientId;
    
    /**
     * Nom du client
     */
    @NotBlank(message = "Le nom du client ne peut pas être vide")
    @Size(max = 100, message = "Le nom du client ne peut pas dépasser 100 caractères")
    private String clientName;
    
    /**
     * Type de contrat (MOBILE, FIXE, INTERNET, ADSL, FIBRE, etc.)
     */
    @NotNull(message = "Le type de contrat est requis")
    private ContractType contractType;
    
    /**
     * Montant du contrat en Dirhams marocains
     */
    @NotNull(message = "Le montant du contrat est requis")
    @DecimalMin(value = "0.0", message = "Le montant doit être positif")
    @Digits(integer = 10, fraction = 2, message = "Le montant doit avoir au maximum 10 chiffres avant la virgule et 2 après")
    private BigDecimal amount;
    
    /**
     * Date de création du contrat
     */
    @NotNull(message = "La date de création est requise")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate creationDate;
    
    /**
     * Date d'activation du contrat
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate activationDate;
    
    /**
     * Date d'expiration du contrat
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;
    
    /**
     * Statut actuel du contrat
     */
    @NotNull(message = "Le statut du contrat est requis")
    private ContractStatus status;
    
    /**
     * Région du contrat (Casablanca, Rabat, Marrakech, etc.)
     */
    @NotBlank(message = "La région ne peut pas être vide")
    @Size(max = 50, message = "La région ne peut pas dépasser 50 caractères")
    private String region;
    
    /**
     * Ville du contrat
     */
    @Size(max = 50, message = "La ville ne peut pas dépasser 50 caractères")
    private String city;
    
    /**
     * Code postal
     */
    @Pattern(regexp = "\\d{5}", message = "Le code postal doit contenir 5 chiffres")
    private String postalCode;
    
    /**
     * Numéro de téléphone principal
     */
    @Pattern(regexp = "^(\\+212|0)[0-9]{9}$", message = "Format de téléphone marocain invalide")
    private String phoneNumber;
    
    /**
     * Email du client
     */
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;
    
    /**
     * Durée du contrat en mois
     */
    @Min(value = 1, message = "La durée doit être d'au moins 1 mois")
    @Max(value = 120, message = "La durée ne peut pas dépasser 120 mois")
    private Integer durationMonths;
    
    /**
     * Canal de vente (ONLINE, BOUTIQUE, TELEPHONE, PARTENAIRE)
     */
    private SalesChannel salesChannel;
    
    /**
     * Identifiant du vendeur/agent
     */
    @Size(max = 30, message = "L'ID du vendeur ne peut pas dépasser 30 caractères")
    private String salesAgentId;
    
    /**
     * Commentaires sur le contrat
     */
    @Size(max = 500, message = "Les commentaires ne peuvent pas dépasser 500 caractères")
    private String comments;
    
    /**
     * Référence externe (pour intégration avec systèmes legacy)
     */
    @Size(max = 50, message = "La référence externe ne peut pas dépasser 50 caractères")
    private String externalReference;
    
    /**
     * Timestamp de dernière modification
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModified;
    
    /**
     * Version pour gestion de la concurrence optimiste
     */
    @Builder.Default
    private Long version = 1L;
    
    /**
     * Indique si le contrat est prépayé
     */
    @Builder.Default
    private Boolean isPrepaid = false;
    
    /**
     * Remise appliquée en pourcentage
     */
    @DecimalMin(value = "0.0", message = "La remise doit être positive")
    @DecimalMax(value = "100.0", message = "La remise ne peut pas dépasser 100%")
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    /**
     * Énumération des types de contrats
     */
    public enum ContractType {
        MOBILE("Mobile"),
        FIXE("Fixe"),
        INTERNET("Internet"),
        ADSL("ADSL"),
        FIBRE("Fibre optique"),
        SATELLITE("Satellite"),
        ENTERPRISE("Entreprise"),
        GOVERNMENT("Gouvernement");
        
        private final String description;
        
        ContractType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Énumération des statuts de contrats
     */
    public enum ContractStatus {
        DRAFT("Brouillon"),
        PENDING("En attente"),
        ACTIVE("Actif"),
        SUSPENDED("Suspendu"),
        CANCELLED("Annulé"),
        EXPIRED("Expiré"),
        RENEWED("Renouvelé");
        
        private final String description;
        
        ContractStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isActive() {
            return this == ACTIVE;
        }
        
        public boolean isInactive() {
            return this == CANCELLED || this == EXPIRED || this == SUSPENDED;
        }
    }
    
    /**
     * Énumération des canaux de vente
     */
    public enum SalesChannel {
        ONLINE("En ligne"),
        BOUTIQUE("Boutique"),
        TELEPHONE("Téléphone"),
        PARTENAIRE("Partenaire"),
        DIRECT("Vente directe");
        
        private final String description;
        
        SalesChannel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Calcule le montant après remise
     */
    public BigDecimal getDiscountedAmount() {
        if (amount == null || discountPercentage == null) {
            return amount;
        }
        
        BigDecimal discount = amount.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        return amount.subtract(discount);
    }
    
    /**
     * Vérifie si le contrat est encore valide
     */
    public boolean isValid() {
        if (expirationDate == null) {
            return true; // Contrat sans expiration
        }
        return LocalDate.now().isBefore(expirationDate) || LocalDate.now().isEqual(expirationDate);
    }
    
    /**
     * Retourne l'âge du contrat en jours
     */
    public long getAgeInDays() {
        if (creationDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(creationDate, LocalDate.now());
    }
    
    /**
     * Indique si le contrat est éligible au renouvellement
     */
    public boolean isEligibleForRenewal() {
        if (expirationDate == null) {
            return false;
        }
        // Éligible 30 jours avant l'expiration
        return LocalDate.now().plusDays(30).isAfter(expirationDate) && isValid();
    }
}