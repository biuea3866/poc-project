# HR Platform Kafka Topics

Terraform module for managing HR Platform Kafka infrastructure. Defines the event topic for employee domain events.

## Topics

### event.hr.employee

Central topic for all employee domain events. Publishes events from hiring through termination, including department transfers, promotions, salary changes, and employment status changes.

| Config | Value |
|--------|-------|
| Partitions | 12 |
| Replication Factor | dev: 1, prod: 3 |
| Retention | 7 days (604800000 ms) |
| Cleanup Policy | delete |
| Compression | lz4 |
| Min In-Sync Replicas | dev: 1, prod: 2 |

## Events (13 types)

| Event Type | Description |
|---|---|
| `employee.hired` | New employee hired |
| `employee.resigned` | Employee resigned |
| `employee.suspended` | Employment suspended |
| `employee.resumed` | Employment resumed from suspension |
| `employee.promoted` | Position/rank promotion |
| `employee.transferred` | Department transfer |
| `employee.salary_changed` | Salary amount updated |
| `department.changed` | Department structure updated |
| `department.head_changed` | Department head assignment |
| `employee.transferred.cancelled` | Transfer cancelled |
| `employee.promoted.cancelled` | Promotion cancelled |
| `employee.salary_changed.cancelled` | Salary change cancelled |
| `employee.suspended.cancelled` | Suspension cancelled |

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

Downstream domains register their consumer groups through the Kafka infrastructure. Consumer definitions should reference the `event.hr.employee` topic.

Example consumer group registration (typically done in service configuration):

```properties
spring.kafka.consumer.group-id=your-service-consumer-group
spring.kafka.consumer.topics=event.hr.employee
```

## JSON Schema

Event payloads conform to JSON Schema Draft 7. Schema definitions are located in `schemas/employee/`.

### Common Fields

All events contain:

| Field | Type | Description |
|---|---|---|
| eventType | string | Enum of event type |
| eventVersion | integer | Schema version (starting at 1) |
| occurredAt | string | ISO-8601 UTC timestamp |
| employmentId | integer | Unique employment record ID |
| companyId | integer | Company ID |
| actorEmploymentId | integer \| null | Employment ID of the actor (null for system actions) |

### Event-Specific Fields

See `schemas/employee/` directory for complete JSON Schema definitions.

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
kafka-topics --bootstrap-server localhost:9092 --describe --topic event.hr.employee
```

## Related Documentation

- **ADR-002**: Employee SSOT Model — `/docs/adr/ADR-002-employee-ssot-model.md`
- **TDD-001**: Employee Service Technical Design — `/docs/tdd/TDD-001-employee-service.md`
- **Ticket KF-01**: Kafka Topic Declaration — `/docs/tickets/HR-M1-EMPLOYEE-TICKETS.md`
