# Traversium Notification Service

A Spring Boot microservice for managing real-time notifications in the Traversium platform. This service handles notification bundling, real-time delivery via Server-Sent Events (SSE), and Firebase authentication.

## Features

- **Real-time Notifications**: Server-Sent Events (SSE) for instant notification delivery
- **Notification Bundling**: Grouping of related notifications
- **Firebase Authentication**: Secure authentication using Firebase ID tokens
- **Multi-tenancy Support**: Isolated data per tenant using custom multi-tenancy library
- **Kafka Integration**: Consumes notification events from Kafka topics
- **RESTful API**: Comprehensive API for notification management
- **Observability**: Prometheus metrics, health checks, and ELK stack integration
- **Spring Cloud Config**: Centralized configuration management support
- **Anti-spam Features**: Built-in spam prevention mechanisms
- **Scheduled Processing**: Automatic notification bundling at configurable intervals

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Apache Kafka
- Firebase project (for authentication)
- Spring Cloud Config Server (optional)

## Configuration

### Application Properties

Located at: `notification-service/src/main/resources/application.properties`

```properties
# Application Name
spring.application.name=NotificationService

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/notifaction_db
spring.datasource.username=<username>
spring.datasource.password=<password>
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.topic=notification-topic

# Notification Bundling Interval (milliseconds)
notification.bundling.interval=5000

# Management & Monitoring
management.endpoints.web.exposure.include=health,info,prometheus,refresh
management.endpoint.health.show-details=always
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true

# Health Check Groups
management.endpoint.health.group.readiness.include=readinessState,db,diskSpace
management.endpoint.health.group.liveness.include=livenessState,ping

# Spring Cloud Config (optional)
spring.config.import=optional:configserver:http://localhost:8888
```

### Firebase Configuration

Place your Firebase service account key file in the resources directory and configure it in `FirebaseConfiguration.kt`.

### Spring Cloud Config

The service supports Spring Cloud Config for centralized configuration. To enable it:

1. Make sure Spring Cloud Config Server is running at `http://localhost:8888`
2. The `spring.config.import` property will automatically connect to the config server
3. To disable config client (for local development): Add `spring.cloud.config.enabled=false`

## Building and Running

### Build the Project

```bash
mvn clean install
```

### Run the Service

```bash
cd notification-service
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar notification-service/target/notification-service-1.4.0-SNAPSHOT.jar
```

## API Endpoints

Base URL: `/rest/v1/notifications`

### 1. SSE Notification Stream

**Endpoint**: `GET /rest/v1/notifications/sse`

**Description**: Establishes a Server-Sent Events connection to receive real-time notification bundle IDs.

**Authentication**: Required (Firebase token)

**Response**: Stream of `BundleIdDto` objects

**Example**:
```javascript
const eventSource = new EventSource('/rest/v1/notifications/sse', {
  headers: { 'Authorization': 'Bearer <firebase-token>' }
});

eventSource.addEventListener('NotificationBundle', (event) => {
  const data = JSON.parse(event.data);
  console.log('New notification bundle:', data.bundleId);
});
```

### 2. Get Unseen Notifications Count

**Endpoint**: `GET /rest/v1/notifications/unseen/count`

**Description**: Returns the count of unseen notification bundles.

**Authentication**: Required

**Response**: `Long`

**Example Response**:
```json
5
```

### 3. Get All Notifications

**Endpoint**: `GET /rest/v1/notifications?offset=0&limit=20`

**Description**: Retrieves both unseen and seen notification bundles with pagination.

**Authentication**: Required

**Parameters**:
- `offset` (default: 0): Pagination offset
- `limit` (default: 20): Number of items per page

**Response**: `NotificationBundleListDto`

**Example Response**:
```json
{
  "unseenBundles": [
    {
      "bundleId": "user123-LIKE_PHOTO-media456",
      "senderIds": ["user789", "user101"],
      "type": "LIKE_PHOTO",
      "mediaReferenceIds": "media456",
      "notificationCount": 2,
      "firstTimestamp": "2025-12-23T10:00:00Z",
      "lastTimestamp": "2025-12-23T10:05:00Z"
    }
  ],
  "seenBundles": [
    {
      "bundleId": "user123-ADD_COMMENT-media789",
      "senderIds": ["user202"],
      "type": "ADD_COMMENT",
      "mediaReferenceIds": "media789",
      "commentReferenceId": "comment303",
      "notificationCount": 1,
      "firstTimestamp": "2025-12-23T09:00:00Z",
      "lastTimestamp": "2025-12-23T09:00:00Z"
    }
  ]
}
```

### 4. Get Unseen Notifications

**Endpoint**: `GET /rest/v1/notifications/unseen?offset=0&limit=20`

**Description**: Retrieves unseen notification bundles and marks them as seen.

**Authentication**: Required

