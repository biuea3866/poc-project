# HR Platform Kafka Topics

Terraform module for managing HR Platform Kafka infrastructure. Defines the event topic for employee domain events.

## Topics

### event.hr.employee.v1

Central topic for all employee domain events. Publishes events from hiring through termination, including department transfers, promotions, salary changes, and employment status changes.

| Config | Value |
|--------|-------|
| Partitions | 12 |
| Replication Factor | dev: 1, prod: 3 |
| Retention | 7 days (604800000 ms) |
| Cleanup Policy | delete |
| Compression | lz4 |
| Min In-Sync Replicas | dev: 1, prod: 2 |

### event.hr.employee.v1.dlq

Dead Letter Queue topic for failed message processing.

| Config | Value |
|--------|-------|
| Partitions | 3 |
| Replication Factor | dev: 1, prod: 3 |
| Retention | 30 days (2592000000 ms) |
| Cleanup Policy | delete |
| Compression | lz4 |
| Min In-Sync Replicas | dev: 1, prod: 2 |

## Events (13 types)

| Event Type | Aggregate Type | Action Type | Description |
|---|---|---|---|
| EmployeeHired | Employment | HIRE | New employee hired |
| EmployeeResigned | Employment | RESIGN | Employee resigned |
| EmployeeSuspended | Employment | SUSPEND | Employment suspended |
| EmployeeResumed | Employment | RESUME | Employment resumed from suspension |
| EmployeePromoted | Employment | PROMOTE | Position/rank promotion |
| EmployeeTransferred | Employment | TRANSFER | Department transfer |
| EmployeeSalaryChanged | Employment | SALARY_CHANGE | Salary amount updated |
| DepartmentChanged | Department | DEPARTMENT_MOVE | Department structure updated |
| DepartmentHeadChanged | Department | HEAD_CHANGE | Department head assignment |
| EmployeeTransferredCancelled | Employment | TRANSFER_CANCELLED | Transfer cancelled |
| EmployeePromotedCancelled | Employment | PROMOTE_CANCELLED | Promotion cancelled |
| EmployeeSalaryChangedCancelled | Employment | SALARY_CHANGE_CANCELLED | Salary change cancelled |
| EmployeeSuspendedCancelled | Employment | SUSPEND_CANCELLED | Suspension cancelled |

## Usage

### Initialize Terraform

```bash
cd infrastructure/kafka
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

Downstream domains register their consumer groups through the Kafka infrastructure. Consumer definitions should reference the `event.hr.employee.v1` topic.

Example consumer group registration (typically done in service configuration):

```properties
spring.kafka.consumer.group-id={service-name}.employee.v1
spring.kafka.consumer.topics=event.hr.employee.v1
```

Consumer group naming convention: `{service-name}.{producer-domain}.v1` (e.g., `auth-service.employee.v1`)

## JSON Schema

Event payloads conform to JSON Schema Draft 7. Schema definitions are located in `schemas/employee/`.

### Envelope Structure

All events follow a consistent envelope structure with `action` and `state` sections:

```json
{
  "eventId": "uuid",
  "eventType": "EmployeeHired",
  "eventVersion": 1,
  "occurredAt": "2026-05-17T12:00:00Z",
  "aggregateType": "Employment",
  "aggregateId": 12345,
  "companyId": 1,
  "actorEmploymentId": 999,
  "action": {
    "type": "HIRE",
    "details": {
      "personId": 456,
      "startDate": "2026-06-01",
      "employmentType": "REGULAR"
    }
  },
  "state": {
    "status": "ACTIVE",
    "snapshot": {
      "departmentId": 10,
      "managerEmploymentId": 888,
      "country": "US",
      "currency": "USD",
      "timezone": "America/New_York"
    }
  }
}
```

### Common Fields

All events contain:

| Field | Type | Description |
|---|---|---|
| eventId | string (UUID) | Unique event identifier |
| eventType | string | Type of event (e.g., EmployeeHired) |
| eventVersion | integer | Schema version (always 1 for v1) |
| occurredAt | string | ISO-8601 UTC timestamp |
| aggregateType | string | Domain aggregate type (Employment or Department) |
| aggregateId | integer | Aggregate ID (employmentId or departmentId) |
| companyId | integer | Company ID |
| actorEmploymentId | integer \| null | Employment ID of the actor (null for system actions) |
| action | object | Action type and details |
| state | object | Current aggregate status and snapshot |

### Event-Specific Fields

See `schemas/employee/` directory for complete JSON Schema definitions of each event type.

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
kafka-topics --bootstrap-server localhost:9092 --describe --topic event.hr.employee.v1
```

Describe DLQ topic:

```bash
kafka-topics --bootstrap-server localhost:9092 --describe --topic event.hr.employee.v1.dlq
```

## DLQ Processing

The DLQ topic (`event.hr.employee.v1.dlq`) is configured for operational monitoring:

- **Retention**: 30 days for incident investigation
- **Monitoring**: Sentry/Datadog alerts on message arrival
- **Purpose**: Dead letter queue for messages that fail processing in downstream consumers
- **Consumer**: DevOps/SRE team monitors for alerting and root cause analysis

No dedicated consumer group is initially required; monitoring and alerting are handled externally.

## Related Documentation

- **ADR-002**: Employee SSOT Model — `/docs/adr/ADR-002-employee-ssot-model.md`
- **TDD-001**: Employee Service Technical Design — `/docs/tdd/TDD-001-employee-service.md`
- **Ticket KF-02**: Kafka Topic v1 Declaration — Event sourcing with action/state payload
