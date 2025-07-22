#!/bin/bash

# Script de monitoring Kafka en temps réel
# Surveille les topics, les partitions, les offsets et les lags

KAFKA_HOME="${KAFKA_HOME:-/opt/kafka}"
BOOTSTRAP_SERVERS="${1:-localhost:9092}"

echo "=== Monitoring Kafka - Spring Batch Partitioning ==="
echo "Bootstrap servers: $BOOTSTRAP_SERVERS"
echo "Timestamp: $(date)"
echo ""

# Fonction pour afficher les topics
show_topics() {
    echo "📋 Topics disponibles:"
    kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --list 2>/dev/null | grep -E "(contract|batch)" | while read topic; do
        echo "  • $topic"
    done
    echo ""
}

# Fonction pour afficher les détails des topics
show_topic_details() {
    echo "🔍 Détails des topics:"
    for topic in contract-partitions contract-results contract-dead-letter contract-monitoring; do
        echo "  📊 Topic: $topic"
        kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --topic $topic 2>/dev/null | grep -v "^$" | sed 's/^/    /'
        echo ""
    done
}

# Fonction pour afficher les groupes de consommateurs
show_consumer_groups() {
    echo "👥 Groupes de consommateurs:"
    kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --list 2>/dev/null | grep -E "(task-master|worker)" | while read group; do
        echo "  • $group"
    done
    echo ""
}

# Fonction pour afficher les offsets et lags
show_consumer_lag() {
    echo "📈 Offsets et Lag des consommateurs:"
    
    for group in task-master-group worker-group; do
        echo "  🎯 Groupe: $group"
        kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $group 2>/dev/null | grep -v "^$" | while read line; do
            if [[ $line == *"TOPIC"* ]]; then
                echo "    $line"
            elif [[ $line == *"contract"* ]]; then
                echo "    $line"
            fi
        done
        echo ""
    done
}

# Fonction pour afficher les métriques de débit
show_throughput() {
    echo "⚡ Métriques de débit (dernière minute):"
    
    # Calcul approximatif basé sur les offsets
    for topic in contract-partitions contract-results; do
        echo "  📊 Topic: $topic"
        
        # Obtenir les offsets de fin
        offsets=$(kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list $BOOTSTRAP_SERVERS --topic $topic --time -1 2>/dev/null)
        
        if [ ! -z "$offsets" ]; then
            total_messages=$(echo "$offsets" | awk -F: '{sum += $3} END {print sum}')
            echo "    Messages totaux: ${total_messages:-0}"
        else
            echo "    Aucune donnée disponible"
        fi
        echo ""
    done
}

# Fonction pour surveillance continue
monitor_continuous() {
    echo "🔄 Surveillance continue (Ctrl+C pour arrêter)..."
    echo ""
    
    while true; do
        clear
        echo "=== Monitoring Kafka - $(date) ==="
        echo ""
        
        show_consumer_lag
        show_throughput
        
        echo "⏰ Prochaine mise à jour dans 10 secondes..."
        sleep 10
    done
}

# Fonction pour afficher l'aide
show_help() {
    echo "Usage: $0 [bootstrap-servers] [option]"
    echo ""
    echo "Options:"
    echo "  topics      - Afficher les topics"
    echo "  details     - Afficher les détails des topics"
    echo "  groups      - Afficher les groupes de consommateurs"
    echo "  lag         - Afficher les offsets et lags"
    echo "  throughput  - Afficher les métriques de débit"
    echo "  monitor     - Surveillance continue"
    echo "  all         - Afficher toutes les informations (défaut)"
    echo ""
    echo "Exemples:"
    echo "  $0 localhost:9092 monitor"
    echo "  $0 kafka:29092 lag"
}

# Vérification de la connectivité Kafka
check_kafka_connectivity() {
    echo "🔍 Vérification de la connectivité Kafka..."
    if kafka-broker-api-versions.sh --bootstrap-server $BOOTSTRAP_SERVERS >/dev/null 2>&1; then
        echo "✅ Connexion Kafka réussie"
        echo ""
    else
        echo "❌ Impossible de se connecter à Kafka ($BOOTSTRAP_SERVERS)"
        echo "Vérifiez que Kafka est démarré et accessible."
        exit 1
    fi
}

# Programme principal
case "${2:-all}" in
    "help"|"-h"|"--help")
        show_help
        ;;
    "topics")
        check_kafka_connectivity
        show_topics
        ;;
    "details")
        check_kafka_connectivity
        show_topic_details
        ;;
    "groups")
        check_kafka_connectivity
        show_consumer_groups
        ;;
    "lag")
        check_kafka_connectivity
        show_consumer_lag
        ;;
    "throughput")
        check_kafka_connectivity
        show_throughput
        ;;
    "monitor")
        check_kafka_connectivity
        monitor_continuous
        ;;
    "all"|*)
        check_kafka_connectivity
        show_topics
        show_topic_details
        show_consumer_groups
        show_consumer_lag
        show_throughput
        ;;
esac