**Response**: `List<NotificationBundleDto>`

### 5. Get Seen Notifications

**Endpoint**: `GET /rest/v1/notifications/seen?offset=0&limit=20`

**Description**: Retrieves previously seen notification bundles.

**Authentication**: Required

**Response**: `List<NotificationBundleDto>`

## Notification Types

The service supports the following notification types:

### Trip (Collection) Notifications
- `CREATE_TRIP` - New trip created
- `DELETE_TRIP` - Trip deleted
- `CHANGE_TRIP_TITLE` - Trip title changed
- `CHANGE_TRIP_DESCRIPTION` - Trip description changed
- `CHANGE_TRIP_COVER_PHOTO` - Trip cover photo changed
- `CHANGE_TRIP_VISIBILITY` - Trip visibility changed
- `ADD_COLLABORATOR` - User added as collaborator
- `REMOVE_COLLABORATOR` - Collaborator removed
- `ADD_VIEWER` - Viewer added
- `REMOVE_VIEWER` - Viewer removed

### Moment (Node) Notifications
- `CREATE_MOMENT` - New moment created
- `DELETE_MOMENT` - Moment deleted
- `CHANGE_MOMENT_TITLE` - Moment title changed
- `CHANGE_MOMENT_DESCRIPTION` - Moment description changed
- `CHANGE_MOMENT_COVER_PHOTO` - Moment cover photo changed
- `REARRANGE_MOMENTS` - Moments reordered

### Photo (Media) Notifications
- `ADD_PHOTO` - Photo added to moment
- `REMOVE_PHOTO` - Photo removed from moment
- `LIKE_PHOTO` - Photo liked
- `ADD_COMMENT` - Comment added to photo
- `REPLY_COMMENT` - Reply to comment

### User Notifications
- `FOLLOW_USER` - User followed

## Kafka Integration

### Consumer Configuration

The service consumes messages from the `notification-topic` Kafka topic.

**Message Format** (`NotificationStreamData`):
```json
{
  "receiverIds": ["user123"],
  "senderId": "user456",
  "collectionReferenceId": "collection789",
  "nodeReferenceId": "node101",
  "mediaReferenceId": "media202",
  "commentReferenceId": "comment303",
  "action": "ADD",
  "timestamp": "2025-12-23T10:00:00Z"
}
```

### Notification Bundling

The `NotificationBundlingService` collects notifications for 5 seconds (configurable via `notification.bundling.interval`) before processing them in batches. This reduces database operations and prevents notification spam.

**Bundling Logic**:
1. Notifications are collected in a concurrent queue
2. Every 5 seconds, the batch is processed
3. Notifications are saved to the database
4. Bundle IDs are generated and emitted via SSE
5. Firebase push notifications are sent (if configured)

**Bundle ID Generation**:
Bundle IDs follow the pattern: `{receiverId}-{action}-{referenceId}`

Example: `user123-LIKE_PHOTO-media456`

## Monitoring & Observability

### Health Checks

- **Liveness**: `GET /actuator/health/liveness`
- **Readiness**: `GET /actuator/health/readiness`
- **Full Health**: `GET /actuator/health`

### Prometheus Metrics

Available at: `GET /actuator/prometheus`

### SSE Heartbeat

The service sends a heartbeat message every 30 seconds to keep SSE connections alive:
```json
{
  "bundleId": "HEALTHCHECK"
}
```

### Logging

Structured logging with Logstash encoder for ELK stack integration.

## Multi-Tenancy

The service uses the `common-multitenancy` library for tenant isolation:

- Automatic tenant detection from request context
- Separate database schemas per tenant
- Flyway migrations per tenant
- Tenant-aware JPA repositories

## API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

OpenAPI spec: `http://localhost:8080/v3/api-docs`

## Development

### Project Structure

```
NotificationService/
├── notification-models/          # Shared data models
├── notification-service/         # Main service implementation
│   ├── src/main/kotlin/
│   │   └── traversium/notification/
│   │       ├── configuration/    # Spring configurations
│   │       ├── db/              # Database entities and repositories
│   │       ├── dto/             # Data transfer objects
│   │       ├── exceptions/      # Exception handling
│   │       ├── kafka/           # Kafka consumers and config
│   │       ├── mapper/          # Entity-DTO mappers
│   │       ├── rest/            # REST controllers
│   │       ├── security/        # Security filters and auth
│   │       ├── service/         # Business logic
│   │       ├── swagger/         # API documentation config
│   │       └── util/            # Utility classes
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/        # Flyway migrations
├── pom.xml                      # Parent POM
└── CHANGELOG.md                 # Version history
```

### Running Tests

```bash
mvn test
```

Tests use H2 in-memory database and Spring Kafka Test for integration testing.

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`

Create a new migration:
```
V{version}__description.sql
```

Example: `V1__initial_schema.sql`
