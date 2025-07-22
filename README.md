# Architecture Spring Batch Remote Partitioning avec Kafka pour Maroc Telecom

Une architecture complète et scalable pour le traitement massif de contrats utilisant Spring Batch Remote Partitioning avec Kafka comme middleware de communication.

## 🚀 Vue d'ensemble

Cette solution permet de traiter des millions de contrats Maroc Telecom de manière distribuée, avec une architecture robuste et des performances optimisées.

### Architecture

```
CSV Files (millions) → TaskMaster → Kafka (contract-partitions) → Workers → Kafka (contract-results) → TaskMaster → Résultats agrégés
                         ↑                                                                                        ↓
                     API REST ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←← Monitoring & Métriques
```

### Fonctionnalités principales

- ✅ **Remote Partitioning** : Découpage intelligent des fichiers CSV avec distribution via Kafka
- ✅ **Traitement distribué** : Multiple workers pour un traitement parallèle haute performance
- ✅ **Validation métier** : Validation complète des contrats selon les règles Maroc Telecom
- ✅ **Monitoring temps réel** : Métriques, health checks et tableaux de bord
- ✅ **Gestion d'erreurs** : Retry logic, dead letter queues et reporting détaillé
- ✅ **Scalabilité** : Auto-scaling horizontal des workers
- ✅ **Production-ready** : Configuration Kubernetes, Docker et monitoring

## 🏗️ Structure du projet

```
spring-batch-kafka-partitioning/
├── spring-batch-common/          # DTOs et modèles partagés
│   ├── dto/                      # PartitionTaskDTO, TaskResultDTO, ErrorInfo
│   ├── model/                    # Contract (modèle métier Maroc Telecom)
│   └── config/                   # KafkaConfig centralisée
├── task-master/                  # Nœud maître (coordination)
│   ├── application/              # TaskMasterApplication
│   ├── partitioner/              # FilePartitioner (découpage intelligent)
│   ├── service/                  # KafkaPartitionSender
│   └── listener/                 # ResultListener (agrégation)
├── worker-node/                  # Nœuds workers (traitement)
│   ├── application/              # WorkerNodeApplication  
│   ├── listener/                 # PartitionTaskListener
│   ├── processor/                # ContractProcessor (logique métier)
│   ├── validator/                # ContractValidator
│   └── service/                  # ResultSender
├── docker/                       # Configuration Docker
├── scripts/                      # Scripts utilitaires
├── test-data/                    # Données de test
└── docs/                         # Documentation
```

## 🚀 Démarrage rapide (5 minutes)

### 1. Prérequis

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 4GB RAM minimum

### 2. Construction du projet

```bash
# Cloner le repository
git clone https://github.com/yasukeo/spring-batch-kafka-partitioning.git
cd spring-batch-kafka-partitioning

# Compilation
mvn clean package -DskipTests

# Génération de données de test
./scripts/generate-test-data.sh test-data/contracts.csv 50000
```

### 3. Démarrage avec Docker Compose

```bash
# Démarrage de l'infrastructure complète
cd docker
docker-compose up -d

# Vérification du statut
docker-compose ps

# Logs en temps réel
docker-compose logs -f task-master worker-node-1
```

### 4. Interfaces web

- **Task Master API** : http://localhost:8080/api/actuator
- **Worker Nodes** : http://localhost:8081/actuator, http://localhost:8082/actuator
- **Kafka UI** : http://localhost:8090
- **Prometheus** : http://localhost:9090

### 5. Lancement d'un job de test

```bash
# Via API REST
curl -X POST "http://localhost:8080/api/batch/jobs/contract-processing/start" \
  -H "Content-Type: application/json" \
  -d '{"filePath": "/app/data/input/contracts.csv"}'

# Monitoring du job
curl "http://localhost:8080/api/batch/jobs/executions"
```

## 📊 Modules détaillés

### Spring Batch Common

Module partagé contenant les DTOs et modèles métier :

#### PartitionTaskDTO
```java
@Data @Builder
public class PartitionTaskDTO {
    private String taskId;
    private String fileName;
    private String filePath;
    private Long startLine;    // Ligne de début (1-based)
    private Long endLine;      // Ligne de fin (inclusive)
    private Integer partitionId;
    private String jobId;
    private LocalDateTime createdAt;
    // ... méthodes utilitaires
}
```

#### Contract (Modèle Maroc Telecom)
```java
@Data @Builder
public class Contract {
    private String contractId;
    private String clientId;
    private String clientName;
    private ContractType contractType; // MOBILE, FIXE, INTERNET, ADSL, FIBRE
    private BigDecimal amount;
    private LocalDate creationDate;
    private ContractStatus status;     // ACTIVE, PENDING, SUSPENDED, etc.
    private String region;             // Régions marocaines
    // ... validation métier et calculs
}
```

