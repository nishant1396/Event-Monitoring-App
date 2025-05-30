# Event Monitoring System

A Spring Boot application that tracks live events and publishes updates to Kafka.

## What it does

This app monitors events (like sports games) by:
1. Letting clients start/stop tracking specific events
2. Checking for score updates from an external API
3. Publishing those updates to Kafka

## Getting started

### Requirements
- Java 17
- Maven
- Docker (for Kafka)

### Run the Application
```bash
# Clone the repository
git clone https://github.com/nishant1396/Event-Monitoring-App.git
cd event-monitoring

mvn spring-boot:run
```
### Run Integration Tests
Ensure Kafka is running first, then:
```bash
mvn test -Dtest=EventMonitoringIntegrationTest
```

### Test Endpoints Manually
Use tools like curl or Postman (the Postman collection is available on the repo):

#### Start tracking an event:
```bash
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId":"game-123","status":true}'
```

#### Check event status:
```bash
curl http://localhost:8080/events
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

The tests verify:
- Kafka connectivity
- Event activation and deactivation
- Message publishing to Kafka
- API endpoints

## Design Decisions

The application uses Spring Boot with a service-based architecture to efficiently monitor events and publish updates. For resilience, I implemented retry mechanisms for Kafka messaging, with graceful error handling to prevent task failures. The design features configurable polling intervals for external APIs, straightforward REST endpoints for control, and Kafka for asynchronous communication. Health endpoints and comprehensive logging provide operational visibility, while integration tests verify functionality using real Kafka infrastructure with mocked external dependencies.
## AI-Assisted Development

This project utilized Claude AI assistance for the following parts:

1. **Integration Test Implementation**:
    - The integration test was initially generated by Claude
    - I verified and fixed the port configuration issue for mock API calls

2. **POM File Optimization**:
    - Claude helped identify unnecessary dependencies
    - I verified each removal to ensure core functionality remained intact

3. **README Documentation**:
    - The initial structure was generated by Claude
    - I expanded it to include the required sections about design decisions and AI assistance
    - Added more detailed setup and test instructions