#!/bin/bash

# Script de génération de données de test pour Maroc Telecom
# Génère un fichier CSV avec des contrats réalistes

OUTPUT_FILE="${1:-test-data/sample-contracts.csv}"
NUM_CONTRACTS="${2:-100000}"

echo "Génération de $NUM_CONTRACTS contrats dans $OUTPUT_FILE..."

# Créer le répertoire si nécessaire
mkdir -p "$(dirname "$OUTPUT_FILE")"

# En-têtes CSV
echo "contractId,clientId,clientName,contractType,amount,creationDate,activationDate,expirationDate,status,region,city,postalCode,phoneNumber,email,durationMonths,salesChannel,salesAgentId" > "$OUTPUT_FILE"

# Types de contrats
TYPES=("MOBILE" "FIXE" "INTERNET" "ADSL" "FIBRE" "SATELLITE")
STATUSES=("ACTIVE" "PENDING" "SUSPENDED" "CANCELLED" "EXPIRED")
REGIONS=("CASABLANCA-SETTAT" "RABAT-SALE-KENITRA" "MARRAKECH-SAFI" "FES-MEKNES" "TANGER-TETOUAN-AL HOCEIMA" "ORIENTAL")
CITIES=("Casablanca" "Rabat" "Marrakech" "Fes" "Tanger" "Oujda" "Sale" "Meknes" "Agadir" "Kenitra")
CHANNELS=("ONLINE" "BOUTIQUE" "TELEPHONE" "PARTENAIRE")

# Noms marocains communs
FIRST_NAMES=("Mohamed" "Ahmed" "Fatima" "Aicha" "Youssef" "Khadija" "Omar" "Zineb" "Hassan" "Nadia" "Rachid" "Samira" "Karim" "Laila" "Abderrahim" "Salma" "Mustapha" "Amina" "Said" "Meryem")
LAST_NAMES=("Alami" "Bennani" "Chraibi" "Douiri" "El Fassi" "Filali" "Ghali" "Hajji" "Idrissi" "Jaber" "Kabbaj" "Lamrani" "Majidi" "Naciri" "Ouali" "Qadiri" "Rami" "Sabbahi" "Tazi" "Ziani")

# Fonction pour générer un nom aléatoire
generate_name() {
    local first=${FIRST_NAMES[$((RANDOM % ${#FIRST_NAMES[@]}))]}
    local last=${LAST_NAMES[$((RANDOM % ${#LAST_NAMES[@]}))]}
    echo "$first $last"
}

# Fonction pour générer une date
generate_date() {
    local year=$((2020 + RANDOM % 4))
    local month=$((1 + RANDOM % 12))
    local day=$((1 + RANDOM % 28))
    printf "%04d-%02d-%02d" $year $month $day
}

# Fonction pour générer un téléphone marocain
generate_phone() {
    local prefix=("06" "07" "05")
    local p=${prefix[$((RANDOM % ${#prefix[@]}))]}
    local number=$((10000000 + RANDOM % 90000000))
    echo "0$p$(printf "%08d" $number | cut -c1-8)"
}

# Génération des contrats
for i in $(seq 1 $NUM_CONTRACTS); do
    if [ $((i % 10000)) -eq 0 ]; then
        echo "Généré $i contrats..."
    fi
    
    # Données de base
    CONTRACT_ID="CT$(printf "%08d" $i)"
    CLIENT_ID="CL$(printf "%06d" $((1 + RANDOM % 50000)))"
    CLIENT_NAME=$(generate_name)
    CONTRACT_TYPE=${TYPES[$((RANDOM % ${#TYPES[@]}))]}
    
    # Montant basé sur le type
    case $CONTRACT_TYPE in
        "MOBILE") AMOUNT=$((50 + RANDOM % 200)) ;;
        "FIXE") AMOUNT=$((80 + RANDOM % 150)) ;;
        "INTERNET") AMOUNT=$((100 + RANDOM % 300)) ;;
        "ADSL") AMOUNT=$((120 + RANDOM % 200)) ;;
        "FIBRE") AMOUNT=$((200 + RANDOM % 400)) ;;
        "SATELLITE") AMOUNT=$((300 + RANDOM % 500)) ;;
    esac
    
    # Dates
    CREATION_DATE=$(generate_date)
    ACTIVATION_DATE=$(generate_date)
    EXPIRATION_DATE=$(generate_date)
    
    # Statut avec distribution réaliste
    if [ $((RANDOM % 100)) -lt 70 ]; then
        STATUS="ACTIVE"
    elif [ $((RANDOM % 100)) -lt 15 ]; then
        STATUS="PENDING"
    elif [ $((RANDOM % 100)) -lt 10 ]; then
        STATUS="SUSPENDED"
    else
        STATUS="CANCELLED"
    fi
    
    # Localisation
    REGION=${REGIONS[$((RANDOM % ${#REGIONS[@]}))]}
    CITY=${CITIES[$((RANDOM % ${#CITIES[@]}))]}
    POSTAL_CODE=$((10000 + RANDOM % 90000))
    
    # Contact
    PHONE=$(generate_phone)
    EMAIL_PREFIX=$(echo "$CLIENT_NAME" | tr ' ' '.' | tr '[:upper:]' '[:lower:]')
    EMAIL="$EMAIL_PREFIX@email.com"
    
    # Contrat
    DURATION=$((12 + RANDOM % 36))
    SALES_CHANNEL=${CHANNELS[$((RANDOM % ${#CHANNELS[@]}))]}
    AGENT_ID="AG$(printf "%04d" $((1 + RANDOM % 200)))"
    
    # Écriture de la ligne
    echo "$CONTRACT_ID,$CLIENT_ID,$CLIENT_NAME,$CONTRACT_TYPE,$AMOUNT,$CREATION_DATE,$ACTIVATION_DATE,$EXPIRATION_DATE,$STATUS,$REGION,$CITY,$POSTAL_CODE,$PHONE,$EMAIL,$DURATION,$SALES_CHANNEL,$AGENT_ID" >> "$OUTPUT_FILE"
done

echo "Génération terminée: $NUM_CONTRACTS contrats générés dans $OUTPUT_FILE"
echo "Taille du fichier: $(du -h "$OUTPUT_FILE" | cut -f1)"
echo ""
echo "Exemples de données générées:"
head -5 "$OUTPUT_FILE"