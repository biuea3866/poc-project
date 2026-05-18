# HR Platform Kafka Topics — Auth Domain

Terraform module for managing HR Platform Kafka infrastructure. Defines the event topic for authentication and user account domain events.

## Topics

### event.hr.auth.v1

Central topic for all authentication and user account domain events. Publishes events from user creation through account lifecycle management, including status changes, access control modifications, and security events.

| Config | Value |
|--------|-------|
| Partitions | 12 |
| Replication Factor | dev: 1, prod: 3 |
| Retention | 7 days (604800000 ms) |
| Cleanup Policy | delete |
| Compression | lz4 |
| Min In-Sync Replicas | dev: 1, prod: 2 |

### event.hr.auth.v1.dlq

Dead Letter Queue topic for failed message processing.

| Config | Value |
|--------|-------|
| Partitions | 3 |
| Replication Factor | dev: 1, prod: 3 |
| Retention | 30 days (2592000000 ms) |
| Cleanup Policy | delete |
| Compression | lz4 |
| Min In-Sync Replicas | dev: 1, prod: 2 |

## Events (11 types)

| Event Type | Aggregate Type | Action Type | Description |
|---|---|---|---|
| UserCreated | UserAccount | CREATE | New user account created |
| UserLocked | UserAccount | LOCK | User account locked after failed attempts |
| UserUnlocked | UserAccount | UNLOCK | User account unlocked |
| UserSuspended | UserAccount | SUSPEND | User account suspended |
| UserReactivated | UserAccount | REACTIVATE | User account reactivated |
| UserDeactivated | UserAccount | DEACTIVATE | User account deactivated/deleted |
| UserRoleAssigned | UserAccount | ROLE_ASSIGN | Role granted to user |
| UserRoleRevoked | UserAccount | ROLE_REVOKE | Role revoked from user |
| UserPasswordChanged | UserAccount | PASSWORD_CHANGE | User password changed |
| UserTwoFactorEnrolled | UserAccount | TWO_FACTOR_ENROLL | Two-factor authentication enrolled |
| UserTwoFactorDisabled | UserAccount | TWO_FACTOR_DISABLE | Two-factor authentication disabled |

## Usage

### Initialize Terraform

```bash
cd infrastructure/kafka-auth
terraform init
```

### Plan deployment (dev environment)

```bash
terraform plan -var environment=dev
```

### Apply configuration (dev environment)

```bash
terraform apply -var environment=dev
```

### Apply configuration (prod environment)

```bash
terraform apply \
  -var environment=prod \
  -var replication_factor=3 \
  -var min_insync_replicas=2
```

## Consumer Group Registration

Downstream domains register their consumer groups through the Kafka infrastructure. Consumer definitions should reference the `event.hr.auth.v1` topic.

Example consumer group registration (typically done in service configuration):

```properties
spring.kafka.consumer.group-id={service-name}.auth.v1
spring.kafka.consumer.topics=event.hr.auth.v1
```

Consumer group naming convention: `{service-name}.auth.v1` (e.g., `employee-service.auth.v1`, `audit-service.auth.v1`)

## JSON Schema

Event payloads conform to JSON Schema Draft 7. Schema definitions are located in `schemas/auth/`.

### Envelope Structure

All events follow a consistent envelope structure with `action` and `state` sections:

```json
{
  "eventId": "uuid",
  "eventType": "UserCreated",
  "eventVersion": 1,
  "occurredAt": "2026-05-18T12:00:00Z",
  "aggregateType": "UserAccount",
  "aggregateId": 12345,
  "companyId": 1,
  "actorEmploymentId": null,
  "action": {
    "type": "CREATE",
    "details": {
      "employmentId": 100,
      "email": "user@example.com",
      "defaultRole": "EMPLOYEE"
    }
  },
  "state": {
    "status": "ACTIVE",
    "snapshot": {
      "employmentId": 100,
      "email": "user@example.com",
      "twoFactorEnabled": false,
      "lockedUntil": null
    }
  }
}
```

### Common Fields

All events contain:

| Field | Type | Description |
|---|---|---|
| eventId | string (UUID) | Unique event identifier |
| eventType | string | Type of event (e.g., UserCreated) |
| eventVersion | integer | Schema version (always 1 for v1) |
| occurredAt | string | ISO-8601 UTC timestamp |
| aggregateType | string | Domain aggregate type (always UserAccount) |
| aggregateId | integer | User account ID |
| companyId | integer | Company ID |
| actorEmploymentId | integer \| null | Employment ID of the actor (null for system actions) |
| action | object | Action type and details |
| state | object | Current account status and snapshot |

### State Status Values

The `state.status` field indicates the current account state:

| Status | Description |
|--------|-------------|
| ACTIVE | Account is active and usable |
| LOCKED | Account temporarily locked (failed login attempts) |
| SUSPENDED | Account suspended (administrative action) |
| DEACTIVATED | Account deactivated/deleted |

### Event-Specific Fields

See `schemas/auth/` directory for complete JSON Schema definitions of each event type.

All event details are contained within `action.details` (event-specific fields) and `state.snapshot` (current state).

## Maintenance

### Scaling Partitions

To increase partitions (non-destructive):

```bash
terraform apply -var partitions=24
```

### Retention Policy Changes

To extend retention (non-destructive):

```bash
terraform apply \
  -var-file=prod.tfvars
```

Then modify `main.tf` and reapply.

### Monitor Topics

List active topics:

```bash
kafka-topics --bootstrap-server localhost:9092 --list
```

Describe topic:

```bash
kafka-topics --bootstrap-server localhost:9092 --describe --topic event.hr.auth.v1
```

Describe DLQ topic:

```bash
kafka-topics --bootstrap-server localhost:9092 --describe --topic event.hr.auth.v1.dlq
```

## DLQ Processing

The DLQ topic (`event.hr.auth.v1.dlq`) is configured for operational monitoring:

- **Retention**: 30 days for incident investigation
- **Monitoring**: Sentry/Datadog alerts on message arrival
- **Purpose**: Dead letter queue for messages that fail processing in downstream consumers
- **Consumer**: DevOps/SRE team monitors for alerting and root cause analysis

No dedicated consumer group is initially required; monitoring and alerting are handled externally.

## Related Documentation

- **ADR-003**: Auth Domain Event Model — `/docs/adr/ADR-003-auth-domain-events.md`
- **TDD-AUTH**: Auth Service Technical Design — `/docs/tdd/TDD-AUTH-service.md`
- **Ticket AT-KF**: Kafka Topic v1 Declaration — Authentication domain events with action/state payload
