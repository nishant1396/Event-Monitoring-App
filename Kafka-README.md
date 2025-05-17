# Kafka with Kafka UI in Docker

This repository contains instructions for setting up Apache Kafka and Kafka UI using Docker on macOS. This configuration allows both local applications and Docker containers to connect to the Kafka broker.

## Prerequisites

- Docker Desktop for Mac
- Terminal access

## Quick Start

### 1. Create a Docker Network

First, create a Docker network to allow containers to communicate with each other:

```bash
docker network create kafka-network
```

### 2. Start Kafka

Run Kafka in Docker with proper configuration:

```bash
docker run -d --name kafka \
  --network kafka-network \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
  apache/kafka:latest
```

### 3. Start Kafka UI

Run the Kafka UI container connected to the same network:

```bash
docker run -d \
  --name kafka-ui \
  --network kafka-network \
  -p 8081:8080 \
  --add-host=localhost:host-gateway \
  -e KAFKA_CLUSTERS_0_NAME=MyKafkaCluster \
  -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=localhost:9092 \
  provectuslabs/kafka-ui:latest
```

### 4. Access Kafka UI

Open your browser and go to [http://localhost:8081](http://localhost:8081)