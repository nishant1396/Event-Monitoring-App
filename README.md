# Event Monitoring System

A simple Spring Boot application that tracks live events and publishes updates to Kafka.

## What it does

This app monitors events (like sports games) by:
1. Checking for score updates from an external API
2. Publishing those updates to Kafka
3. Letting clients start/stop tracking specific events

## Getting started

### Requirements
- Java 17
- Maven
- Docker (for Kafka)

### Running the app
```bash
mvn spring-boot:run
```

## API endpoints

### Start tracking an event
```
POST /events/status
Content-Type: application/json

{
  "eventId": "game-123",
  "status": true
}
```

### Stop tracking an event
```
POST /events/status
Content-Type: application/json

{
  "eventId": "game-123",
  "status": false
}
```

### View all events
```
GET /events
```
Response:
```json
{
  "game-123": {
    "eventId": "game-123",
    "live": true,
    "currentScore": "1",
    "lastUpdated": "2025-05-16T10:30:45Z"
  },
  "game-456": {
    "eventId": "game-456",
    "live": false,
    "currentScore": "3",
    "lastUpdated": "2025-05-16T08:15:20Z"
  }
}
```

### Check Kafka health
```
GET /health
```
Response:
```json
{
  "status": "UP",
  "kafka": "UP"
}
```

## How it works

1. Client sends a request to track an event
2. The app regularly polls a mock API for event updates (every 10 seconds by default)
3. When updates are found, the app publishes them to Kafka
4. Clients can consume these updates from the Kafka topic "live-sports-events"
5. The app handles retries if the external API or Kafka is temporarily unavailable

## Testing

Run the integration tests with:
```bash
mvn test
```

The tests verify:
- Kafka connectivity
- Event activation and deactivation
- Message publishing to Kafka
- API endpoints