### Task Master

Le nœud maître coordonne le traitement :

- **FilePartitioner** : Découpe les fichiers CSV en partitions optimales
- **KafkaPartitionSender** : Distribue les tâches via Kafka
- **ResultListener** : Agrège les résultats des workers en temps réel
- **JobController** : API REST pour la gestion des jobs

### Worker Node

Les workers traitent les partitions :

- **PartitionTaskListener** : Écoute les tâches Kafka
- **ContractProcessor** : Traitement métier (parsing, validation, calculs)
- **ContractValidator** : Validation spécifique Maroc Telecom
- **ResultSender** : Envoi des résultats vers le master

## 📈 Monitoring et métriques

### Métriques disponibles

- **Débit** : Contrats/seconde par worker
- **Latence** : Temps de traitement par partition
- **Taux d'erreur** : Pourcentage d'échecs par type
- **Utilisation ressources** : CPU, mémoire, I/O
- **Kafka** : Offsets, lag, débit des topics

### Health checks

```bash
# Status global
curl http://localhost:8080/api/actuator/health

# Métriques Prometheus
curl http://localhost:8080/api/actuator/prometheus

# Monitoring Kafka
./scripts/kafka-monitor.sh localhost:9092 monitor
```

## 🔧 Configuration

### Configuration Kafka

```yaml
app:
  kafka:
    broker:
      servers: localhost:9092
    topics:
      contract-partitions: contract-partitions  # 8 partitions
      contract-results: contract-results        # 4 partitions
    consumer-groups:
      task-master: task-master-group
      worker: worker-group
    producer:
      acks: all
      retries: 3
      enable-idempotence: true
```

### Configuration traitement

```yaml
processing:
  contract:
    default-timeout-seconds: 300
    max-partition-size: 50000
    strict-validation: true
    continue-on-error: true
    max-errors-per-partition: 1000
```

## 🐳 Déploiement

### Docker

```bash
# Build des images
mvn clean package
docker build -f docker/Dockerfile.task-master -t maroctelecom/task-master:1.0.0 task-master/
docker build -f docker/Dockerfile.worker-node -t maroctelecom/worker-node:1.0.0 worker-node/

# Déploiement
docker-compose up -d --scale worker-node=4
```

### Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|---------|
| `KAFKA_BROKERS` | Serveurs Kafka | `localhost:9092` |
| `BATCH_INPUT_DIR` | Répertoire d'entrée | `/tmp/batch/input` |
| `SERVER_PORT` | Port du service | `8080`/`8081` |
| `JAVA_OPTS` | Options JVM | `-Xms512m -Xmx1024m` |

## 📁 Format des données

### Fichier CSV d'entrée

```csv
contractId,clientId,clientName,contractType,amount,creationDate,activationDate,expirationDate,status,region,city,postalCode,phoneNumber,email,durationMonths,salesChannel,salesAgentId
CT00000001,CL000001,Mohamed Alami,MOBILE,150.00,2023-01-15,2023-01-16,2024-01-15,ACTIVE,CASABLANCA-SETTAT,Casablanca,20000,0661234567,mohamed.alami@email.com,12,BOUTIQUE,AG0001
```

### Validation métier

- **Formats** : Téléphones marocains (+212/0), emails, codes postaux
- **Montants** : 0-1M DH selon le type de contrat
- **Régions** : 12 régions officielles du Maroc
- **Dates** : Cohérence temporelle (création < activation < expiration)
- **Business rules** : Statuts compatibles, canaux autorisés par type

## 🔍 Troubleshooting

### Problèmes courants

**1. Connexion Kafka échouée**
```bash
# Vérifier Kafka
docker-compose logs kafka
./scripts/kafka-monitor.sh localhost:9092 topics
```

**2. Workers ne reçoivent pas de tâches**
```bash
# Vérifier les groupes de consommateurs
./scripts/kafka-monitor.sh localhost:9092 groups
```

**3. Erreurs de parsing CSV**
```bash
# Vérifier le format des données
head -5 test-data/contracts.csv
```

### Logs utiles

```bash
# Logs du master
docker-compose logs task-master | grep ERROR

# Logs des workers
docker-compose logs worker-node-1 | grep "Tâche.*traitée"

# Monitoring Kafka
./scripts/kafka-monitor.sh localhost:9092 lag
```

## 🤝 Contribution

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Commit (`git commit -am 'Ajout nouvelle fonctionnalité'`)
4. Push (`git push origin feature/nouvelle-fonctionnalite`)
5. Créer une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 📞 Support

- **Documentation** : [docs/](docs/)
- **Issues** : [GitHub Issues](https://github.com/yasukeo/spring-batch-kafka-partitioning/issues)
- **Email** : support@maroctelecom.ma

---

**Maroc Telecom** - Architecture Spring Batch Kafka Partitioning v1.0.0